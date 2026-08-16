package cn.petpet.app.data.pethealth

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

/** 健康计划核心计算结果。 */
data class HealthPlan(
    val rer: Double,                 // 静息能量
    val dailyCalories: Double,       // 每日推荐热量
    val feedingTimes: Int,           // 每日喂食次数
    val mealSizeG: Double,           // 单次份量建议（g，按干粮估算）
    val foodBans: List<String>,      // 禁忌清单
    val exerciseText: String,        // 运动建议文案
    val exerciseMinutes: Int,        // 每日运动时长建议
    val vaccinePlan: List<VaccinePlanItem>,  // 疫苗时间表
    val dewormPlan: List<DewormPlanItem>,    // 驱虫时间表
    val checkupNote: String,         // 体检建议文案
    val isSenior: Boolean,
    val isOverweight: Boolean,
)

data class VaccinePlanItem(
    val vaccineName: String,
    val dueDate: String,
    val note: String = "",
)

data class DewormPlanItem(
    val type: String,                // 体内/体外
    val dueDate: String,
    val note: String = "",
)

/** 体型评估结果。 */
data class BodyCondition(
    val level: String,               // 偏瘦/标准/偏胖/肥胖
    val advice: String,
)

/**
 * 宠物健康计算器：RER 热量、体型评估、疫苗/驱虫/体检计划（猫狗差异化）。
 * 全部纯函数，便于单测。
 */
object PetHealthCalculator {

    /** RER = 70 × 体重kg^0.75（哺乳动物通用公式）。 */
    fun rer(weightKg: Double): Double = 70.0 * weightKg.pow(0.75)

    /** 每日推荐热量 = RER × 系数（按类型/绝育/年龄/活动）。 */
    fun dailyCalories(profile: HealthPetProfile, activityLevel: String = "normal"): Double {
        val base = rer(profile.weightKg)
        val ageMonths = ageInMonths(profile.birthDate)
        val senior = isSenior(profile)
        val factor = when (profile.kind) {
            PetKind.CAT -> when {
                ageMonths < 12 -> 2.5
                senior -> 1.1
                !profile.neutered -> 1.4
                else -> 1.2
            }
            PetKind.DOG -> when {
                ageMonths < 12 -> 2.5
                senior -> 1.4
                else -> when (activityLevel) {
                    "high" -> 2.0
                    "low" -> 1.4
                    else -> 1.6
                }
            }
        }
        val overweightFactor = if (isOverweight(profile)) 0.85 else 1.0
        return base * factor * overweightFactor
    }

    /** 年龄（月）。 */
    fun ageInMonths(birthDate: String): Int {
        return runCatching {
            val birth = LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            Period.between(birth, today).let { it.years * 12 + it.months }
        }.getOrDefault(12)
    }

    /** 年龄展示文本：<1 岁显示 X 月龄。 */
    fun ageText(birthDate: String): String {
        val months = ageInMonths(birthDate)
        return if (months < 12) "${months} 月龄" else {
            val y = months / 12
            val m = months % 12
            if (m == 0) "${y} 岁" else "${y} 岁 ${m} 月"
        }
    }

    /** 老年判断：猫 >10 岁，狗 >7 岁（大型犬 >5 岁）。 */
    fun isSenior(profile: HealthPetProfile): Boolean {
        val ageMonths = ageInMonths(profile.birthDate)
        return when (profile.kind) {
            PetKind.CAT -> ageMonths > 120
            PetKind.DOG -> {
                val large = profile.breedId in LARGE_BREEDS
                ageMonths > if (large) 60 else 84
            }
        }
    }

    /** 肥胖判断：体重超品种标准上限 15%。 */
    fun isOverweight(profile: HealthPetProfile): Boolean {
        val range = PetBreedLibrary.standardWeightRange(profile.breedId)
            ?: return false
        return profile.weightKg > range.second * 1.15
    }

