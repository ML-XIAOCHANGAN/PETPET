package cn.petpet.app.data.pethealth

/** 症状自查知识库：按宠物类型 + 症状组合 → 等级/原因/建议。 */
object SelfCheckLibrary {

    /** 严重症状（多选任一即红色预警，不展示家庭护理建议）。 */
    val SEVERE_SYMPTOMS = setOf("抽搐", "跛行", "呼吸困难", "尿血", "便血", "呕吐不止")

    private data class Rule(
        val keywords: List<String>,
        val level: String,             // 绿/黄/红
        val causes: String,
        val advice: String,
    )

    /** 通用规则（猫狗共用，严重度略有差异）。 */
    private val rules: List<Rule> = listOf(
        Rule(listOf("呕吐"), "黄", "进食过快、毛球（猫）、胃肠不适、食物不耐受",
            "禁食 4-6 小时观察，少量喂水；若呕吐不止/带血或精神萎靡请就医。"),
        Rule(listOf("腹泻"), "黄", "饮食变化、肠胃敏感、寄生虫、感染",
            "观察 24 小时，保证饮水；持续腹泻或带血请就医并携带粪便样本。"),
        Rule(listOf("咳嗽", "打喷嚏"), "黄", "上呼吸道感染、过敏、异物刺激",
            "观察 48 小时，注意鼻分泌物；伴随食欲下降或发热请就医。"),
        Rule(listOf("挠痒"), "绿", "皮肤干燥、跳蚤/螨虫、过敏",
            "检查皮肤是否有红疹/脱毛，定期驱虫；持续挠痒或掉毛加重请就医。"),
        Rule(listOf("掉毛异常"), "绿", "换毛期、营养问题、皮肤疾病",
            "观察 1-2 周，检查皮肤状态；伴随瘙痒/皮屑增多请就医。"),
        Rule(listOf("眼屎增多", "泪痕"), "绿", "轻微炎症、鼻泪管堵塞、饮食上火",
            "用宠物专用湿巾清洁，观察 3 天；眼睛红肿/流泪不止请就医。"),
        Rule(listOf("萎靡"), "黄", "疲劳、轻微不适、疾病前兆",
            "观察 24 小时，监测食欲饮水；持续萎靡请就医。"),
        Rule(listOf("抽搐"), "红", "癫痫、中毒、神经系统疾病、低血糖",
            "保持环境安静避免刺激，**尽快送医**，记录抽搐时长与表现。"),
        Rule(listOf("跛行"), "红", "外伤、骨折、关节问题、指甲过长",
            "限制运动避免加重，**尽快送医**拍片检查。"),
        Rule(listOf("呼吸困难"), "红", "呼吸道梗阻、心肺疾病、中暑",
            "保持通风降温，**立即送医**。"),
        Rule(listOf("尿血", "便血"), "红", "泌尿/消化道出血、感染、结石",
            "**立即送医**，如可能收集尿/粪便样本。"),
        Rule(listOf("呕吐不止"), "红", "异物梗阻、中毒、急性胰腺炎",
            "**立即送医**，禁食禁水。"),
    )

    /** 症状自查：返回等级、可能原因、建议。 */
    fun evaluate(kind: PetKind, symptoms: List<String>): SelfCheckResult {
        val result = SelfCheckResult(
            id = System.currentTimeMillis(),
            petId = "",
            symptoms = symptoms,
            level = "绿",
            possibleCauses = "",
            advice = "",
            date = java.time.LocalDate.now().toString(),
        )

        // 多选多个严重症状 → 直接红色
        val severeHit = symptoms.filter { it in SEVERE_SYMPTOMS }
        if (severeHit.size >= 2) {
            return result.copy(
                level = "红",
                possibleCauses = "多个严重症状同时出现，提示可能存在急症",
                advice = "不要等待观察，**尽快送医**。携带宠物既往病史与疫苗记录。",
            )
        }

        // 单严重症状 → 红色
        if (severeHit.size == 1) {
            val rule = rules.firstOrNull { r -> severeHit.any { it in r.keywords } }
            return result.copy(
                level = "红",
                possibleCauses = rule?.causes ?: "严重症状",
                advice = rule?.advice ?: "尽快送医。",
            )
        }

        // 普通症状：取匹配规则中等级最高的
        var best: Rule? = null
        symptoms.forEach { s ->
            rules.filter { s in it.keywords }.forEach { r ->
                if (best == null || levelRank(r.level) > levelRank(best!!.level)) best = r
            }
        }
        if (best == null) {
            return result.copy(
                possibleCauses = "暂未收录该症状组合的完整资料",
                advice = "观察 24 小时；症状持续或加重请及时就医。",
            )
        }
        return result.copy(
            level = best.level,
            possibleCauses = best.causes,
            advice = best.advice,
        )
    }

    private fun levelRank(level: String): Int = when (level) {
        "红" -> 3; "黄" -> 2; else -> 1
    }
}

/** 自动预警规则引擎（基于记录数据，纯函数）。 */
object HealthAlertRules {

    private const val DISCLAIMER = "以上信息仅供参考，不能替代兽医诊断，如有疑虑请及时就医"

    fun disclaimer(): String = DISCLAIMER

    /** 饮食预警：连续 2 天食欲下降或拒食。 */
    fun appetiteAlert(mealsByDate: Map<String, List<HealthMealRecord>>, petId: String): HealthAlert? {
        val badDays = mealsByDate.filterValues { list ->
            list.isNotEmpty() && list.all { it.appetite in setOf("食欲下降", "拒食") }
        }.keys
        if (badDays.size >= 2) {
            return HealthAlert(
                id = System.currentTimeMillis() + 1,
                petId = petId,
                title = "连续 ${badDays.size} 天食欲异常",
                detail = "近 ${badDays.size} 天进食意愿下降，建议观察口腔与消化道，若持续请就医。",
                priority = "黄",
                date = java.time.LocalDate.now().toString(),
                source = "auto",
            )
        }
        return null
    }

