package cn.petpet.app.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import cn.petpet.app.MainActivity
import cn.petpet.app.R
import cn.petpet.app.data.ChatGatewayClient
import cn.petpet.app.data.PetPetPreferencesRepository
import cn.petpet.app.data.PetPetRepository
import cn.petpet.app.model.ChatProvider
import cn.petpet.app.model.PetBreeds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

class FloatingPetService : Service() {
    private var windowManager: WindowManager? = null
    private var notificationChannelReady = false
    private var floatingView: FloatingPetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val chatScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        FloatingPetController.setRunning(false)
        windowManager = runCatching {
            getSystemService(WindowManager::class.java)
        }.onFailure {
            Log.e(TAG, "读取系统窗口服务失败", it)
        }.getOrNull()
        notificationChannelReady = runCatching {
            createNotificationChannel()
        }.onFailure {
            Log.e(TAG, "创建悬浮小猫通知渠道失败", it)
        }.isSuccess
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            FloatingPetController.setAutoStartEnabled(this, false)
            FloatingPetController.setRunning(false)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (
            !notificationChannelReady ||
            windowManager == null ||
            !FloatingPetController.hasRequiredPermissions(this)
        ) {
            FloatingPetController.setRunning(false)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val foregroundStarted = runCatching {
            startForeground(NOTIFICATION_ID, buildNotification())
        }.onFailure {
            Log.e(TAG, "悬浮小猫进入前台服务失败", it)
        }.isSuccess
        val floatingPetShown = foregroundStarted && runCatching {
            showFloatingPet()
        }.onFailure {
            Log.e(TAG, "创建悬浮小猫视图失败", it)
        }.getOrDefault(false)
        if (!floatingPetShown) {
            FloatingPetController.setRunning(false)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        FloatingPetController.setRunning(true)
        if (intent?.getBooleanExtra(EXTRA_ENABLE_AUTO_START, false) == true) {
            FloatingPetController.setAutoStartEnabled(this, true)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        floatingView?.let { view ->
            runCatching { windowManager?.removeView(view) }
                .onFailure { Log.w(TAG, "移除悬浮小猫视图失败", it) }
        }
        floatingView = null
        layoutParams = null
        chatDialogView?.let { dialog ->
            runCatching { windowManager?.removeView(dialog) }
                .onFailure { Log.w(TAG, "移除悬浮聊天对话框失败", it) }
        }
        chatDialogView = null
        chatDialogParams = null
        bubbleView?.let { bubble ->
            runCatching { windowManager?.removeView(bubble) }
                .onFailure { Log.w(TAG, "移除回复气泡失败", it) }
        }
        bubbleView = null
        bubbleParams = null
        chatScope.cancel()
        FloatingPetController.setRunning(false)
        super.onDestroy()
    }

    private fun showFloatingPet(): Boolean {
        if (floatingView != null) return true
        val windowManager = windowManager ?: return false

        val collapsedSize = (FloatingPetView.PET_SIZE_DP * resources.displayMetrics.density).toInt()
        val expandedSize = (FloatingPetView.EXPANDED_SIZE_DP * resources.displayMetrics.density).toInt()
        val savedPosition = getSharedPreferences(POSITION_PREFERENCES, MODE_PRIVATE)
        val params = WindowManager.LayoutParams(
            collapsedSize,
            collapsedSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedPosition.getInt(KEY_X, resources.displayMetrics.widthPixels - collapsedSize)
            y = savedPosition.getInt(KEY_Y, resources.displayMetrics.heightPixels / 3)
        }

        lateinit var view: FloatingPetView
        // 悬浮宠物图标 = 用户领养的宠物主头像
        val petBreedId = runCatching { PetPetRepository(this).snapshot().pet?.breedId }
            .getOrNull()
        val petAvatarRes = PetBreeds.byId(petBreedId ?: "")?.avatarRes ?: R.drawable.cat_main
        view = FloatingPetView(
            context = this,
            petAvatarRes = petAvatarRes,
            onMenuVisibilityChanged = { expanded ->
                if (expanded) {
                    val direction = chooseMenuDirection(
                        handleX = params.x,
                        handleY = params.y,
                        handleSize = collapsedSize,
                    )
                    view.setExpansionDirection(direction)
                    params.x -= view.handleOffsetX
                    params.y -= view.handleOffsetY
                    params.width = expandedSize
                    params.height = expandedSize
                } else {
                    params.x += view.handleOffsetX
                    params.y += view.handleOffsetY
                    params.width = collapsedSize
                    params.height = collapsedSize
                    view.resetCollapsedLayout()
                }
                runCatching { windowManager.updateViewLayout(view, params) }
            },
            onInteract = {
                runCatching { PetPetRepository(this).interact() }
                    .onFailure { Log.e(TAG, "记录悬浮小猫互动失败", it) }
            },
            onOpenApp = { openMainScreen(openChat = false) },
            // 💬 点击：应用在前台 → 跳转 MyPet 聊天；其他情况 → 居中偏下的独立聊天对话框
            onChatPanelRequested = {
                if (isAppInForeground()) {
                    openMainScreen(openChat = true)
                } else {
                    showChatDialog(view, petBreedId ?: "")
                }
            },
            onChat = { message -> startFloatingChat(view, petBreedId ?: "", message) },
        )
        attachDragBehavior(view, params, windowManager)
        return runCatching {
            windowManager.addView(view, params)
            floatingView = view
            layoutParams = params
            true
        }.onFailure {
            Log.e(TAG, "挂载悬浮小猫视图失败", it)
        }.getOrDefault(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragBehavior(
        view: FloatingPetView,
        params: WindowManager.LayoutParams,
        windowManager: WindowManager,
    ) {
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var moved = false
        // 按下时若菜单已展开会先收起（便于拖动）；此时抬起不应再触发 performClick 重新展开
        var suppressNextClick = false

        // 监听器负责拖动；未发生拖动时仍调用 performClick，保留无障碍点击语义。
        view.petHandle.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (view.isMenuExpanded) {
                        view.closeMenu(immediate = true)
                        suppressNextClick = true
                    } else {
                        suppressNextClick = false
                    }
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    val screenWidth = resources.displayMetrics.widthPixels
                    val screenHeight = resources.displayMetrics.heightPixels
                    params.x = (startX + dx).coerceIn(0, (screenWidth - view.width).coerceAtLeast(0))
                    params.y = (startY + dy).coerceIn(0, (screenHeight - view.height).coerceAtLeast(0))
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val screenHeight = resources.displayMetrics.heightPixels
                        val safeMargin = (6 * resources.displayMetrics.density).toInt()
                        val maxX = (screenWidth - view.width - safeMargin).coerceAtLeast(safeMargin)
                        val maxY = (screenHeight - view.height - safeMargin).coerceAtLeast(safeMargin)
                        val distanceLeft = params.x
                        val distanceRight = screenWidth - (params.x + view.width)
                        val distanceTop = params.y
                        val distanceBottom = screenHeight - (params.y + view.height)
                        when (minOf(distanceLeft, distanceRight, distanceTop, distanceBottom)) {
                            distanceLeft -> params.x = safeMargin
                            distanceRight -> params.x = maxX
                            distanceTop -> params.y = safeMargin
                            else -> params.y = maxY
                        }
                        runCatching { windowManager.updateViewLayout(view, params) }
                        savePosition(params.x, params.y)
                    } else if (!suppressNextClick) {
                        // 普通点击：菜单开→关、菜单关→开
                        touchedView.performClick()
                    }
                    suppressNextClick = false
                    true
                }
                else -> false
            }
        }
    }

