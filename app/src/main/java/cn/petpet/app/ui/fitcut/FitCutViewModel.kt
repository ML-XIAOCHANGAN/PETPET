package cn.petpet.app.ui.fitcut

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.petpet.app.data.fitcut.Achievement
import cn.petpet.app.data.fitcut.ActivityLevel
import cn.petpet.app.data.fitcut.FitCutCalculator
import cn.petpet.app.data.fitcut.FitCutTodo
import cn.petpet.app.data.fitcut.FitCutLocalData
import cn.petpet.app.data.fitcut.FitCutPlan
import cn.petpet.app.data.fitcut.FitCutRepository
import cn.petpet.app.data.fitcut.FoodItem
import cn.petpet.app.data.fitcut.Gender
import cn.petpet.app.data.fitcut.MealRecord
import cn.petpet.app.data.fitcut.MealType
import cn.petpet.app.data.fitcut.UserProfile
import cn.petpet.app.data.fitcut.WeightRecord
import cn.petpet.app.data.fitcut.WorkoutRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** FitCut UI 状态。 */
data class FitCutUiState(
    val data: FitCutLocalData = FitCutLocalData(null, emptyList(), emptyList(), emptyList(), emptyList()),
    val plan: FitCutPlan? = null,
    val achievements: List<Achievement> = emptyList(),
    val onboardingStep: Int = 0,
    // 引导页输入草稿
    val draftGender: Gender = Gender.MALE,
    val draftAge: String = "",
    val draftHeight: String = "",
    val draftWeight: String = "",
    val draftTargetWeight: String = "",
    val draftActivity: ActivityLevel = ActivityLevel.LIGHT,
    val notice: String? = null,
)

class FitCutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FitCutRepository(application)

    private val _state = MutableStateFlow(FitCutUiState())
    val state: StateFlow<FitCutUiState> = _state.asStateFlow()

    /** 底部 tab 切换（由 Screen 内部持有，不进 StateFlow）。 */
    var currentTab: Int = 0

    init {
        viewModelScope.launch {
            repository.dataFlow.collect { data ->
                val profile = data.profile
                val plan = profile?.let { FitCutCalculator.buildPlan(it) }
                _state.update {
                    it.copy(
                        data = data,
                        plan = plan,
                        achievements = buildAchievements(data, plan),
                        onboardingStep = if (profile == null) 0 else 4,
                    )
                }
            }
        }
    }

    // ---- 引导页输入 ----

    fun setDraftGender(gender: Gender) = _state.update { it.copy(draftGender = gender) }
    fun setDraftAge(v: String) = _state.update { it.copy(draftAge = v.filter(Char::isDigit)) }
    fun setDraftHeight(v: String) = _state.update { it.copy(draftHeight = v.filter(Char::isDigit)) }
    fun setDraftWeight(v: String) = _state.update { it.copy(draftWeight = v.filter { it.isDigit() || it == '.' }) }
    fun setDraftTargetWeight(v: String) = _state.update { it.copy(draftTargetWeight = v.filter { it.isDigit() || it == '.' }) }
    fun setDraftActivity(activity: ActivityLevel) = _state.update { it.copy(draftActivity = activity) }

    fun nextOnboardingStep() = _state.update { it.copy(onboardingStep = it.onboardingStep + 1) }
    fun prevOnboardingStep() = _state.update { it.copy(onboardingStep = (it.onboardingStep - 1).coerceAtLeast(0)) }

    /** 校验并保存档案。 */
    fun saveProfile() {
        val s = _state.value
        val age = s.draftAge.toIntOrNull()
        val height = s.draftHeight.toIntOrNull()
        val weight = s.draftWeight.toDoubleOrNull()
        val target = s.draftTargetWeight.toDoubleOrNull()
        when {
            age == null || age !in 12..100 -> showNotice("请输入 12-100 岁的年龄")
            height == null || height !in 100..250 -> showNotice("请输入 100-250cm 的身高")
            weight == null || weight !in 30.0..300.0 -> showNotice("请输入 30-300kg 的当前体重")
            target == null || target !in 30.0..300.0 -> showNotice("请输入 30-300kg 的目标体重")
            target >= weight -> showNotice("目标体重应低于当前体重（减脂模式）")
            else -> {
                viewModelScope.launch {
                    repository.saveProfile(
                        UserProfile(
                            gender = s.draftGender,
                            age = age,
                            heightCm = height,
                            weightKg = FitCutCalculator.round1(weight),
                            targetWeightKg = FitCutCalculator.round1(target),
                            activity = s.draftActivity,
                            createdAt = repository.now(),
                        ),
                    )
                    // 建档即记录首日体重
                    repository.addWeightRecord(
                        WeightRecord(
                            id = System.currentTimeMillis(),
                            date = repository.today(),
                            weightKg = FitCutCalculator.round1(weight),
                            createdAt = repository.now(),
                        ),
                    )
                }
            }
        }
    }

    /** 修改档案：回退到引导第一步并预填现有值。 */
    fun editProfile() {
        val p = _state.value.data.profile ?: return
        _state.update {
            it.copy(
                onboardingStep = 0,
                draftGender = p.gender,
                draftAge = p.age.toString(),
                draftHeight = p.heightCm.toString(),
                draftWeight = p.weightKg.toString(),
                draftTargetWeight = p.targetWeightKg.toString(),
                draftActivity = p.activity,
            )
        }
    }

    // ---- 饮食 ----

    /** 添加饮食记录（foodId 来自内置库或自定义）。 */
    fun addMeal(mealType: MealType, food: FoodItem, grams: Double) {
        val s = _state.value
        val ratio = grams / 100.0
        val record = MealRecord(
            id = System.currentTimeMillis(),
            date = repository.today(),
            mealType = mealType,
            foodId = food.id,
            foodName = food.name,
            grams = grams,
            calories = FitCutCalculator.round1(food.caloriesPer100g * ratio),
            protein = FitCutCalculator.round1(food.proteinPer100g * ratio),
            carb = FitCutCalculator.round1(food.carbPer100g * ratio),
            fat = FitCutCalculator.round1(food.fatPer100g * ratio),
            createdAt = repository.now(),
        )
        viewModelScope.launch { repository.addMealRecord(record) }
    }

    /** 自定义食物并立即添加一条记录。 */
    fun addCustomFoodWithMeal(
        mealType: MealType,
        name: String,
        caloriesPer100g: Double,
        protein: Double,
        carb: Double,
        fat: Double,
        grams: Double,
    ) {
        val food = FoodItem(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = protein,
            carbPer100g = carb,
            fatPer100g = fat,
            isCustom = true,
        )
        viewModelScope.launch {
            repository.addCustomFood(food)
            addMeal(mealType, food, grams)
        }
    }

    fun removeMeal(id: Long) {
        viewModelScope.launch { repository.removeMealRecord(id) }
    }

    // ---- 运动 ----

    /** 运动打卡：按类别估算消耗并记录。 */
    fun logWorkout(exerciseName: String, category: String, minutes: Int) {
        val profile = _state.value.data.profile ?: return
        val calories = FitCutCalculator.round1(
            FitCutCalculator.workoutCalories(category, minutes, profile.weightKg),
        )
        viewModelScope.launch {
            repository.addWorkoutRecord(
                WorkoutRecord(
                    id = System.currentTimeMillis(),
                    date = repository.today(),
                    exerciseName = exerciseName,
                    category = category,
                    minutes = minutes,
                    calories = calories,
                    createdAt = repository.now(),
                ),
            )
        }
    }

    // ---- 体重 ----

    /** 记录今日体重（覆盖同日）。 */
    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            repository.addWeightRecord(
                WeightRecord(
                    id = System.currentTimeMillis(),
                    date = repository.today(),
                    weightKg = FitCutCalculator.round1(weightKg),
                    createdAt = repository.now(),
                ),
            )
        }
    }

    /** 今日日期（yyyy-MM-dd）。 */
    fun today(): String = repository.today()

    /** 重置：清空全部 FitCut 数据。 */
    fun resetAll() {
        viewModelScope.launch {
            repository.resetAll()
            _state.update {
                it.copy(
                    onboardingStep = 0,
                    draftGender = Gender.MALE,
                    draftAge = "", draftHeight = "", draftWeight = "", draftTargetWeight = "",
                    draftActivity = ActivityLevel.LIGHT,
                )
            }
        }
    }

    // ---- 成就推导 ----

    private fun buildAchievements(data: FitCutLocalData, plan: FitCutPlan?): List<Achievement> {
        val weights = data.weightRecords
        val workouts = data.workoutRecords
        val meals = data.mealRecords

        val firstWeight = weights.minByOrNull { it.date }?.weightKg
        val lastWeight = weights.maxByOrNull { it.date }?.weightKg
        val lostKg = if (firstWeight != null && lastWeight != null) {
            (firstWeight - lastWeight).coerceAtLeast(0.0)
        } else 0.0

        val workoutDates = workouts.map { it.date }.toSet()
        val streak = consecutiveDays(workoutDates)

        val goalCalories = plan?.recommendedCalories
        val onTargetDays = if (goalCalories != null) {
            meals.groupBy { it.date }.count { (_, dayMeals) ->
                dayMeals.sumOf { it.calories } <= goalCalories
            }
        } else 0

        return listOf(
            Achievement("first_meal", "初次记录", "🍽️", meals.isNotEmpty(), if (meals.isNotEmpty()) 1 else 0, 1),
            Achievement("first_workout", "动起来", "🏃", workouts.isNotEmpty(), if (workouts.isNotEmpty()) 1 else 0, 1),
            Achievement("lost_2kg", "减重 2kg", "🎯", lostKg >= 2.0, lostKg.toInt(), 2),
            Achievement("lost_5kg", "减重 5kg", "🏆", lostKg >= 5.0, lostKg.toInt(), 5),
            Achievement("streak_7", "连续打卡 7 天", "🔥", streak >= 7, streak, 7),
            Achievement("streak_30", "连续打卡 30 天", "💪", streak >= 30, streak, 30),
            Achievement("on_target_7", "达标 7 天", "✅", onTargetDays >= 7, onTargetDays, 7),
        )
    }

    /** 从打卡日期集合推导最长连续天数（含今天向前的连续天数）。 */
    private fun consecutiveDays(dates: Set<String>): Int {
        if (dates.isEmpty()) return 0
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        var count = 0
        var day = LocalDate.now()
        while (dates.contains(day.format(fmt))) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    private fun showNotice(message: String) {
        _state.update { it.copy(notice = message) }
    }

    /** 消费一次性提示。 */
    fun consumeNotice() = _state.update { it.copy(notice = null) }

    // ---- 今日待办（用户自建） ----

    val todos: List<FitCutTodo> get() = _state.value.data.todos

    fun addTodo(text: String, date: String = repository.today()) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.addTodo(text, date) }
    }

    fun updateTodo(todoId: Long, text: String, date: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.updateTodo(todoId, text, date) }
    }

    fun toggleTodo(todoId: Long) {
        viewModelScope.launch { repository.toggleTodo(todoId) }
    }

    fun removeTodo(todoId: Long) {
        viewModelScope.launch { repository.removeTodo(todoId) }
    }
}
