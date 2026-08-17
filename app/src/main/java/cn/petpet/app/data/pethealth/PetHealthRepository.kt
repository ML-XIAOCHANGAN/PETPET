package cn.petpet.app.data.pethealth

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.petHealthDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pethealth_preferences",
    produceMigrations = { _ -> listOf() },
)

/** Petpet 健康助手全量本地数据。 */
data class PetHealthLocalData(
    val pets: List<HealthPetProfile>,
    val vaccines: List<VaccineRecord>,
    val deworms: List<DewormRecord>,
    val meals: List<HealthMealRecord>,
    val waters: List<WaterRecord>,
    val poops: List<PoopRecord>,
    val weights: List<HealthWeightRecord>,
    val alerts: List<HealthAlert>,
    val selfChecks: List<SelfCheckResult>,
    val todos: List<HealthTodo> = emptyList(),
)

/**
 * Petpet 健康助手 Repository：独立 DataStore 文件 + JSON 编解码 + 损坏容错。
 * 沿用 FitCut / PetPetPreferencesRepository 模式。
 */
class PetHealthRepository(context: Context) {

    private val dataStore = context.petHealthDataStore
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val dataFlow: Flow<PetHealthLocalData> = dataStore.data
        .catch { exception ->
            if (exception is IOException || exception is CorruptionException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            PetHealthLocalData(
                pets = PetHealthJson.decodePets(prefs[KEY_PETS]),
                vaccines = PetHealthJson.decodeVaccines(prefs[KEY_VACCINES]),
                deworms = PetHealthJson.decodeDeworms(prefs[KEY_DEWORMS]),
                meals = PetHealthJson.decodeMeals(prefs[KEY_MEALS]),
                waters = PetHealthJson.decodeWaters(prefs[KEY_WATERS]),
                poops = PetHealthJson.decodePoops(prefs[KEY_POOPS]),
                weights = PetHealthJson.decodeWeights(prefs[KEY_WEIGHTS]),
                alerts = PetHealthJson.decodeAlerts(prefs[KEY_ALERTS]),
                selfChecks = PetHealthJson.decodeSelfChecks(prefs[KEY_SELF_CHECKS]),
                todos = PetHealthJson.decodeTodos(prefs[KEY_TODOS]),
            )
        }

    // ---- 宠物档案 ----

    suspend fun addPet(pet: HealthPetProfile) {
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodePets(prefs[KEY_PETS])
            // 按 id 去重（防止重复添加同一宠物）
            prefs[KEY_PETS] = PetHealthJson.encodePets(
                if (current.any { it.id == pet.id }) current else current + pet,
            )
        }
    }