    private fun chooseMenuDirection(
        handleX: Int,
        handleY: Int,
        handleSize: Int,
    ): FloatingMenuDirection {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val distanceLeft = handleX
        val distanceRight = screenWidth - (handleX + handleSize)
        val distanceTop = handleY
        val distanceBottom = screenHeight - (handleY + handleSize)

        return when (minOf(distanceLeft, distanceRight, distanceTop, distanceBottom)) {
            distanceLeft -> FloatingMenuDirection.RIGHT
            distanceRight -> FloatingMenuDirection.LEFT
            distanceTop -> FloatingMenuDirection.DOWN
            else -> FloatingMenuDirection.UP
        }
    }

    private fun openMainScreen(openChat: Boolean) {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    )
                    putExtra(MainActivity.EXTRA_OPEN_CHAT, openChat)
                },
            )
        }.onFailure { Log.e(TAG, "从悬浮小猫打开主界面失败", it) }
    }

    private fun savePosition(x: Int, y: Int) {
        runCatching {
            getSharedPreferences(POSITION_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putInt(KEY_X, x)
                .putInt(KEY_Y, y)
                .apply()
        }.onFailure { Log.e(TAG, "保存悬浮小猫位置失败", it) }
    }

    private fun createNotificationChannel() {
        val manager = checkNotNull(getSystemService(NotificationManager::class.java)) {
            "系统未提供通知管理服务"
        }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.floating_pet_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "保证用户主动开启的悬浮小猫能够正常显示"
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingPetService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_petpet)
            .setContentTitle(getString(R.string.floating_pet_running))
            .setContentText("点击悬浮小猫可展开互动、返回和聊天菜单")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, getString(R.string.floating_pet_stop), stopIntent)
            .build()
    }

    /**
     * 悬浮窗内直接聊天：思考表情 → 直连模型 → 情绪表情（2.8s 后恢复主头像）。
     * 与主界面对话同源（ChatGatewayClient.directChat + 情绪分析），不跳转应用。
     */
    private fun startFloatingChat(view: FloatingPetView, breedId: String, message: String) {
        val isDog = PetBreeds.byId(breedId)?.type == cn.petpet.app.model.PetType.DOG
        val petName = runCatching { PetPetRepository(this).snapshot().pet?.name }.getOrNull() ?: ""
        val preferencesRepo = PetPetPreferencesRepository(this)

        // 思考表情
        val thinkingRes = cn.petpet.app.ui.components.PetExpressionLibrary.thinkingFor(breedId)
        view.setThinking(true, thinkingRes)

        // 读取模型配置（同步读取 DataStore）
        val providerId = runBlocking { preferencesRepo.chatProviderFlow.first() }
        val apiKey = runBlocking { preferencesRepo.chatApiKeyFlow.first() }
        val provider = ChatProvider.byId(providerId)

        chatScope.launch {
            val client = ChatGatewayClient()
            val response = if (apiKey.isNotBlank()) {
                client.directChat(
                    provider = provider,
                    apiKey = apiKey,
                    message = message,
                    history = emptyList(),
                    petName = petName.ifEmpty { if (isDog) "小狗" else "小猫" },
                    isDog = isDog,
                )
            } else {
                null // 无 Key：走网关失败路径 → 本地规则
            }
            val reply = if (response != null && !response.degraded && response.reply.isNotBlank()) {
                response.reply
            } else {
                localReply(message, isDog)
            }
            // 回复气泡（独立小窗口，方向跟随悬浮窗位置）+ 情绪表情
            showReplyBubble(reply, bubbleDirectionForWindow())
            // 同步显示在聊天对话框回复区（若对话框还开着）
            chatReply?.let { it.text = reply }
            val emotion = analyzeEmotion(reply)
            if (emotion != null) {
                val res = cn.petpet.app.ui.components.PetExpressionLibrary.expressionRes(breedId, emotion)
                view.setPetImage(res)
                delay(2800)
                view.setPetImage(null)
            } else {
                view.setThinking(false, null)
            }
        }
    }

    /** 根据悬浮窗窗口位置决定气泡弹出方向（与菜单方向逻辑一致）。 */
    private fun bubbleDirectionForWindow(): FloatingMenuDirection {
        val density = resources.displayMetrics.density
        val collapsed = (FloatingPetView.PET_SIZE_DP * density).toInt()
        val windowLeft = layoutParams?.x ?: 0
        val screenWidth = resources.displayMetrics.widthPixels
        // 窗口在屏幕左半 → 气泡从右侧冒出；右半 → 气泡从左侧冒出
        return if (windowLeft + collapsed / 2 < screenWidth / 2) {
            FloatingMenuDirection.RIGHT
        } else {
            FloatingMenuDirection.LEFT
        }
    }

    // ── 回复气泡（独立小窗口，贴近宠物边缘，5 秒后自动移除） ──

    private var bubbleView: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private fun showReplyBubble(text: String, direction: FloatingMenuDirection) {
        val windowManager = windowManager ?: return
        val density = resources.displayMetrics.density
        val collapsed = (FloatingPetView.PET_SIZE_DP * density).toInt()

        if (bubbleView == null) {
            val bubble = TextView(this).apply {
                textSize = 13f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp3(12), dp3(8), dp3(12), dp3(8))
                setBackgroundResource(R.drawable.bg_chat_bubble)
                elevation = dp3(4).toFloat()
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            bubbleView = bubble
            bubbleParams = params
        }

        val bubble = bubbleView ?: return
        val params = bubbleParams ?: return
        bubble.animate().cancel()
        bubble.text = text
        bubble.measure(0, 0)
        val bubbleW = bubble.measuredWidth
        val bubbleH = bubble.measuredHeight

        // 气泡位置：水平贴近宠物边缘（左/右方向），垂直居中于宠物
        val windowLeft = layoutParams?.x ?: 0
        val windowTop = layoutParams?.y ?: 0
        when (direction) {
            FloatingMenuDirection.LEFT -> {
                params.x = windowLeft - bubbleW - dp3(6)
            }
            else -> {
                params.x = windowLeft + collapsed + dp3(6)
            }
        }
        params.y = windowTop + collapsed / 2 - bubbleH / 2

        // 越界保护：气泡不超出屏幕
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        params.x = params.x.coerceIn(dp3(4), screenWidth - bubbleW - dp3(4))
        params.y = params.y.coerceIn(dp3(4), screenHeight - bubbleH - dp3(4))

        // 展开动画：锚定宠物一侧横向展开。
        // 悬浮窗在左 → 气泡在右、从左往右展开（pivotX=0 锚左缘）；
        // 悬浮窗在右 → 气泡在左、从右往左展开（pivotX=1 锚右缘）。
        val expandFromLeft = direction != FloatingMenuDirection.LEFT
        bubble.pivotX = if (expandFromLeft) 0f else bubbleW.toFloat()
        bubble.pivotY = bubbleH / 2f
        bubble.scaleX = 0.05f
        bubble.alpha = 0f

        runCatching {
            if (bubble.parent == null) {
                windowManager.addView(bubble, params)
            } else {
                windowManager.updateViewLayout(bubble, params)
            }
        }.onFailure { Log.w(TAG, "更新回复气泡失败", it) }

        // 展开动画（0.3 秒横向展开）
        bubble.animate()
            .scaleX(1f)
            .alpha(1f)
            .setDuration(300L)
            .start()

        chatScope.launch {
            delay(4500)
            // 收缩动画后移除（统一向锚点收拢，pivot 已固定）
            bubble.animate()
                .scaleX(0.05f)
                .alpha(0f)
                .setDuration(200L)
                .start()
            delay(300)
            runCatching { windowManager.removeView(bubble) }
                .onFailure { /* 可能已移除 */ }
        }
    }

    private fun dp3(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // ── 独立聊天对话框（居中偏下，与屏幕底部保持距离） ──

    private var chatDialogView: android.widget.LinearLayout? = null
    private var chatDialogParams: WindowManager.LayoutParams? = null
    private var chatInput: android.widget.EditText? = null
    private var chatReply: android.widget.TextView? = null
    private var chatPetBreedId: String = ""
    private var chatPetView: FloatingPetView? = null

    private fun showChatDialog(view: FloatingPetView, breedId: String) {
        val windowManager = windowManager ?: return
        chatPetView = view
        chatPetBreedId = breedId

        if (chatDialogView == null) buildChatDialog()
        val dialog = chatDialogView ?: return
        if (dialog.parent != null) {
            // 已显示 → 直接带回前台
            runCatching { windowManager.updateViewLayout(dialog, chatDialogParams) }
            return
        }
        runCatching {
            windowManager.addView(dialog, chatDialogParams)
        }.onFailure { Log.e(TAG, "挂载悬浮聊天对话框失败", it) }
    }

    private fun hideChatDialog() {
        val windowManager = windowManager ?: return
        val dialog = chatDialogView ?: return
        runCatching { windowManager.removeView(dialog) }
            .onFailure { Log.w(TAG, "移除悬浮聊天对话框失败", it) }
        hideKeyboardFrom(dialog)
    }

    private fun buildChatDialog() {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val dialogW = (300 * density).toInt()

        val petName = runCatching { PetPetRepository(this).snapshot().pet?.name }
            .getOrNull()?.ifEmpty { "宠物" } ?: "宠物"

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(250, 255, 255, 255))
            elevation = dp2(10).toFloat()
            setPadding(dp2(14), dp2(12), dp2(14), dp2(12))
            // 圆角
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp2(18).toFloat()
                setColor(Color.argb(250, 255, 255, 255))
            }
        }

        // 标题行：宠物名 + 关闭按钮
        val titleRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = android.widget.TextView(this).apply {
            text = "$petName 正在和你聊天"
            textSize = 15f
            setTextColor(Color.DKGRAY)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val closeBtn = android.widget.TextView(this).apply {
            text = "✕"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            isClickable = true
            setOnClickListener { hideChatDialog() }
        }
        titleRow.addView(title, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(closeBtn, android.widget.LinearLayout.LayoutParams(dp2(32), dp2(32)))
        container.addView(titleRow)

        // 回复区
        chatReply = android.widget.TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp2(6), 0, dp2(6))
            minHeight = dp2(28)
            text = "和小宠物说点什么吧～"
        }
        container.addView(chatReply, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        // 输入行
        val inputRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = android.widget.EditText(this)
        input.apply {
            hint = "和小宠物说点什么…"
            textSize = 14f
            isSingleLine = true
            setBackgroundColor(Color.TRANSPARENT)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendFromDialog(input)
                true
            } else false
        }
        val sendBtn = android.widget.TextView(this).apply {
            text = "发送"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp2(10).toFloat()
                setColor(Color.parseColor("#38BDF8"))
            }
            isClickable = true
            setOnClickListener { sendFromDialog(input) }
        }
        inputRow.addView(input, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        inputRow.addView(sendBtn, android.widget.LinearLayout.LayoutParams(dp2(56), dp2(36)).apply {
            leftMargin = dp2(8)
        })
        container.addView(inputRow)

        chatInput = input

        // 窗口参数：水平居中，垂直位置在屏幕 62% 处（居中偏下、与底部保持距离）
        chatDialogParams = WindowManager.LayoutParams(
            dialogW,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - dialogW) / 2
            y = (screenHeight * 0.62f).toInt()
        }

        chatDialogView = container
    }

    private fun sendFromDialog(input: android.widget.EditText) {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        hideKeyboardFrom(chatDialogView)
        chatReply?.let {
            it.text = "思考中…"
        }
        val view = chatPetView
        val breedId = chatPetBreedId
        if (view != null) {
            startFloatingChat(view, breedId, text)
        }
    }

    private fun hideKeyboardFrom(anyView: View?) {
        anyView?.let { v ->
            runCatching {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
    }

    private fun dp2(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** 离线本地规则回复（与主界面 replyFor 同源）。 */
    private fun localReply(message: String, isDog: Boolean): String {
        val sound = if (isDog) "汪" else "喵"
        val kind = if (isDog) "小狗" else "小猫"
        return when {
            listOf("难过", "伤心", "不好", "累").any(message::contains) ->
                "$sound……我会安静地陪着你。今天已经很努力了，可以慢一点。"
            listOf("开心", "高兴", "喜欢").any(message::contains) ->
                "听起来是很棒的一天！我也跟着开心起来啦，$sound～"
            listOf("早安", "早上好").any(message::contains) ->
                "早安！今天也请带着一点点好心情出发吧。"
            listOf("晚安", "睡觉").any(message::contains) ->
                "晚安，我会把尾巴圈起来，守着你的好梦。"
            message.endsWith("吗") || message.contains("？") || message.contains("?") ->
                "让我用${kind}的方式想一想……我觉得你的感受最重要，$sound。"
            else -> "我听见啦！再和我多说一点吧，$sound～"
        }
    }

    /** 根据回复内容分析情绪表情（与主界面 analyzeReplyEmotion 同源）。 */
    private fun analyzeEmotion(reply: String): String? {
        val text = reply.trim()
        return when {
            text.contains("开心") || text.contains("高兴") || text.contains("哈哈") ||
                text.contains("太好了") || text.contains("真棒") || text.contains("喜欢") ||
                text.contains("加油") || text.contains("真好") -> "happy"
            text.contains("哇") || text.contains("真的吗") || text.contains("居然") ||
                text.contains("没想到") -> "surprised"
            text.contains("难过") || text.contains("伤心") || text.contains("别担心") ||
                text.contains("抱抱") || text.contains("没事的") || text.contains("心疼") -> "sad"
            else -> null
        }
    }

    /** 应用主界面是否在前台（决定 💬 点击行为：前台跳转 MyPet，后台悬浮窗内聊）。 */
    private fun isAppInForeground(): Boolean {
        return getSharedPreferences(FOREGROUND_PREFERENCES, MODE_PRIVATE)
            .getBoolean(KEY_FOREGROUND, false)
    }

    companion object {
        private const val TAG = "FloatingPetService"
        const val ACTION_SHOW = "cn.petpet.app.action.SHOW_FLOATING_PET"
        const val ACTION_STOP = "cn.petpet.app.action.STOP_FLOATING_PET"
        const val EXTRA_ENABLE_AUTO_START = "cn.petpet.app.extra.ENABLE_FLOATING_AUTO_START"
        private const val CHANNEL_ID = "floating_pet"
        private const val NOTIFICATION_ID = 1001
        private const val FOREGROUND_PREFERENCES = "foreground_state"
        private const val KEY_FOREGROUND = "foreground"
        private const val POSITION_PREFERENCES = "floating_pet_position"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
    }
}
