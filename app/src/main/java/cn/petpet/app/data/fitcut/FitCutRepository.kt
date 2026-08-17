package cn.petpet.app.data.fitcut

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

private val Context.fitCutDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fitcut_preferences",
    produceMigrations = { _ -> listOf() },
)

/** FitCut 全量本地数据（从 DataStore 反序列化的内存态）。 */
data class FitCutLocalData(
    val profile: UserProfile?,
    val customFoods: List<FoodItem>,
    val mealRecords: List<MealRecord>,
    val workoutRecords: List<WorkoutRecord>,
    val weightRecords: List<WeightRecord>,
    val todos: List<FitCutTodo> = emptyList(),
)

/**
 * FitCut 减脂助手 Repository：独立 DataStore 文件 + JSON 编解码。
 * 沿用项目 PetPetPreferencesRepository 的损坏容错模式（先备份再继续写入）。
 */
class FitCutRepository(context: Context) {

    private val dataStore = context.fitCutDataStore
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 全量数据流：档案 + 自定义食物 + 三餐 + 运动 + 体重。 */
    val dataFlow: Flow<FitCutLocalData> = dataStore.data
        .catch { exception ->
            if (exception is IOException || exception is CorruptionException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            FitCutLocalData(
                profile = FitCutJson.decodeProfile(prefs[KEY_PROFILE]),
                customFoods = FitCutJson.decodeFoods(prefs[KEY_CUSTOM_FOODS]),
                mealRecords = FitCutJson.decodeMeals(prefs[KEY_MEAL_RECORDS]),
                workoutRecords = FitCutJson.decodeWorkouts(prefs[KEY_WORKOUT_RECORDS]),
                weightRecords = FitCutJson.decodeWeights(prefs[KEY_WEIGHT_RECORDS]),
                todos = FitCutJson.decodeTodos(prefs[KEY_TODOS]),
            )
        }

    /** 保存用户档案（首次建档或修改）。 */
    suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[KEY_PROFILE] = FitCutJson.encodeProfile(profile)
        }
    }

    /** 追加自定义食物。 */
    suspend fun addCustomFood(food: FoodItem) {
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeFoods(prefs[KEY_CUSTOM_FOODS])
            prefs[KEY_CUSTOM_FOODS] = FitCutJson.encodeFoods(current + food)
        }
    }

    /** 新增一条饮食记录。 */
    suspend fun addMealRecord(record: MealRecord) {
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeMeals(prefs[KEY_MEAL_RECORDS])
            prefs[KEY_MEAL_RECORDS] = FitCutJson.encodeMeals(current + record)
        }
    }

    /** 删除一条饮食记录。 */
    suspend fun removeMealRecord(id: Long) {
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeMeals(prefs[KEY_MEAL_RECORDS])
            prefs[KEY_MEAL_RECORDS] = FitCutJson.encodeMeals(current.filterNot { it.id == id })
        }
    }

    /** 新增一条运动打卡。 */
    suspend fun addWorkoutRecord(record: WorkoutRecord) {
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeWorkouts(prefs[KEY_WORKOUT_RECORDS])
            prefs[KEY_WORKOUT_RECORDS] = FitCutJson.encodeWorkouts(current + record)
        }
    }

    /** 新增一条体重记录（同一天重复则覆盖）。 */
    suspend fun addWeightRecord(record: WeightRecord) {
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeWeights(prefs[KEY_WEIGHT_RECORDS])
            val filtered = current.filterNot { it.date == record.date }
            prefs[KEY_WEIGHT_RECORDS] = FitCutJson.encodeWeights(filtered + record)
        }
    }

    /** 清空全部 FitCut 数据（重置）。 */
    suspend fun resetAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_PROFILE)
            prefs.remove(KEY_CUSTOM_FOODS)
            prefs.remove(KEY_MEAL_RECORDS)
            prefs.remove(KEY_WORKOUT_RECORDS)
            prefs.remove(KEY_WEIGHT_RECORDS)
        }
    }

    /** 今日日期（yyyy-MM-dd）。 */
    fun today(): String = LocalDate.now().format(dateFormatter)

    /** 当前时间戳（yyyy-MM-dd HH:mm）。 */
    fun now(): String = java.time.LocalDateTime.now().format(timeFormatter)

    // ---- 待办事项（用户自建） ----

    suspend fun addTodo(text: String, date: String = today()) {
        if (text.isBlank()) return
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = FitCutJson.encodeTodos(
                current + FitCutTodo(
                    id = System.currentTimeMillis(),
                    text = text.trim(),
                    date = date,
                ),
            )
        }
    }

    suspend fun updateTodo(todoId: Long, text: String, date: String) {
        if (text.isBlank()) return
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = FitCutJson.encodeTodos(
                current.map { if (it.id == todoId) it.copy(text = text.trim(), date = date) else it },
            )
        }
    }

    suspend fun toggleTodo(todoId: Long) {
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = FitCutJson.encodeTodos(
                current.map { if (it.id == todoId) it.copy(done = !it.done) else it },
            )
        }
    }

    suspend fun removeTodo(todoId: Long) {
        dataStore.edit { prefs ->
            val current = FitCutJson.decodeTodos(prefs[KEY_TODOS])
            prefs[KEY_TODOS] = FitCutJson.encodeTodos(current.filterNot { it.id == todoId })
        }
    }

    private companion object {
        val KEY_PROFILE = stringPreferencesKey("fitcut_profile")
        val KEY_CUSTOM_FOODS = stringPreferencesKey("fitcut_custom_foods")
        val KEY_MEAL_RECORDS = stringPreferencesKey("fitcut_meal_records")
        val KEY_WORKOUT_RECORDS = stringPreferencesKey("fitcut_workout_records")
        val KEY_WEIGHT_RECORDS = stringPreferencesKey("fitcut_weight_records")
        val KEY_TODOS = stringPreferencesKey("fitcut_todos")
    }
}

