package cn.petpet.app.ui.pethealth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.petpet.app.data.PetPetRepository
import cn.petpet.app.data.pethealth.DewormRecord
import cn.petpet.app.data.pethealth.HealthAlert
import cn.petpet.app.data.pethealth.HealthAlertRules
import cn.petpet.app.data.pethealth.HealthMealRecord
import cn.petpet.app.data.pethealth.HealthPetProfile
import cn.petpet.app.data.pethealth.HealthPlan
import cn.petpet.app.data.pethealth.HealthTodo
import cn.petpet.app.data.pethealth.HealthWeightRecord
import cn.petpet.app.data.pethealth.PetBreedLibrary
import cn.petpet.app.data.pethealth.PetGender
import cn.petpet.app.data.pethealth.PetHealthCalculator
import cn.petpet.app.data.pethealth.PetHealthLocalData
import cn.petpet.app.data.pethealth.PetHealthRepository
import cn.petpet.app.data.pethealth.PetKind
import cn.petpet.app.data.pethealth.PoopRecord
import cn.petpet.app.data.pethealth.SelfCheckLibrary
import cn.petpet.app.data.pethealth.SelfCheckResult
import cn.petpet.app.data.pethealth.VaccineRecord
import cn.petpet.app.data.pethealth.WaterRecord
import cn.petpet.app.model.PetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Petpet 健康助手 UI 状态。 */
data class PetHealthUiState(
    val data: PetHealthLocalData = PetHealthLocalData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
    val currentPetId: String? = null,
    val showAddPet: Boolean = false,
    val showOnboarding: Boolean = true,   // 无任何档案时显示引导
    val notice: String? = null,
)

class PetHealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetHealthRepository(application)
    private val hostRepository = PetPetRepository(application)

    private val _state = MutableStateFlow(PetHealthUiState())
    val state: StateFlow<PetHealthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataFlow.collect { data ->
                val currentId = _state.value.currentPetId
                    ?.takeIf { id -> data.pets.any { it.id == id } }
                    ?: data.pets.firstOrNull()?.id
                _state.update {
                    it.copy(
                        data = data,
                        currentPetId = currentId,
                        showOnboarding = data.pets.isEmpty(),
                    )
                }
            }
        }
        // 不再自动带入宿主宠物（2026-08 用户反馈 BUG01：改由用户主动选择添加，
        // 入口在宠物切换栏的「＋添加」→「添加当前电子宠物 / 自定义添加宠物」）
    }

    // ---- 当前宠物 ----

    fun selectPet(petId: String) = _state.update { it.copy(currentPetId = petId) }

    fun currentPet(): HealthPetProfile? =
        _state.value.data.pets.firstOrNull { it.id == _state.value.currentPetId }

    fun planOf(pet: HealthPetProfile): HealthPlan {
        val foodBans = pet.foodBans + PetHealthCalculator.foodBans(pet.kind)
        return HealthPlan(
            rer = PetHealthCalculator.rer(pet.weightKg),
            dailyCalories = PetHealthCalculator.dailyCalories(pet),
            feedingTimes = PetHealthCalculator.feedingTimes(pet),
            mealSizeG = PetHealthCalculator.mealSizeG(pet),
            foodBans = foodBans.distinct(),
            exerciseText = PetHealthCalculator.exerciseText(pet),
            exerciseMinutes = PetHealthCalculator.exerciseMinutes(pet),
            vaccinePlan = PetHealthCalculator.vaccinePlan(pet),
            dewormPlan = PetHealthCalculator.dewormPlan(pet),
            checkupNote = PetHealthCalculator.checkupNote(pet),
            isSenior = PetHealthCalculator.isSenior(pet),
            isOverweight = PetHealthCalculator.isOverweight(pet),
        )
    }

    // ---- 档案 ----

    fun showAddPet() = _state.update { it.copy(showAddPet = true) }
    fun hideAddPet() = _state.update { it.copy(showAddPet = false) }

    /**
     * 添加当前电子宠物（宿主已领养的宠物）。
     * 防止重复：若健康档案里已有同品种的宿主宠物则提示不重复添加。
     */
    fun addHostPet() {
        val snapshot = hostRepository.snapshot()
        val pet = snapshot.pet ?: run {
            showNotice("还没有领养电子宠物，请先回 My Pet 领养")
            return
        }
        val kind = if (PetBreedsHelper.typeOf(pet.breedId) == PetType.DOG) PetKind.DOG else PetKind.CAT
        // 防重复：宿主宠物档案 id 固定为 host_<breedId>，已存在即不重复添加
        val hostId = "host_${pet.breedId}"
        if (_state.value.data.pets.any { it.id == hostId }) {
            showNotice("已添加过当前电子宠物，无需重复添加")
            return
        }
        val healthBreed = if (kind == PetKind.DOG) {
            PetBreedLibrary.byId("dog_tianyuan") ?: PetBreedLibrary.byKind(PetKind.DOG).first()
        } else {
            PetBreedLibrary.byId("cat_shorthair") ?: PetBreedLibrary.byKind(PetKind.CAT).first()
        }
        val birthDate = runCatching {
            java.time.LocalDate.parse(pet.adoptedAt)
                .minusYears(1)
                .toString()
        }.getOrDefault(java.time.LocalDate.now().minusYears(1).toString())
        viewModelScope.launch {
            repository.addPet(
                HealthPetProfile(
                    id = hostId,
                    kind = kind,
                    name = pet.name,
                    gender = PetGender.MALE,
                    neutered = false,
                    birthDate = birthDate,
                    breedId = healthBreed.id,
                    breedName = PetBreedsHelper.nameOf(pet.breedId),
                    weightKg = 4.0,
                    isHostPet = true,
                    createdAt = repository.now(),
                ),
            )
            // 立即选中新宠物并关闭弹窗（不等下次 dataFlow collect）
            _state.update { it.copy(showAddPet = false, currentPetId = hostId) }
            showNotice("已添加电子宠物 ${pet.name} 的健康档案")
        }
    }

    fun addPet(
        kind: PetKind, name: String, gender: PetGender, neutered: Boolean,
        birthDate: String, breedId: String, weightKg: Double,
        foodBans: List<String>,
    ) {
        if (name.isBlank() || birthDate.isBlank() || weightKg <= 0) {
            showNotice("请完整填写必填项")
            return
        }
        val breed = PetBreedLibrary.byId(breedId)
            ?: PetBreedLibrary.byKind(kind).first()
        val newPetId = "pet_${System.currentTimeMillis()}"
        viewModelScope.launch {
            repository.addPet(
                HealthPetProfile(
                    id = newPetId,
                    kind = kind, name = name.trim(), gender = gender, neutered = neutered,
                    birthDate = birthDate, breedId = breed.id, breedName = breed.name,
                    weightKg = weightKg, foodBans = foodBans,
                    isHostPet = false, createdAt = repository.now(),
                ),
            )
            _state.update { it.copy(showAddPet = false, currentPetId = newPetId) }
            showNotice("已添加 ${name.trim()} 的健康档案")
        }
    }

    fun removePet(petId: String) {
        val pet = _state.value.data.pets.firstOrNull { it.id == petId }
        if (pet?.isHostPet == true) {
            showNotice("宿主已领养宠物不可删除")
            return
        }
        viewModelScope.launch {
            repository.removePet(petId)
            showNotice("已删除宠物档案及全部记录")
        }
    }

    // ---- 记录 ----

    fun addMeal(foodName: String, grams: Double, calories: Double, appetite: String) {
        val petId = _state.value.currentPetId ?: return
        viewModelScope.launch {
            repository.addMeal(
                HealthMealRecord(
                    id = System.currentTimeMillis(), petId = petId,
                    date = repository.today(), foodName = foodName, grams = grams,
                    calories = calories, appetite = appetite, time = repository.now(),
                ),
            )
            runAlertChecks(petId)
        }
    }

    fun addWater(ml: Int) {
        val petId = _state.value.currentPetId ?: return
        viewModelScope.launch {
            repository.addWater(
                WaterRecord(System.currentTimeMillis(), petId, repository.today(), ml, repository.now()),
            )
            runAlertChecks(petId)
        }
    }

    fun addPoop(type: String, count: Int, bristol: Int, urineAmount: String, hasBlood: Boolean, hasMucus: Boolean, abnormal: Boolean) {
        val petId = _state.value.currentPetId ?: return
        viewModelScope.launch {
            repository.addPoop(
                PoopRecord(
                    id = System.currentTimeMillis(), petId = petId, date = repository.today(),
                    type = type, count = count, bristol = bristol, urineAmount = urineAmount,
                    hasBlood = hasBlood, hasMucus = hasMucus, abnormal = abnormal,
                    time = repository.now(),
                ),
            )
            runAlertChecks(petId)
        }
    }

    fun addWeight(weightKg: Double) {
        val petId = _state.value.currentPetId ?: return
        viewModelScope.launch {
            repository.addWeight(
                HealthWeightRecord(System.currentTimeMillis(), petId, repository.today(), weightKg),
            )
            runAlertChecks(petId)
        }
    }

    // ---- 疫苗 / 驱虫 ----

    fun addVaccine(vaccineName: String, date: String, nextDate: String) {
        val petId = _state.value.currentPetId ?: return
        viewModelScope.launch {
            repository.addVaccine(
                VaccineRecord(System.currentTimeMillis(), petId, "custom", vaccineName, date, nextDate),
            )
            runAlertChecks(petId)
        }
    }

    fun addDeworm(type: String, drugName: String, date: String, nextDate: String) {
        val petId = _state.value.currentPetId ?: return
        viewModelScope.launch {
            repository.addDeworm(
                DewormRecord(System.currentTimeMillis(), petId, type, drugName, date, nextDate),
            )
            runAlertChecks(petId)
        }
    }

    // ---- 症状自查 ----

    fun selfCheck(symptoms: List<String>) {
        val pet = currentPet() ?: return
        val result = SelfCheckLibrary.evaluate(pet.kind, symptoms)
            .copy(petId = pet.id)
        viewModelScope.launch {
            repository.addSelfCheck(result)
            if (result.level == "红") {
                repository.addAlert(
                    HealthAlert(
                        id = System.currentTimeMillis() + 99,
                        petId = pet.id,
                        title = "症状自查：红色预警",
                        detail = "症状：${symptoms.joinToString("、")}。${result.advice}",
                        priority = "红", date = repository.today(), source = "selfcheck",
                    ),
                )
            }
        }
    }

    fun resolveAlert(id: Long) {
        viewModelScope.launch { repository.resolveAlert(id) }
    }

    // ---- 预警引擎 ----

    private suspend fun runAlertChecks(petId: String) {
        val data = _state.value.data
        val pet = data.pets.firstOrNull { it.id == petId } ?: return
        val alerts = mutableListOf<HealthAlert>()

        val mealsByDate = data.meals.filter { it.petId == petId }.groupBy { it.date }
        HealthAlertRules.appetiteAlert(mealsByDate, petId)?.let { alerts += it }

        val poops = data.poops.filter { it.petId == petId }
        HealthAlertRules.poopAlert(poops, petId, pet.kind)?.let { alerts += it }

        val weights = data.weights.filter { it.petId == petId }
        HealthAlertRules.weightAlert(weights, petId)?.let { alerts += it }

        val waters = data.waters.filter { it.petId == petId }
        HealthAlertRules.waterAlert(waters, petId)?.let { alerts += it }

        val vaccines = data.vaccines.filter { it.petId == petId }
        val deworms = data.deworms.filter { it.petId == petId }
        alerts += HealthAlertRules.overdueAlerts(vaccines, deworms, petId)

        alerts.forEach { repository.addAlert(it) }
    }

    /** 健康评分（本周）。 */
    fun healthScore(petId: String): Int {
        val data = _state.value.data
        val today = LocalDate.now()
        val weekStart = today.minusDays(6).toString()
        val petData = { list: List<*> -> list.count { it is HealthMealRecord && it.petId == petId && it.date >= weekStart } }

        val hasMeal = data.meals.any { it.petId == petId && it.date >= weekStart }
        val hasWater = data.waters.any { it.petId == petId && it.date >= weekStart }
        val hasPoop = data.poops.any { it.petId == petId && it.date >= weekStart }
        val hasWeight = data.weights.any { it.petId == petId && it.date >= weekStart }
        val abnormalCount = data.alerts.count { it.petId == petId && !it.resolved && it.date >= weekStart }
        val redAlert = data.alerts.any { it.petId == petId && !it.resolved && it.priority == "红" }
        return HealthAlertRules.healthScore(hasMeal, hasWater, hasPoop, hasWeight, abnormalCount, redAlert)
    }

    /** 今日待办（用户自建，按宠物过滤）。 */
    fun todayTodos(pet: HealthPetProfile): List<HealthTodo> =
        _state.value.data.todos.filter { it.petId == pet.id }

    fun addTodo(text: String) {
        val petId = _state.value.currentPetId ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addTodo(petId, text)
        }
    }

    fun toggleTodo(todoId: Long) {
        viewModelScope.launch { repository.toggleTodo(todoId) }
    }

    fun removeTodo(todoId: Long) {
        viewModelScope.launch { repository.removeTodo(todoId) }
    }

    /** 上次记录距今天数（温和提醒用）。 */
    fun daysSinceLastRecord(petId: String): Int {
        val data = _state.value.data
        val lastDates = listOf(
            data.meals.filter { it.petId == petId }.maxOfOrNull { it.date },
            data.waters.filter { it.petId == petId }.maxOfOrNull { it.date },
            data.poops.filter { it.petId == petId }.maxOfOrNull { it.date },
        ).filterNotNull().maxOrNull() ?: return 0
        return runCatching {
            val days = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.parse(lastDates), LocalDate.now())
            days.toInt().coerceAtLeast(0)
        }.getOrDefault(0)
    }

    fun today(): String = repository.today()

    private fun showNotice(message: String) = _state.update { it.copy(notice = message) }
    fun consumeNotice() = _state.update { it.copy(notice = null) }
}

/** 宿主领养宠物信息适配（breedId → 名称/类型）。 */
private object PetBreedsHelper {
    fun typeOf(breedId: String): PetType =
        cn.petpet.app.model.PetBreeds.byId(breedId)?.type ?: PetType.CAT

    fun nameOf(breedId: String): String =
        cn.petpet.app.model.PetBreeds.byId(breedId)?.name ?: "宠物"
}
