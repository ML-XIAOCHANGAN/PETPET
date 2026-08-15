package cn.petpet.app.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import cn.petpet.app.MainActivity
import cn.petpet.app.R
import cn.petpet.app.data.PetPetRepository
import cn.petpet.app.model.PetBreeds
import kotlin.math.abs

class FloatingPetService : Service() {
    private var windowManager: WindowManager? = null
    private var notificationChannelReady = false
    private var floatingView: FloatingPetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

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
            onChat = { openMainScreen(openChat = true) },
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

    companion object {
        private const val TAG = "FloatingPetService"
        const val ACTION_SHOW = "cn.petpet.app.action.SHOW_FLOATING_PET"
        const val ACTION_STOP = "cn.petpet.app.action.STOP_FLOATING_PET"
        const val EXTRA_ENABLE_AUTO_START = "cn.petpet.app.extra.ENABLE_FLOATING_AUTO_START"
        private const val CHANNEL_ID = "floating_pet"
        private const val NOTIFICATION_ID = 1001
        private const val POSITION_PREFERENCES = "floating_pet_position"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
    }
}