    /** 体型评估四档。 */
    fun bodyCondition(profile: HealthPetProfile): BodyCondition {
        val range = PetBreedLibrary.standardWeightRange(profile.breedId)
        if (range == null) {
            return BodyCondition("标准", "暂未收录该品种标准体重，建议咨询兽医评估。")
        }
        val (minW, maxW) = range
        val w = profile.weightKg
        return when {
            w < minW * 0.9 -> BodyCondition("偏瘦", "体重低于标准下限 10% 以上。建议增加优质蛋白摄入，排查寄生虫/慢性消耗，咨询兽医制定增重计划。")
            w <= maxW -> BodyCondition("标准", "体重在品种标准区间内，保持当前饮食与运动节奏。")
            w <= maxW * 1.15 -> BodyCondition("偏胖", "接近标准上限。建议逐步减少零食、增加运动，避免突然节食。")
            else -> BodyCondition("肥胖", "超出标准上限 15% 以上。建议咨询兽医制定科学减重方案，避免剧烈运动伤关节。")
        }
    }

    /** 喂食次数：幼宠 3-4 次 / 成宠 2 次 / 老年 2-3 次。 */
    fun feedingTimes(profile: HealthPetProfile): Int {
        val months = ageInMonths(profile.birthDate)
        return when {
            months < 6 -> 4
            months < 12 -> 3
            isSenior(profile) -> 3
            else -> 2
        }
    }

    /** 单次份量（g，按干粮 350kcal/100g 估算）。 */
    fun mealSizeG(profile: HealthPetProfile): Double {
        val daily = dailyCalories(profile)
        val times = feedingTimes(profile)
        return (daily / times / 3.5).roundToInt().toDouble()
    }

    /** 每日运动建议。 */
    fun exerciseText(profile: HealthPetProfile): String {
        val senior = isSenior(profile)
        val overweight = isOverweight(profile)
        return when (profile.kind) {
            PetKind.CAT -> buildString {
                append("每日互动游戏 15-30 分钟，分 2-3 次（逗猫棒/小球）。")
                if (senior) append(" 老年猫缩短单次时长，轻柔互动。")
                if (overweight) append(" 循序渐进增加活动量，避免高处跳跃。")
            }
            PetKind.DOG -> {
                val size = PetBreedLibrary.sizeClassOf(profile.breedId) ?: "中型"
                val (min, max) = when (size) {
                    "小型" -> 30 to 60
                    "中型" -> 45 to 90
                    "大型" -> 60 to 120
                    else -> 90 to 150
                }
                buildString {
                    append("每日散步 ${min}-${max} 分钟（分 2 次）+ 奔跑/训练时间。")
                    if (senior) append(" 老年犬减量至 ${min / 2}-${max / 2} 分钟，注意关节。")
                    if (overweight) append(" 从短时散步开始，循序渐进，避免剧烈运动。")
                }
            }
        }
    }

    fun exerciseMinutes(profile: HealthPetProfile): Int {
        return when (profile.kind) {
            PetKind.CAT -> if (isSenior(profile)) 15 else 30
            PetKind.DOG -> {
                val size = PetBreedLibrary.sizeClassOf(profile.breedId) ?: "中型"
                val base = when (size) {
                    "小型" -> 60; "中型" -> 90; "大型" -> 120; else -> 150
                }
                if (isSenior(profile)) base / 2 else base
            }
        }
    }