    /** 排泄预警：连续 2 天未排便 / 腹泻（布里斯托 6-7 型）→ 黄；猫排尿异常 → 红。 */
    fun poopAlert(records: List<PoopRecord>, petId: String, kind: PetKind): HealthAlert? {
        val recent = records.filter { it.date >= java.time.LocalDate.now().minusDays(2).toString() }
        // 猫排尿异常：尿血/尿少/尿频标注 → 红
        if (kind == PetKind.CAT) {
            val urineAbnormal = recent.any {
                it.type == "排尿" && (it.hasBlood || it.abnormal)
            }
            if (urineAbnormal) {
                return HealthAlert(
                    id = System.currentTimeMillis() + 2,
                    petId = petId,
                    title = "猫咪排尿异常",
                    detail = "检测到排尿异常（尿血/尿少/尿频）。猫咪易患泌尿系统疾病，**建议立即就医**。",
                    priority = "红",
                    date = java.time.LocalDate.now().toString(),
                    source = "auto",
                )
            }
        }
        // 腹泻：布里斯托 6-7 型
        val diarrhea = recent.any { it.type == "排便" && it.bristol in 6..7 }
        if (diarrhea) {
            return HealthAlert(
                id = System.currentTimeMillis() + 3,
                petId = petId,
                title = "腹泻（布里斯托 6-7 型）",
                detail = "粪便呈稀软/水样，注意补水，持续或带血请就医。",
                priority = "黄",
                date = java.time.LocalDate.now().toString(),
                source = "auto",
            )
        }
        return null
    }

    /** 体重预警：一周内波动 >5%。 */
    fun weightAlert(records: List<HealthWeightRecord>, petId: String): HealthAlert? {
        if (records.size < 2) return null
        val sorted = records.sortedBy { it.date }
        val recent = sorted.filter { it.date >= java.time.LocalDate.now().minusDays(7).toString() }
        if (recent.size < 2) return null
        val first = recent.first().weightKg
        val last = recent.last().weightKg
        val change = (last - first) / first
        if (kotlin.math.abs(change) > 0.05) {
            return HealthAlert(
                id = System.currentTimeMillis() + 4,
                petId = petId,
                title = if (change > 0) "一周内体重上升 ${"%.1f".format(change * 100)}%" else "一周内体重下降 ${"%.1f".format(-change * 100)}%",
                detail = if (change > 0) "体重波动超过 5%，注意控制饮食与运动。" else "体重快速下降，排查疾病与进食情况，建议就医。",
                priority = "黄",
                date = java.time.LocalDate.now().toString(),
                source = "auto",
            )
        }
        return null
    }

    /** 饮水预警：较 7 日均值骤增/骤减 ±50%。 */
    fun waterAlert(records: List<WaterRecord>, petId: String): HealthAlert? {
        if (records.size < 4) return null
        val sorted = records.sortedBy { it.date }
        val recent = sorted.takeLast(3).sumOf { it.ml } / 3.0
        val older = sorted.dropLast(3).takeLast(7).sumOf { it.ml } / 7.0
        if (older <= 0) return null
        val ratio = recent / older
        if (ratio > 1.5 || ratio < 0.5) {
            return HealthAlert(
                id = System.currentTimeMillis() + 5,
                petId = petId,
                title = if (ratio > 1.5) "饮水量骤增" else "饮水量骤减",
                detail = "近期饮水量较往日变化 ±50%，猫咪需警惕肾脏/糖尿病，建议观察并咨询兽医。",
                priority = "黄",
                date = java.time.LocalDate.now().toString(),
                source = "auto",
            )
        }
        return null
    }

    /** 逾期提醒：疫苗/驱虫。 */
    fun overdueAlerts(
        vaccines: List<VaccineRecord>,
        deworms: List<DewormRecord>,
        petId: String,
    ): List<HealthAlert> {
        val today = java.time.LocalDate.now().toString()
        val result = mutableListOf<HealthAlert>()
        vaccines.filter { it.nextDate.isNotBlank() && it.nextDate < today }.forEachIndexed { i, v ->
            result += HealthAlert(
                id = System.currentTimeMillis() + 10 + i,
                petId = petId,
                title = "疫苗逾期：${v.vaccineName}",
                detail = "原定 ${v.nextDate} 接种，已逾期，请尽快补种。",
                priority = "黄",
                date = today,
                source = "auto",
            )
        }
        deworms.filter { it.nextDate.isNotBlank() && it.nextDate < today }.forEachIndexed { i, d ->
            result += HealthAlert(
                id = System.currentTimeMillis() + 20 + i,
                petId = petId,
                title = "驱虫逾期：${d.type}驱虫",
                detail = "原定 ${d.nextDate} 驱虫，已逾期，请尽快补驱。",
                priority = "黄",
                date = today,
                source = "auto",
            )
        }
        return result
    }

    /** 健康评分：记录完整度 + 异常减分。 */
    fun healthScore(
        hasMeal: Boolean, hasWater: Boolean, hasPoop: Boolean, hasWeight: Boolean,
        abnormalCount: Int, redAlert: Boolean,
    ): Int {
        var score = 60
        if (hasMeal) score += 10
        if (hasWater) score += 10
        if (hasPoop) score += 10
        if (hasWeight) score += 5
        score -= abnormalCount * 5
        if (redAlert) score -= 20
        return score.coerceIn(0, 100)
    }
}