    suspend fun updatePet(pet: HealthPetProfile) {
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodePets(prefs[KEY_PETS])
            prefs[KEY_PETS] = PetHealthJson.encodePets(
                current.map { if (it.id == pet.id) pet else it },
            )
        }
    }

    suspend fun removePet(petId: String) {
        dataStore.edit { prefs ->
            val pets = PetHealthJson.decodePets(prefs[KEY_PETS]).filterNot { it.id == petId }
            prefs[KEY_PETS] = PetHealthJson.encodePets(pets)
            // 级联删除该宠物全部数据
            prefs[KEY_VACCINES] = PetHealthJson.encodeVaccines(
                PetHealthJson.decodeVaccines(prefs[KEY_VACCINES]).filterNot { it.petId == petId })
            prefs[KEY_DEWORMS] = PetHealthJson.encodeDeworms(
                PetHealthJson.decodeDeworms(prefs[KEY_DEWORMS]).filterNot { it.petId == petId })
            prefs[KEY_MEALS] = PetHealthJson.encodeMeals(
                PetHealthJson.decodeMeals(prefs[KEY_MEALS]).filterNot { it.petId == petId })
            prefs[KEY_WATERS] = PetHealthJson.encodeWaters(
                PetHealthJson.decodeWaters(prefs[KEY_WATERS]).filterNot { it.petId == petId })
            prefs[KEY_POOPS] = PetHealthJson.encodePoops(
                PetHealthJson.decodePoops(prefs[KEY_POOPS]).filterNot { it.petId == petId })
            prefs[KEY_WEIGHTS] = PetHealthJson.encodeWeights(
                PetHealthJson.decodeWeights(prefs[KEY_WEIGHTS]).filterNot { it.petId == petId })
            prefs[KEY_ALERTS] = PetHealthJson.encodeAlerts(
                PetHealthJson.decodeAlerts(prefs[KEY_ALERTS]).filterNot { it.petId == petId })
            prefs[KEY_SELF_CHECKS] = PetHealthJson.encodeSelfChecks(
                PetHealthJson.decodeSelfChecks(prefs[KEY_SELF_CHECKS]).filterNot { it.petId == petId })
        }
    }

    // ---- 记录 CRUD（各类型 append + remove 模式）----

    suspend fun addVaccine(record: VaccineRecord) {
        dataStore.edit { prefs ->
            prefs[KEY_VACCINES] = PetHealthJson.encodeVaccines(
                PetHealthJson.decodeVaccines(prefs[KEY_VACCINES]) + record)
        }
    }

    suspend fun addDeworm(record: DewormRecord) {
        dataStore.edit { prefs ->
            prefs[KEY_DEWORMS] = PetHealthJson.encodeDeworms(
                PetHealthJson.decodeDeworms(prefs[KEY_DEWORMS]) + record)
        }
    }

    suspend fun addMeal(record: HealthMealRecord) {
        dataStore.edit { prefs ->
            prefs[KEY_MEALS] = PetHealthJson.encodeMeals(
                PetHealthJson.decodeMeals(prefs[KEY_MEALS]) + record)
        }
    }

    suspend fun addWater(record: WaterRecord) {
        dataStore.edit { prefs ->
            prefs[KEY_WATERS] = PetHealthJson.encodeWaters(
                PetHealthJson.decodeWaters(prefs[KEY_WATERS]) + record)
        }
    }

    suspend fun addPoop(record: PoopRecord) {
        dataStore.edit { prefs ->
            prefs[KEY_POOPS] = PetHealthJson.encodePoops(
                PetHealthJson.decodePoops(prefs[KEY_POOPS]) + record)
        }
    }

    suspend fun addWeight(record: HealthWeightRecord) {
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodeWeights(prefs[KEY_WEIGHTS])
            // 同日覆盖
            prefs[KEY_WEIGHTS] = PetHealthJson.encodeWeights(
                current.filterNot { it.petId == record.petId && it.date == record.date } + record)
        }
    }

    /** 追加预警（去重：同宠物同标题 3 天内不重复）。 */
    suspend fun addAlert(alert: HealthAlert) {
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodeAlerts(prefs[KEY_ALERTS])
            val duplicate = current.any {
                it.petId == alert.petId && it.title == alert.title &&
                    it.date >= LocalDate.now().minusDays(3).toString()
            }
            if (!duplicate) {
                prefs[KEY_ALERTS] = PetHealthJson.encodeAlerts(current + alert)
            }
        }
    }

    suspend fun resolveAlert(id: Long) {
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodeAlerts(prefs[KEY_ALERTS])
            prefs[KEY_ALERTS] = PetHealthJson.encodeAlerts(
                current.map { if (it.id == id) it.copy(resolved = true) else it })
        }
    }

    suspend fun addSelfCheck(result: SelfCheckResult) {
        dataStore.edit { prefs ->
            prefs[KEY_SELF_CHECKS] = PetHealthJson.encodeSelfChecks(
                PetHealthJson.decodeSelfChecks(prefs[KEY_SELF_CHECKS]) + result)
        }
    }

    /** 重置全部（调试用）。 */
    suspend fun resetAll() {
        dataStore.edit { prefs ->
            listOf(KEY_PETS, KEY_VACCINES, KEY_DEWORMS, KEY_MEALS, KEY_WATERS,
                KEY_POOPS, KEY_WEIGHTS, KEY_ALERTS, KEY_SELF_CHECKS, KEY_TODOS).forEach { prefs.remove(it) }
        }
    }

    // ---- 待办事项（用户自建） ----

    suspend fun addTodo(petId: String, text: String, date: String = today()) {
        if (text.isBlank()) return
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = PetHealthJson.encodeTodos(
                current + HealthTodo(
                    id = System.currentTimeMillis(),
                    petId = petId,
                    text = text.trim(),
                    date = date,
                ),
            )
        }
    }

    suspend fun updateTodo(todoId: Long, text: String, date: String) {
        if (text.isBlank()) return
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = PetHealthJson.encodeTodos(
                current.map { if (it.id == todoId) it.copy(text = text.trim(), date = date) else it },
            )
        }
    }

    suspend fun toggleTodo(todoId: Long) {
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = PetHealthJson.encodeTodos(
                current.map { if (it.id == todoId) it.copy(done = !it.done) else it },
            )
        }
    }

    suspend fun removeTodo(todoId: Long) {
        dataStore.edit { prefs ->
            val current = PetHealthJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = PetHealthJson.encodeTodos(current.filterNot { it.id == todoId })
        }
    }

    fun today(): String = LocalDate.now().format(dateFormatter)
    fun now(): String = java.time.LocalDateTime.now().format(timeFormatter)

    private companion object {
        val KEY_PETS = stringPreferencesKey("ph_pets")
        val KEY_VACCINES = stringPreferencesKey("ph_vaccines")
        val KEY_DEWORMS = stringPreferencesKey("ph_deworms")
        val KEY_MEALS = stringPreferencesKey("ph_meals")
        val KEY_WATERS = stringPreferencesKey("ph_waters")
        val KEY_POOPS = stringPreferencesKey("ph_poops")
        val KEY_WEIGHTS = stringPreferencesKey("ph_weights")
        val KEY_ALERTS = stringPreferencesKey("ph_alerts")
        val KEY_SELF_CHECKS = stringPreferencesKey("ph_self_checks")
        val KEY_TODOS = stringPreferencesKey("ph_todos")
    }
}