    /** 疫苗时间表（按当前年龄推算下一针）。 */
    fun vaccinePlan(profile: HealthPetProfile): List<VaccinePlanItem> {
        val months = ageInMonths(profile.birthDate)
        val today = LocalDate.now()
        val items = mutableListOf<VaccinePlanItem>()
        when (profile.kind) {
            PetKind.CAT -> {
                if (months < 2) {
                    items += VaccinePlanItem("猫三联（首免第 1 针）", today.plusWeeks(1).toString(), "未满 2 月龄，离乳后 8 周龄接种")
                } else if (months < 3) {
                    items += VaccinePlanItem("猫三联（首免第 2 针）", today.plusWeeks(3).toString(), "距首针 3-4 周")
                } else if (months < 4) {
                    items += VaccinePlanItem("猫三联（首免第 3 针）", today.plusWeeks(3).toString(), "完成首免")
                    items += VaccinePlanItem("狂犬疫苗", today.plusWeeks(3).toString(), "3 月龄后可接种")
                } else {
                    items += VaccinePlanItem("猫三联（年度加强）", today.plusYears(1).toString())
                    items += VaccinePlanItem("狂犬疫苗（加强）", today.plusYears(1).toString(), "按当地法规 1-3 年")
                }
            }
            PetKind.DOG -> {
                if (months < 2) {
                    items += VaccinePlanItem("犬四联（首免第 1 针）", today.plusWeeks(1).toString(), "未满 2 月龄，6-8 周龄接种")
                } else if (months < 3) {
                    items += VaccinePlanItem("犬四联（首免第 2 针）", today.plusWeeks(3).toString(), "距首针 3-4 周")
                } else if (months < 4) {
                    items += VaccinePlanItem("犬四联（首免第 3 针）", today.plusWeeks(3).toString(), "完成首免")
                    items += VaccinePlanItem("狂犬疫苗", today.plusWeeks(3).toString(), "3 月龄后可接种")
                } else {
                    items += VaccinePlanItem("犬四联/六联/八联（年度加强）", today.plusYears(1).toString())
                    items += VaccinePlanItem("狂犬疫苗（加强）", today.plusYears(1).toString(), "按当地法规 1-3 年")
                }
            }
        }
        items += VaccinePlanItem(
            if (profile.kind == PetKind.CAT) "猫白血病疫苗（非核心）" else "钩端螺旋体疫苗（非核心）",
            "",
            "咨询兽医后选择",
        )
        return items
    }

    /** 驱虫时间表：体内幼宠每月/成宠每 3 月；体外每月（夏季可调整）。 */
    fun dewormPlan(profile: HealthPetProfile): List<DewormPlanItem> {
        val months = ageInMonths(profile.birthDate)
        val today = LocalDate.now()
        val internalMonths = if (months < 12) 1 else 3
        val items = mutableListOf<DewormPlanItem>()
        repeat(4) { i ->
            items += DewormPlanItem("体内驱虫", today.plusMonths((i * internalMonths).toLong()).toString(),
                if (i == 0) "幼宠每月 / 成宠每 3 月" else "")
            items += DewormPlanItem("体外驱虫", today.plusMonths(i.toLong()).toString(),
                if (i == 0) "每月一次（夏季/外出频繁）" else "")
        }
        return items
    }

    /** 体检建议文案。 */
    fun checkupNote(profile: HealthPetProfile): String {
        val months = ageInMonths(profile.birthDate)
        return when {
            months < 2 -> "幼宠：领养后 1 个月内做入园体检（传染病筛查+基础体格）。"
            isSenior(profile) -> "老年宠：建议每半年一次体检，增加生化、心脏、甲状腺等专项检查。"
            else -> "成宠：建议每年一次常规体检。"
        }
    }

    /** 食物禁忌（按类型）。 */
    fun foodBans(kind: PetKind): List<String> = when (kind) {
        PetKind.CAT -> listOf("洋葱/大蒜", "巧克力", "葡萄/葡萄干", "百合（剧毒）", "木糖醇", "生面团")
        PetKind.DOG -> listOf("洋葱/大蒜", "巧克力", "葡萄/葡萄干", "木糖醇", "牛油果", "夏威夷果")
    }

    /** 大型犬品种 id 集合（老年判断用）。 */
    private val LARGE_BREEDS = setOf("golden", "labrador", "german_shepherd", "berner", "husky", "border_collie")
}
