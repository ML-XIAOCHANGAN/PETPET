package cn.petpet.app.data.fitcut

/** 性别。 */
enum class Gender(val label: String) {
    MALE("男"),
    FEMALE("女"),
}

/** 活动水平（对应 TDEE 系数）。 */
enum class ActivityLevel(val factor: Double, val label: String) {
    SEDENTARY(1.2, "久坐"),
    LIGHT(1.375, "轻度活动"),
    MODERATE(1.55, "中度活动"),
    HEAVY(1.725, "重度活动"),
}

/** 三餐分类。 */
enum class MealType(val label: String, val emoji: String) {
    BREAKFAST("早餐", "🌅"),
    LUNCH("午餐", "☀️"),
    DINNER("晚餐", "🌙"),
    SNACK("加餐", "🍎"),
}

/** 用户档案。 */
data class UserProfile(
    val gender: Gender,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val targetWeightKg: Double,
    val activity: ActivityLevel,
    val createdAt: String,
)

/** 食物条目（内置库与自定义共用）。 */
data class FoodItem(
    val id: String,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbPer100g: Double,
    val fatPer100g: Double,
    val isCustom: Boolean = false,
)

/** 一条饮食记录（按实际克数折算后的营养快照）。 */
data class MealRecord(
    val id: Long,
    val date: String,
    val mealType: MealType,
    val foodId: String,
    val foodName: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carb: Double,
    val fat: Double,
    val createdAt: String,
)

/** 一条运动打卡记录。 */
data class WorkoutRecord(
    val id: Long,
    val date: String,
    val exerciseName: String,
    val category: String,
    val minutes: Int,
    val calories: Double,
    val createdAt: String,
)

/** 一条体重记录。 */
data class WeightRecord(
    val id: Long,
    val date: String,
    val weightKg: Double,
    val createdAt: String,
)

/** 成就（计算型，解锁状态由数据实时推导）。 */
data class Achievement(
    val id: String,
    val title: String,
    val icon: String,
    val unlocked: Boolean,
    val progress: Int,
    val target: Int,
)
