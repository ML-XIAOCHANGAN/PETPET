package cn.petpet.app.ui.components

import androidx.annotation.DrawableRes
import cn.petpet.app.R

/**
 * 18 种宠物表情包映射（2026-08 用户素材全量更新）。
 *
 * 每个品种的表情文件统一命名 `expr_<breedId>_<emotion>.png`：
 * wink 眨眼 / happy 高兴 / joy 开心 / surprised 惊讶 / shocked 震惊 /
 * panic 慌张 / sad 伤心 / sleeping 睡觉 / thinking 思考 / speechless 无语。
 * 点击宠物随机显示表情列表中的一个，思考状态显示 thinking，
 * 对话回复后按情绪名（expression）查询对应表情。
 */
object PetExpressionLibrary {

    /** 品种 id → 情绪名 → 表情资源。 */
    private val allExpressions: Map<String, Map<String, Int>> = mapOf(
        "black_cat" to mapOf(
            "wink" to R.drawable.expr_black_cat_wink,
            "happy" to R.drawable.expr_black_cat_happy,
            "joy" to R.drawable.expr_black_cat_joy,
            "panic" to R.drawable.expr_black_cat_panic,
            "sad" to R.drawable.expr_black_cat_sad,
            "sleeping" to R.drawable.expr_black_cat_sleeping,
            "speechless" to R.drawable.expr_black_cat_speechless,
            "thinking" to R.drawable.expr_black_cat_thinking,
        ),
        "gray_cat" to mapOf(
            "wink" to R.drawable.expr_gray_cat_wink,
            "happy" to R.drawable.expr_gray_cat_happy,
            "joy" to R.drawable.expr_gray_cat_joy,
            "surprised" to R.drawable.expr_gray_cat_surprised,
            "sleeping" to R.drawable.expr_gray_cat_sleeping,
            "thinking" to R.drawable.expr_gray_cat_thinking,
        ),
        "orange_cat" to mapOf(
            "wink" to R.drawable.expr_orange_cat_wink,
            "happy" to R.drawable.expr_orange_cat_happy,
            "joy" to R.drawable.expr_orange_cat_joy,
            "surprised" to R.drawable.expr_orange_cat_surprised,
            "sleeping" to R.drawable.expr_orange_cat_sleeping,
            "thinking" to R.drawable.expr_orange_cat_thinking,
        ),
        "calico" to mapOf(
            "wink" to R.drawable.expr_calico_wink,
            "happy" to R.drawable.expr_calico_happy,
            "shocked" to R.drawable.expr_calico_shocked,
            "sleeping" to R.drawable.expr_calico_sleeping,
            "thinking" to R.drawable.expr_calico_thinking,
        ),
        "ragdoll" to mapOf(
            "wink" to R.drawable.expr_ragdoll_wink,
            "happy" to R.drawable.expr_ragdoll_happy,
            "joy" to R.drawable.expr_ragdoll_joy,
            "surprised" to R.drawable.expr_ragdoll_surprised,
            "sleeping" to R.drawable.expr_ragdoll_sleeping,
            "thinking" to R.drawable.expr_ragdoll_thinking,
        ),
        "white_british" to mapOf(
            "wink" to R.drawable.expr_white_british_wink,
            "happy" to R.drawable.expr_white_british_happy,
            "joy" to R.drawable.expr_white_british_joy,
            "shocked" to R.drawable.expr_white_british_shocked,
            "sleeping" to R.drawable.expr_white_british_sleeping,
            "thinking" to R.drawable.expr_white_british_thinking,
        ),
        "golden_shaded" to mapOf(
            "wink" to R.drawable.expr_golden_shaded_wink,
            "joy" to R.drawable.expr_golden_shaded_joy,
            "surprised" to R.drawable.expr_golden_shaded_surprised,
            "sleeping" to R.drawable.expr_golden_shaded_sleeping,
            "thinking" to R.drawable.expr_golden_shaded_thinking,
        ),
        "maine_coon" to mapOf(
            "wink" to R.drawable.expr_maine_coon_wink,
            "happy" to R.drawable.expr_maine_coon_happy,
            "joy" to R.drawable.expr_maine_coon_joy,
            "surprised" to R.drawable.expr_maine_coon_surprised,
            "sleeping" to R.drawable.expr_maine_coon_sleeping,
            "thinking" to R.drawable.expr_maine_coon_thinking,
        ),
        "scottish_fold" to mapOf(
            "wink" to R.drawable.expr_scottish_fold_wink,
            "happy" to R.drawable.expr_scottish_fold_happy,
            "joy" to R.drawable.expr_scottish_fold_joy,
            "shocked" to R.drawable.expr_scottish_fold_shocked,
            "sleeping" to R.drawable.expr_scottish_fold_sleeping,
            "thinking" to R.drawable.expr_scottish_fold_thinking,
        ),
        "siamese" to mapOf(
            "wink" to R.drawable.expr_siamese_wink,
            "happy" to R.drawable.expr_siamese_happy,
            "joy" to R.drawable.expr_siamese_joy,
            "surprised" to R.drawable.expr_siamese_surprised,
            "sleeping" to R.drawable.expr_siamese_sleeping,
            "thinking" to R.drawable.expr_siamese_thinking,
        ),
        // ── 狗狗 ──
        "shiba" to mapOf(
            "wink" to R.drawable.expr_shiba_wink,
            "happy" to R.drawable.expr_shiba_happy,
            "joy" to R.drawable.expr_shiba_joy,
            "shocked" to R.drawable.expr_shiba_shocked,
            "sleeping" to R.drawable.expr_shiba_sleeping,
            "thinking" to R.drawable.expr_shiba_thinking,
        ),
        "golden_retriever" to mapOf(
            "wink" to R.drawable.expr_golden_retriever_wink,
            "happy" to R.drawable.expr_golden_retriever_happy,
            "joy" to R.drawable.expr_golden_retriever_joy,
            "surprised" to R.drawable.expr_golden_retriever_surprised,
            "sleeping" to R.drawable.expr_golden_retriever_sleeping,
            "thinking" to R.drawable.expr_golden_retriever_thinking,
        ),
        "samoyed" to mapOf(
            "wink" to R.drawable.expr_samoyed_wink,
            "happy" to R.drawable.expr_samoyed_happy,
            "joy" to R.drawable.expr_samoyed_joy,
            "surprised" to R.drawable.expr_samoyed_surprised,
            "sleeping" to R.drawable.expr_samoyed_sleeping,
            "thinking" to R.drawable.expr_samoyed_thinking,
        ),
        "bichon" to mapOf(
            "wink" to R.drawable.expr_bichon_wink,
            "happy" to R.drawable.expr_bichon_happy,
            "joy" to R.drawable.expr_bichon_joy,
            "surprised" to R.drawable.expr_bichon_surprised,
            "sleeping" to R.drawable.expr_bichon_sleeping,
            "thinking" to R.drawable.expr_bichon_thinking,
        ),
        "pomeranian" to mapOf(
            "wink" to R.drawable.expr_pomeranian_wink,
            "happy" to R.drawable.expr_pomeranian_happy,
            "joy" to R.drawable.expr_pomeranian_joy,
            "surprised" to R.drawable.expr_pomeranian_surprised,
            "sleeping" to R.drawable.expr_pomeranian_sleeping,
            "thinking" to R.drawable.expr_pomeranian_thinking,
        ),
        "border_collie" to mapOf(
            "wink" to R.drawable.expr_border_collie_wink,
            "happy" to R.drawable.expr_border_collie_happy,
            "joy" to R.drawable.expr_border_collie_joy,
            "surprised" to R.drawable.expr_border_collie_surprised,
            "sleeping" to R.drawable.expr_border_collie_sleeping,
            "thinking" to R.drawable.expr_border_collie_thinking,
        ),
        "german_shepherd" to mapOf(
            "wink" to R.drawable.expr_german_shepherd_wink,
            "happy" to R.drawable.expr_german_shepherd_happy,
            "joy" to R.drawable.expr_german_shepherd_joy,
            "shocked" to R.drawable.expr_german_shepherd_shocked,
            "sleeping" to R.drawable.expr_german_shepherd_sleeping,
            "thinking" to R.drawable.expr_german_shepherd_thinking,
        ),
        "berner" to mapOf(
            "wink" to R.drawable.expr_berner_wink,
            "happy" to R.drawable.expr_berner_happy,
            "joy" to R.drawable.expr_berner_joy,
            "surprised" to R.drawable.expr_berner_surprised,
            "sleeping" to R.drawable.expr_berner_sleeping,
            "thinking" to R.drawable.expr_berner_thinking,
        ),
    )

    /** 点击随机表情列表（无表情包的品种回退空列表，由调用方用主头像）。 */
    fun clickExpressionsFor(breedId: String): List<Int> =
        allExpressions[breedId]?.values?.filterNot { it == thinkingFor(breedId) }?.toList() ?: emptyList()

    /** 思考表情（无则返回 null，调用方保持主头像）。 */
    fun thinkingFor(breedId: String): Int? = allExpressions[breedId]?.get("thinking")

    /** 按情绪名查询表情资源（happy/joy/surprised/shocked/sad/panic/speechless/wink）。 */
    @DrawableRes
    fun expressionRes(breedId: String, emotion: String): Int? =
        allExpressions[breedId]?.get(emotion)
}