/** FitCut JSON 编解码（集中管理，损坏时返回空值而非崩溃）。 */
internal object FitCutJson {

    fun encodeProfile(p: UserProfile): String = JSONObject()
        .put("gender", p.gender.name)
        .put("age", p.age)
        .put("heightCm", p.heightCm)
        .put("weightKg", p.weightKg)
        .put("targetWeightKg", p.targetWeightKg)
        .put("activity", p.activity.name)
        .put("createdAt", p.createdAt)
        .toString()

    fun decodeProfile(raw: String?): UserProfile? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(raw)
            UserProfile(
                gender = Gender.valueOf(o.getString("gender")),
                age = o.getInt("age"),
                heightCm = o.getInt("heightCm"),
                weightKg = o.getDouble("weightKg"),
                targetWeightKg = o.getDouble("targetWeightKg"),
                activity = ActivityLevel.valueOf(o.getString("activity")),
                createdAt = o.getString("createdAt"),
            )
        }.getOrNull()
    }

    fun encodeFoods(foods: List<FoodItem>): String {
        val arr = JSONArray()
        foods.forEach { f ->
            arr.put(
                JSONObject()
                    .put("id", f.id)
                    .put("name", f.name)
                    .put("caloriesPer100g", f.caloriesPer100g)
                    .put("proteinPer100g", f.proteinPer100g)
                    .put("carbPer100g", f.carbPer100g)
                    .put("fatPer100g", f.fatPer100g)
                    .put("isCustom", true),
            )
        }
        return arr.toString()
    }

    fun decodeFoods(raw: String?): List<FoodItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(
                        FoodItem(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            caloriesPer100g = o.getDouble("caloriesPer100g"),
                            proteinPer100g = o.getDouble("proteinPer100g"),
                            carbPer100g = o.getDouble("carbPer100g"),
                            fatPer100g = o.getDouble("fatPer100g"),
                            isCustom = true,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeMeals(records: List<MealRecord>): String {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("date", r.date)
                    .put("mealType", r.mealType.name)
                    .put("foodId", r.foodId)
                    .put("foodName", r.foodName)
                    .put("grams", r.grams)
                    .put("calories", r.calories)
                    .put("protein", r.protein)
                    .put("carb", r.carb)
                    .put("fat", r.fat)
                    .put("createdAt", r.createdAt),
            )
        }
        return arr.toString()
    }

    fun decodeMeals(raw: String?): List<MealRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(
                        MealRecord(
                            id = o.getLong("id"),
                            date = o.getString("date"),
                            mealType = MealType.valueOf(o.getString("mealType")),
                            foodId = o.getString("foodId"),
                            foodName = o.getString("foodName"),
                            grams = o.getDouble("grams"),
                            calories = o.getDouble("calories"),
                            protein = o.getDouble("protein"),
                            carb = o.getDouble("carb"),
                            fat = o.getDouble("fat"),
                            createdAt = o.getString("createdAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeWorkouts(records: List<WorkoutRecord>): String {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("date", r.date)
                    .put("exerciseName", r.exerciseName)
                    .put("category", r.category)
                    .put("minutes", r.minutes)
                    .put("calories", r.calories)
                    .put("createdAt", r.createdAt),
            )
        }
        return arr.toString()
    }

    fun decodeWorkouts(raw: String?): List<WorkoutRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(
                        WorkoutRecord(
                            id = o.getLong("id"),
                            date = o.getString("date"),
                            exerciseName = o.getString("exerciseName"),
                            category = o.getString("category"),
                            minutes = o.getInt("minutes"),
                            calories = o.getDouble("calories"),
                            createdAt = o.getString("createdAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeWeights(records: List<WeightRecord>): String {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("date", r.date)
                    .put("weightKg", r.weightKg)
                    .put("createdAt", r.createdAt),
            )
        }
        return arr.toString()
    }

    fun decodeWeights(raw: String?): List<WeightRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(
                        WeightRecord(
                            id = o.getLong("id"),
                            date = o.getString("date"),
                            weightKg = o.getDouble("weightKg"),
                            createdAt = o.getString("createdAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeTodos(list: List<FitCutTodo>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject()
                .put("id", t.id).put("text", t.text)
                .put("date", t.date).put("done", t.done))
        }
        return arr.toString()
    }

    fun decodeTodos(raw: String?): List<FitCutTodo> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                repeat(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    add(FitCutTodo(
                        id = o.getLong("id"), text = o.getString("text"),
                        date = o.getString("date"), done = o.optBoolean("done", false)))
                }
            }
        }.getOrDefault(emptyList())
    }
}
