package cn.petpet.app.ui.components

import android.content.Context
import android.media.MediaPlayer
import cn.petpet.app.R

/**
 * 宠物点击音效播放器。
 *
 * - 普通叫声：按品种随机抽取（猫 → cat_meow_1/2/3，狗 → dog_bark_1/2/3）
 * - 哈气：猫连续点击时播放 cat_hiss + 对应哈气表情（由调用方切图）
 * - 重复点击时释放上一个播放器，避免声音重叠
 */
object PetSoundPlayer {

    private var player: MediaPlayer? = null

    /** 猫叫声资源列表。 */
    private val catMeowRes = intArrayOf(
        R.raw.cat_meow_1,
        R.raw.cat_meow_2,
        R.raw.cat_meow_3,
    )

    /** 狗叫声资源列表。 */
    private val dogBarkRes = intArrayOf(
        R.raw.dog_bark_1,
        R.raw.dog_bark_2,
        R.raw.dog_bark_3,
    )

    /**
     * 播放随机叫声（按品种）。
     * @param isDog true=狗叫，false=猫叫
     */
    fun playRandom(context: Context, isDog: Boolean) {
        val pool = if (isDog) dogBarkRes else catMeowRes
        play(context, pool.random())
    }

    /** 播放猫哈气音频（连续点击触发）。 */
    fun playHiss(context: Context) {
        play(context, R.raw.cat_hiss)
    }

    private fun play(context: Context, resId: Int) {
        stop()
        player = runCatching {
            MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        }.getOrNull()
    }

    /** 停止并释放当前播放器（避免声音重叠 / 泄漏）。 */
    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }
}