/** PetHealth JSON 编解码（损坏返回空集合）。 */
internal object PetHealthJson {

    fun encodePets(list: List<HealthPetProfile>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id).put("kind", p.kind.name).put("name", p.name)
                    .put("gender", p.gender.name).put("neutered", p.neutered)
                    .put("birthDate", p.birthDate).put("breedId", p.breedId)
                    .put("breedName", p.breedName).put("weightKg", p.weightKg)
                    .put("color", p.color).put("avatarEmoji", p.avatarEmoji)
                    .put("medicalHistory", p.medicalHistory).put("allergies", p.allergies)
                    .put("medications", p.medications)
                    .put("foodBans", JSONArray(p.foodBans))
                    .put("isHostPet", p.isHostPet).put("createdAt", p.createdAt),
            )
        }
        return arr.toString()
    }

    fun decodePets(raw: String?): List<HealthPetProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val bans = o.optJSONArray("foodBans") ?: JSONArray()
                    add(
                        HealthPetProfile(
                            id = o.getString("id"),
                            kind = PetKind.valueOf(o.getString("kind")),
                            name = o.getString("name"),
                            gender = PetGender.valueOf(o.getString("gender")),
                            neutered = o.optBoolean("neutered", false),
                            birthDate = o.getString("birthDate"),
                            breedId = o.getString("breedId"),
                            breedName = o.getString("breedName"),
                            weightKg = o.getDouble("weightKg"),
                            color = o.optString("color", ""),
                            avatarEmoji = o.optString("avatarEmoji", ""),
                            medicalHistory = o.optString("medicalHistory", ""),
                            allergies = o.optString("allergies", ""),
                            medications = o.optString("medications", ""),
                            foodBans = buildList { repeat(bans.length()) { add(bans.getString(it)) } },
                            isHostPet = o.optBoolean("isHostPet", false),
                            createdAt = o.optString("createdAt", ""),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeVaccines(list: List<VaccineRecord>): String {
        val arr = JSONArray()
        list.forEach { v ->
            arr.put(JSONObject()
                .put("id", v.id).put("petId", v.petId).put("vaccineId", v.vaccineId)
                .put("vaccineName", v.vaccineName).put("date", v.date)
                .put("nextDate", v.nextDate).put("institution", v.institution)
                .put("isOverdue", v.isOverdue))
        }
        return arr.toString()
    }

    fun decodeVaccines(raw: String?): List<VaccineRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(VaccineRecord(
                        id = o.getLong("id"), petId = o.getString("petId"),
                        vaccineId = o.getString("vaccineId"), vaccineName = o.getString("vaccineName"),
                        date = o.getString("date"), nextDate = o.optString("nextDate", ""),
                        institution = o.optString("institution", ""), isOverdue = o.optBoolean("isOverdue", false)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeDeworms(list: List<DewormRecord>): String {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(JSONObject()
                .put("id", d.id).put("petId", d.petId).put("type", d.type)
                .put("drugName", d.drugName).put("date", d.date)
                .put("nextDate", d.nextDate).put("isOverdue", d.isOverdue))
        }
        return arr.toString()
    }

    fun decodeDeworms(raw: String?): List<DewormRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(DewormRecord(
                        id = o.getLong("id"), petId = o.getString("petId"), type = o.getString("type"),
                        drugName = o.getString("drugName"), date = o.getString("date"),
                        nextDate = o.optString("nextDate", ""), isOverdue = o.optBoolean("isOverdue", false)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeMeals(list: List<HealthMealRecord>): String {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject()
                .put("id", m.id).put("petId", m.petId).put("date", m.date)
                .put("foodName", m.foodName).put("grams", m.grams)
                .put("calories", m.calories).put("appetite", m.appetite).put("time", m.time))
        }
        return arr.toString()
    }

    fun decodeMeals(raw: String?): List<HealthMealRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(HealthMealRecord(
                        id = o.getLong("id"), petId = o.getString("petId"), date = o.getString("date"),
                        foodName = o.getString("foodName"), grams = o.getDouble("grams"),
                        calories = o.getDouble("calories"), appetite = o.optString("appetite", "正常"),
                        time = o.optString("time", "")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeWaters(list: List<WaterRecord>): String {
        val arr = JSONArray()
        list.forEach { w ->
            arr.put(JSONObject()
                .put("id", w.id).put("petId", w.petId).put("date", w.date)
                .put("ml", w.ml).put("time", w.time))
        }
        return arr.toString()
    }

    fun decodeWaters(raw: String?): List<WaterRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(WaterRecord(
                        id = o.getLong("id"), petId = o.getString("petId"), date = o.getString("date"),
                        ml = o.getInt("ml"), time = o.optString("time", "")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodePoops(list: List<PoopRecord>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject()
                .put("id", p.id).put("petId", p.petId).put("date", p.date)
                .put("type", p.type).put("count", p.count).put("bristol", p.bristol)
                .put("urineAmount", p.urineAmount).put("hasBlood", p.hasBlood)
                .put("hasMucus", p.hasMucus).put("abnormal", p.abnormal).put("time", p.time))
        }
        return arr.toString()
    }

    fun decodePoops(raw: String?): List<PoopRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(PoopRecord(
                        id = o.getLong("id"), petId = o.getString("petId"), date = o.getString("date"),
                        type = o.getString("type"), count = o.getInt("count"), bristol = o.optInt("bristol", 0),
                        urineAmount = o.optString("urineAmount", ""), hasBlood = o.optBoolean("hasBlood", false),
                        hasMucus = o.optBoolean("hasMucus", false), abnormal = o.optBoolean("abnormal", false),
                        time = o.optString("time", "")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeWeights(list: List<HealthWeightRecord>): String {
        val arr = JSONArray()
        list.forEach { w ->
            arr.put(JSONObject()
                .put("id", w.id).put("petId", w.petId).put("date", w.date)
                .put("weightKg", w.weightKg))
        }
        return arr.toString()
    }

    fun decodeWeights(raw: String?): List<HealthWeightRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(HealthWeightRecord(
                        id = o.getLong("id"), petId = o.getString("petId"), date = o.getString("date"),
                        weightKg = o.getDouble("weightKg")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeAlerts(list: List<HealthAlert>): String {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject()
                .put("id", a.id).put("petId", a.petId).put("title", a.title)
                .put("detail", a.detail).put("priority", a.priority)
                .put("date", a.date).put("source", a.source).put("resolved", a.resolved))
        }
        return arr.toString()
    }

    fun decodeAlerts(raw: String?): List<HealthAlert> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(HealthAlert(
                        id = o.getLong("id"), petId = o.getString("petId"), title = o.getString("title"),
                        detail = o.getString("detail"), priority = o.optString("priority", "黄"),
                        date = o.getString("date"), source = o.optString("source", "auto"),
                        resolved = o.optBoolean("resolved", false)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeSelfChecks(list: List<SelfCheckResult>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject()
                .put("id", s.id).put("petId", s.petId)
                .put("symptoms", JSONArray(s.symptoms)).put("level", s.level)
                .put("possibleCauses", s.possibleCauses).put("advice", s.advice)
                .put("date", s.date))
        }
        return arr.toString()
    }

    fun decodeSelfChecks(raw: String?): List<SelfCheckResult> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val symptomsArr = o.optJSONArray("symptoms") ?: JSONArray()
                    add(SelfCheckResult(
                        id = o.getLong("id"), petId = o.getString("petId"),
                        symptoms = buildList { repeat(symptomsArr.length()) { add(symptomsArr.getString(it)) } },
                        level = o.getString("level"), possibleCauses = o.optString("possibleCauses", ""),
                        advice = o.optString("advice", ""), date = o.getString("date")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeTodos(list: List<HealthTodo>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject()
                .put("id", t.id).put("petId", t.petId).put("text", t.text)
                .put("date", t.date).put("done", t.done))
        }
        return arr.toString()
    }

    fun decodeTodos(raw: String?): List<HealthTodo> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(HealthTodo(
                        id = o.getLong("id"), petId = o.getString("petId"),
                        text = o.getString("text"), date = o.getString("date"),
                        done = o.optBoolean("done", false)))
                }
            }
        }.getOrDefault(emptyList())
    }
}
