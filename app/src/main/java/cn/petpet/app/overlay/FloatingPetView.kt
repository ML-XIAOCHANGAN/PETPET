package cn.petpet.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
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
 * 三个动作围绕宠物排成半环，并从小猫中心依次滑出。菜单朝向由服务根据
 * 宠物距离屏幕四条边的远近决定，始终优先向屏幕内部展开。
 */
class FloatingPetView(
    context: Context,
    private val petAvatarRes: Int,
    private val onMenuVisibilityChanged: (Boolean) -> Unit,
    private val onInteract: () -> Unit,
    private val onOpenApp: () -> Unit,
    private val onChat: () -> Unit,
) : FrameLayout(context) {
    val petHandle = ImageView(context)
    private val handler = Handler(Looper.getMainLooper())
    private val menuButtons = mutableListOf<TextView>()

    var isMenuExpanded: Boolean = false
        private set

    var expansionDirection: FloatingMenuDirection = FloatingMenuDirection.RIGHT
        private set

    var handleOffsetX: Int = 0
        private set

    var handleOffsetY: Int = 0
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

        menuButtons += emojiButton("🐾", "和小宠物互动") {
            onInteract()
            closeMenu()
        }
        menuButtons += emojiButton("⌂", "返回应用") {
            closeMenu()
            onOpenApp()
        }
        menuButtons += emojiButton("💬", "打开应用聊天框") {
            closeMenu()
            onChat()
        }
        menuButtons.forEach { button ->
            button.visibility = View.GONE
            addView(button)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (isMenuExpanded) closeMenu() else openMenu()
        return true
    }

    fun setExpansionDirection(direction: FloatingMenuDirection) {
        expansionDirection = direction
        val expandedSize = dp(EXPANDED_SIZE_DP)
        val petSize = dp(PET_SIZE_DP)
        val centered = (expandedSize - petSize) / 2

        when (direction) {
            FloatingMenuDirection.RIGHT -> {
                handleOffsetX = 0
                handleOffsetY = centered
            }
            FloatingMenuDirection.LEFT -> {
                handleOffsetX = expandedSize - petSize
                handleOffsetY = centered
            }
            FloatingMenuDirection.DOWN -> {
                handleOffsetX = centered
                handleOffsetY = 0
            }
            FloatingMenuDirection.UP -> {
                handleOffsetX = centered
                handleOffsetY = expandedSize - petSize
            }
        }

        petHandle.layoutParams = LayoutParams(petSize, petSize).apply {
            leftMargin = handleOffsetX
            topMargin = handleOffsetY
        }
        positionMenuButtons(direction)
        requestLayout()
    }

    fun resetCollapsedLayout() {
        handleOffsetX = 0
        handleOffsetY = 0
        petHandle.layoutParams = LayoutParams(dp(PET_SIZE_DP), dp(PET_SIZE_DP))
        requestLayout()
    }

    fun closeMenu(immediate: Boolean = false) {
        if (!isMenuExpanded) return
        isMenuExpanded = false
        petHandle.contentDescription = "悬浮宠物，点击展开环形菜单"

        // 收起时不做"弹回中心"动画（用户反馈回弹幅度大），直接隐藏选项并收缩窗口；
        // 展开时的选项弹动动画（playMenuReveal）保留。
        menuButtons.forEach { it.visibility = View.GONE }
        onMenuVisibilityChanged(false)
        if (!immediate) {
            handler.removeCallbacksAndMessages(null)
        }
    }

    private fun openMenu() {
        if (isMenuExpanded) return
        isMenuExpanded = true
        menuButtons.forEach { it.visibility = View.VISIBLE }
        petHandle.contentDescription = "悬浮宠物，点击收起环形菜单"
        onMenuVisibilityChanged(true)
        post { playMenuReveal() }
    }

    private fun playMenuReveal() {
        val petCenterX = handleOffsetX + dp(PET_SIZE_DP) / 2f
        val petCenterY = handleOffsetY + dp(PET_SIZE_DP) / 2f
        menuButtons.forEachIndexed { index, button ->
            val buttonCenterX = button.left + button.width / 2f
            val buttonCenterY = button.top + button.height / 2f
            button.animate().cancel()
            button.translationX = petCenterX - buttonCenterX
            button.translationY = petCenterY - buttonCenterY
            button.alpha = 0f
            button.scaleX = 0.72f
            button.scaleY = 0.72f
            button.animate()
                .translationX(0f)
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260L)
                .setStartDelay(index * 52L)
                .start()
        }
    }

    private fun positionMenuButtons(direction: FloatingMenuDirection) {
        val expandedSize = dp(EXPANDED_SIZE_DP)
        val center = expandedSize / 2
        val near = dp(72)
        val far = dp(98)
        val spread = dp(49)
        val petCenterX = handleOffsetX + dp(PET_SIZE_DP) / 2
        val petCenterY = handleOffsetY + dp(PET_SIZE_DP) / 2

        val centers = when (direction) {
            FloatingMenuDirection.RIGHT -> listOf(
                petCenterX + near to center - spread,
                petCenterX + far to center,
                petCenterX + near to center + spread,
            )
            FloatingMenuDirection.LEFT -> listOf(
                petCenterX - near to center - spread,
                petCenterX - far to center,
                petCenterX - near to center + spread,
            )
            FloatingMenuDirection.DOWN -> listOf(
                center - spread to petCenterY + near,
                center to petCenterY + far,
                center + spread to petCenterY + near,
            )
            FloatingMenuDirection.UP -> listOf(
                center - spread to petCenterY - near,
                center to petCenterY - far,
                center + spread to petCenterY - near,
            )
        }

        val buttonSize = dp(MENU_BUTTON_SIZE_DP)
        menuButtons.zip(centers).forEach { (button, point) ->
            button.layoutParams = LayoutParams(buttonSize, buttonSize).apply {
                leftMargin = point.first - buttonSize / 2
                topMargin = point.second - buttonSize / 2
            }
        }
    }

    private fun emojiButton(
        emoji: String,
        description: String,
        action: () -> Unit,
    ): TextView {
        return TextView(context).apply {
            text = emoji
            textSize = 23f
            gravity = Gravity.CENTER
            contentDescription = description
            isClickable = true
            isFocusable = true
            elevation = dp(6).toFloat()
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(242, 255, 255, 255))
                setStroke(dp(1), Color.argb(28, 20, 24, 21))
            }
            setOnClickListener { action() }
            layoutParams = LayoutParams(dp(MENU_BUTTON_SIZE_DP), dp(MENU_BUTTON_SIZE_DP))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PET_SIZE_DP = 82
        const val MENU_BUTTON_SIZE_DP = 48
        const val EXPANDED_SIZE_DP = 230
    }
}
