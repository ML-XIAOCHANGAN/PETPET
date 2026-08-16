package cn.petpet.app.data.fitcut

import kotlin.math.roundToInt

/** 减脂计划核心计算结果。 */
data class FitCutPlan(
    val bmr: Double,
    val tdee: Double,
    val calorieDeficit: Double,
    val recommendedCalories: Double,
    val targetProteinG: Double,
    val targetCarbG: Double,
    val targetFatG: Double,
    val weeklyTargetKg: Double,
    val totalWeeks: Int,
    val isMaintenance: Boolean,
)

/**
 * 减脂核心计算器（Mifflin-St Jeor 公式）。
 *
 * 全部为纯函数 + 静态常量，方便单测断言。
 */
object FitCutCalculator {

    /** 每公斤脂肪约需消耗的热量（kcal）。 */
    const val KCAL_PER_KG_FAT = 7700.0

    /** 建议热量缺口范围（kcal/天）。 */
    const val DEFICIT_MIN = 300.0
    const val DEFICIT_MAX = 500.0

    /** 健康减重区间（kg/周）。 */
    const val WEEKLY_MIN = 0.5
    const val WEEKLY_MAX = 1.0

    /** 计划周数范围。 */
    const val WEEKS_MIN = 4
    const val WEEKS_MAX = 12

    /** 营养素供能占比。 */
    const val PROTEIN_RATIO = 0.30
    const val CARB_RATIO = 0.40
    const val FAT_RATIO = 0.30

    private const val KCAL_PER_G_PROTEIN = 4.0
    private const val KCAL_PER_G_CARB = 4.0
    private const val KCAL_PER_G_FAT = 9.0

    /** Mifflin-St Jeor 基础代谢率。 */
    fun bmr(profile: UserProfile): Double {
        val base = 10.0 * profile.weightKg + 6.25 * profile.heightCm - 5.0 * profile.age
        return when (profile.gender) {
            Gender.MALE -> base + 5.0
            Gender.FEMALE -> base - 161.0
        }
    }

    /** 每日总消耗 = BMR × 活动系数。 */
    fun tdee(profile: UserProfile): Double = bmr(profile) * profile.activity.factor

    /** 每日推荐热量缺口：由目标体重差与计划周数推导，clamp 到 300-500。 */
    fun calorieDeficit(profile: UserProfile, weeks: Int): Double {
        val diffKg = profile.weightKg - profile.targetWeightKg
        if (diffKg <= 0) return 0.0
        val deficit = diffKg * KCAL_PER_KG_FAT / (weeks * 7.0)
        return deficit.coerceIn(DEFICIT_MIN, DEFICIT_MAX)
    }

    /** 推荐计划周数：按每周约 3500 kcal 缺口（≈0.45kg）估算，clamp 4-12 周。 */
    fun planWeeks(profile: UserProfile): Int {
        val diffKg = profile.weightKg - profile.targetWeightKg
        if (diffKg <= 0) return WEEKS_MIN
        val weeks = (diffKg * KCAL_PER_KG_FAT / 3500.0).roundToInt()
        return weeks.coerceIn(WEEKS_MIN, WEEKS_MAX)
    }

    /** 每周目标减重（健康区间 0.5-1kg）。 */
    fun weeklyTarget(profile: UserProfile, weeks: Int): Double {
        val diffKg = profile.weightKg - profile.targetWeightKg
        if (diffKg <= 0) return 0.0
        return (diffKg / weeks).coerceIn(WEEKLY_MIN, WEEKLY_MAX)
    }

    /** 生成完整减脂计划。 */
    fun buildPlan(profile: UserProfile): FitCutPlan {
        val weeks = planWeeks(profile)
        val deficit = calorieDeficit(profile, weeks)
        val tdee = tdee(profile)
        val recommended = tdee - deficit
        val isMaintenance = deficit <= 0.0
        return FitCutPlan(
            bmr = bmr(profile),
            tdee = tdee,
            calorieDeficit = deficit,
            recommendedCalories = recommended,
            targetProteinG = recommended * PROTEIN_RATIO / KCAL_PER_G_PROTEIN,
            targetCarbG = recommended * CARB_RATIO / KCAL_PER_G_CARB,
            targetFatG = recommended * FAT_RATIO / KCAL_PER_G_FAT,
            weeklyTargetKg = weeklyTarget(profile, weeks),
            totalWeeks = weeks,
            isMaintenance = isMaintenance,
        )
    }

    /** BMI = 体重kg / (身高m)²。 */
    fun bmi(weightKg: Double, heightCm: Int): Double {
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    /** BMI 档位标签。 */
    fun bmiCategory(bmi: Double): String = when {
        bmi < 18.5 -> "偏瘦"
        bmi < 24.0 -> "正常"
        bmi < 28.0 -> "偏胖"
        else -> "肥胖"
    }

    /** 运动消耗估算：有氧按固定 kcal/min，力量按 0.12 kcal/min/kg。 */
    fun workoutCalories(category: String, minutes: Int, weightKg: Double): Double = when {
        category == "有氧" -> minutes * 0.12 * weightKg
        else -> minutes * 0.08 * weightKg
    }

    /** 四舍五入到一位小数。 */
    fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0
}
