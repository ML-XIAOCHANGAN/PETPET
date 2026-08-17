package cn.petpet.app.data.pethealth

/** 宠物类型（决定全部差异化规则）。 */
enum class PetKind(val label: String) {
    CAT("猫咪"),
    DOG("狗狗"),
}

/** 性别。 */
enum class PetGender(val label: String) {
    MALE("公"),
    FEMALE("母"),
}

/** 品种信息（含体型档位，狗按体型分类）。 */
data class BreedOption(
    val id: String,
    val name: String,
    val kind: PetKind,
    val sizeClass: String? = null,      // 狗：小型/中型/大型/巨型；猫为 null
    val standardWeightRange: String = "", // 标准体重区间展示文本
    val minWeightKg: Double,
    val maxWeightKg: Double,
)

/** 宠物健康档案。 */
data class HealthPetProfile(
    val id: String,
    val kind: PetKind,
    val name: String,
    val gender: PetGender,
    val neutered: Boolean,
    val birthDate: String,           // yyyy-MM-dd
    val breedId: String,             // 品种 id（对应 BreedOption / 自定义）
    val breedName: String,
    val weightKg: Double,
    val color: String = "",
    val avatarEmoji: String = "",    // 头像（本期用 emoji 占位，预留相册能力）
    val medicalHistory: String = "", // 既往病史
    val allergies: String = "",      // 过敏史
    val medications: String = "",    // 长期用药
    val foodBans: List<String> = emptyList(), // 食物禁忌（多选+自定义）
    val isHostPet: Boolean = false,  // 是否为宿主已领养宠物（不可删除）
    val createdAt: String,
)

/** 疫苗定义（按宠物类型）。 */
data class VaccineDef(
    val id: String,
    val name: String,
    val kind: PetKind,
    val isCore: Boolean,
    val note: String = "",           // 非核心疫苗标注「咨询兽医后选择」
)

/** 一条疫苗记录。 */
data class VaccineRecord(
    val id: Long,
    val petId: String,
    val vaccineId: String,
    val vaccineName: String,
    val date: String,                // 接种日期
    val nextDate: String,            // 下次接种日期（可为空）
    val institution: String = "",
    val isOverdue: Boolean = false,
)

/** 一条驱虫记录。 */
data class DewormRecord(
    val id: Long,
    val petId: String,
    val type: String,                // 体内/体外
    val drugName: String,
    val date: String,
    val nextDate: String,
    val isOverdue: Boolean = false,
)

/** 饮食记录。 */
data class HealthMealRecord(
    val id: Long,
    val petId: String,
    val date: String,
    val foodName: String,
    val grams: Double,
    val calories: Double,            // 估算 kcal
    val appetite: String,            // 正常/食欲下降/拒食
    val time: String,
)

/** 饮水记录。 */
data class WaterRecord(
    val id: Long,
    val petId: String,
    val date: String,
    val ml: Int,
    val time: String,
)

/** 排泄记录。 */
data class PoopRecord(
    val id: Long,
    val petId: String,
    val date: String,
    val type: String,                // 排便/排尿
    val count: Int,
    val bristol: Int = 0,            // 排便：布里斯托 1-7；排尿为 0
    val urineAmount: String = "",    // 排尿：多/正常/少
    val hasBlood: Boolean = false,
    val hasMucus: Boolean = false,
    val abnormal: Boolean = false,   // 用户标注异常
    val time: String,
)

/** 体重记录。 */
data class HealthWeightRecord(
    val id: Long,
    val petId: String,
    val date: String,
    val weightKg: Double,
)

/** 预警条目。 */
data class HealthAlert(
    val id: Long,
    val petId: String,
    val title: String,
    val detail: String,
    val priority: String,            // 红/黄/绿
    val date: String,
    val source: String,              // auto / selfcheck
    val resolved: Boolean = false,
)

/** 症状自查结果。 */
data class SelfCheckResult(
    val id: Long,
    val petId: String,
    val symptoms: List<String>,
    val level: String,               // 绿/黄/红
    val possibleCauses: String,
    val advice: String,
    val date: String,
)

/** 用户自定义待办事项（今日待办，2026-08 由系统自动生成改为用户自建）。 */
data class HealthTodo(
    val id: Long,
    val petId: String,
    val text: String,
    val date: String,                // yyyy-MM-dd（待办归属日）
    val done: Boolean = false,
)

/** 健康评分。 */
data class HealthScore(
    val petId: String,
    val weekScore: Int,              // 0-100
    val recordDays: Int,             // 本周有记录的天数
    val abnormalCount: Int,          // 本周异常事件数
)
