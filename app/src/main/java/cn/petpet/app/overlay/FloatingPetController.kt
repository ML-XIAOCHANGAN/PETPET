package cn.petpet.app.overlay

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.R
import cn.petpet.app.model.PetBreeds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FloatingPetController {
    private const val PREFERENCES = "floating_pet_state"
    private const val KEY_AUTO_START = "auto_start"
    private const val TAG = "FloatingPetController"
    private val _running = MutableStateFlow(false)

    val running: StateFlow<Boolean> = _running.asStateFlow()

    internal fun setRunning(running: Boolean) {
        _running.value = running
    }

    fun isAutoStartEnabled(context: Context): Boolean {
        return runCatching {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_START, false)
        }.getOrDefault(false)
    }

    @SuppressLint("ApplySharedPref")
    fun setAutoStartEnabled(context: Context, enabled: Boolean): Boolean {
        // 这是低频且关键的用户开关，确认落盘后再结束启动或停止流程。
        val saved = runCatching {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTO_START, enabled)
                .commit()
        }.onFailure {
            Log.e(TAG, "保存悬浮小猫自启动状态失败", it)
        }.getOrDefault(false)
        if (!saved) Log.e(TAG, "悬浮小猫自启动状态未能写入本地存储")
        return saved
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        // 安卓 13 及以上即使未授予通知权限，也允许启动前台服务；
        // 差异仅在于通知不会出现在通知抽屉，因此这里只校验悬浮窗权限。
        return canDrawOverlays(context)
    }

    fun canDrawOverlays(context: Context): Boolean {
        return runCatching { Settings.canDrawOverlays(context) }
            .onFailure { Log.e(TAG, "读取悬浮窗授权状态失败", it) }
            .getOrDefault(false)
    }

    fun start(context: Context, enableAutoStart: Boolean = true): Boolean {
        if (!hasRequiredPermissions(context)) return false
        val dispatched = runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FloatingPetService::class.java)
                    .setAction(FloatingPetService.ACTION_SHOW)
                    .putExtra(FloatingPetService.EXTRA_ENABLE_AUTO_START, enableAutoStart),
            )
        }.onFailure {
            Log.e(TAG, "启动悬浮小猫服务失败", it)
        }.isSuccess
        return dispatched
    }

    fun stop(context: Context, disableAutoStart: Boolean = true): Boolean {
        val stopResult = runCatching {
            context.stopService(Intent(context, FloatingPetService::class.java))
        }.onFailure {
            Log.e(TAG, "停止悬浮小猫服务失败", it)
        }
        if (stopResult.isSuccess) setRunning(false)
        if (disableAutoStart) setAutoStartEnabled(context, false)
        return stopResult.getOrDefault(false)
    }
}

/**
 * 使用用户领养宠物主头像作为首页悬浮窗按钮。
 * 未开启时负责申请权限并启动；已开启时再次点击会关闭并停用自启动。
 */
@Composable
fun FloatingPetImageButton(modifier: Modifier = Modifier, breedId: String? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var overlayAllowed by remember {
        mutableStateOf(FloatingPetController.canDrawOverlays(context))
    }
    val running by FloatingPetController.running.collectAsStateWithLifecycle()
    val interactionSource = remember { MutableInteractionSource() }
    val avatarRes = remember(breedId) {
        PetBreeds.byId(breedId ?: "")?.avatarRes ?: R.drawable.cat_main
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        overlayAllowed = FloatingPetController.canDrawOverlays(context)
        if (overlayAllowed) FloatingPetController.start(context)
    }

    fun startAfterNotificationCheck() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            FloatingPetController.start(context)
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        overlayAllowed = FloatingPetController.canDrawOverlays(context)
        if (overlayAllowed) startAfterNotificationCheck()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayAllowed = FloatingPetController.canDrawOverlays(context)
                if (!overlayAllowed) {
                    FloatingPetController.stop(context, disableAutoStart = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = {
                when {
                    running -> {
                        FloatingPetController.stop(context)
                    }
                    overlayAllowed -> startAfterNotificationCheck()
                    else -> overlayPermissionLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(avatarRes),
            contentDescription = if (running) "关闭悬浮宠物" else "设置并开启悬浮宠物",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (running) 1f else 0.78f },
            contentScale = ContentScale.Fit,
        )
    }
}
