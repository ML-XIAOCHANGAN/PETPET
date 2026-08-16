package cn.petpet.app.data.fitcut

/** 训练动作。 */
data class Exercise(
    val name: String,
    val category: String,
    val targetMuscle: String,
    val description: String,
    val setsReps: String,
)

/** 内置训练库：有氧 + 力量五肌群。 */
object FitCutExerciseLibrary {

    val exercises: List<Exercise> = listOf(
        // 有氧
        Exercise("跑步", "有氧", "全身", "保持匀速呼吸，步频 170-180/分，从 20 分钟起步循序渐进。", "20-40 分钟"),
        Exercise("跳绳", "有氧", "全身/小腿", "前脚掌落地，膝盖微屈缓冲，每分钟 100-120 下。", "15-30 分钟（分 3 组）"),
        Exercise("游泳", "有氧", "全身", "自由泳/蛙泳交替，注意呼吸节奏，游完及时补充水分。", "30-45 分钟"),
        Exercise("骑行", "有氧", "腿部", "坐姿稳定，踏频 80-90，保持中等阻力不憋气。", "30-60 分钟"),
        // 胸
        Exercise("俯卧撑", "力量", "胸/肱三头", "双手略宽于肩，身体成直线，下降时胸部贴近地面。", "3 组 × 8-12 次"),
        Exercise("哑铃卧推", "力量", "胸", "肩胛骨后收下沉，杠铃下降至胸部轻触后推起。", "4 组 × 8-12 次"),
        // 背
        Exercise("引体向上", "力量", "背/肱二", "肩胛先下沉再发力上拉，下巴过杠，下落控制节奏。", "3 组 × 5-10 次"),
        Exercise("哑铃划船", "力量", "背", "背部挺直，手肘贴近身体向后拉，感受背部收缩。", "4 组 × 10-12 次"),
        // 腿
        Exercise("深蹲", "力量", "腿/臀", "双脚与肩同宽，下蹲时膝盖与脚尖同向，大腿平行地面。", "4 组 × 10-15 次"),
        Exercise("箭步蹲", "力量", "腿/臀", "前后跨步下蹲，前膝不过脚尖，后膝接近地面。", "3 组 × 10 次/腿"),
        // 肩
        Exercise("哑铃推举", "力量", "肩", "坐姿背靠椅背，哑铃从肩部推至头顶，不要耸肩。", "4 组 × 8-12 次"),
        Exercise("侧平举", "力量", "肩", "肘微屈，哑铃向两侧抬起至与肩平，慢放还原。", "3 组 × 12-15 次"),
        // 腹
        Exercise("卷腹", "力量", "腹", "下背贴地，用腹部力量卷起上背，不要拉脖子。", "4 组 × 15-20 次"),
        Exercise("平板支撑", "力量", "核心", "肘撑地，身体成直线，收紧核心不塌腰。", "3 组 × 30-60 秒"),
    )

    fun byCategory(category: String): List<Exercise> = exercises.filter { it.category == category }

    /** 有氧 / 力量分类列表。 */
    val categories: List<String> = listOf("有氧", "力量")
}
