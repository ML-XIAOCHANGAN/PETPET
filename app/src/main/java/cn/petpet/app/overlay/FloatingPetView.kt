package cn.petpet.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import cn.petpet.app.R

enum class FloatingMenuDirection {
    RIGHT,
    LEFT,
    DOWN,
    UP,
}

/**
 * 使用用户领养宠物头像显示的悬浮宠物。
 *
 * 窗口始终保持 PET_SIZE_DP 大小，点击/拖动都不会改变自身位置；
 * 环形菜单由 Service 以独立 overlay 窗口在宠物旁弹出（宠物绝对静止）。
 */
class FloatingPetView(
    context: Context,
    private val petAvatarRes: Int,
    private val onPetClicked: () -> Unit,
) : FrameLayout(context) {
    val petHandle = ImageView(context)
    private var mainAvatarRes: Int = petAvatarRes

    var isMenuExpanded: Boolean = false
        private set

    /** 聊天状态：true 时第三个菜单按钮显示 ✕（退出对话框），false 显示 💬（打开聊天）。 */
    var isChatMode: Boolean = false
        private set

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren = false
        clipToPadding = false

        petHandle.apply {
            contentDescription = "悬浮宠物，点击展开环形菜单"
            isClickable = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(petAvatarRes)
            setOnClickListener { this@FloatingPetView.performClick() }
            layoutParams = LayoutParams(dp(PET_SIZE_DP), dp(PET_SIZE_DP))
        }
        addView(petHandle)
    }

    override fun performClick(): Boolean {
        super.performClick()
        isMenuExpanded = !isMenuExpanded
        onPetClicked()
        return true
    }

    /** 切换宠物图片（表情）：传 null 恢复主头像。 */
    fun setPetImage(resId: Int?) {
        if (resId != null && resId != 0) {
            petHandle.setImageResource(resId)
        } else {
            petHandle.setImageResource(mainAvatarRes)
        }
    }

    /** 设置思考状态（true=thinking 表情，false=恢复主头像）。 */
    fun setThinking(thinking: Boolean, thinkingRes: Int?) {
        if (thinking) {
            if (thinkingRes != null && thinkingRes != 0) petHandle.setImageResource(thinkingRes)
        } else {
            petHandle.setImageResource(mainAvatarRes)
        }
    }

    /** 进入/退出聊天状态：true 时第三个菜单按钮变为 ✕，false 恢复 💬。 */
    fun setChatMode(enabled: Boolean) {
        isChatMode = enabled
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PET_SIZE_DP = 82
        const val MENU_BUTTON_SIZE_DP = 48
        const val MENU_SPREAD_DP = 230
    }
}
