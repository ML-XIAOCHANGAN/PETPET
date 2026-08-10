package cn.petpet.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 手机完成启动后，仅恢复用户此前主动开启过的悬浮小猫。
 * 从未授权或已经关闭自启动时不会在后台自行运行。
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val isBootAction = intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == ACTION_QUICK_BOOT
        if (!isBootAction) return
        if (!FloatingPetController.isAutoStartEnabled(context)) return
        if (!FloatingPetController.hasRequiredPermissions(context)) return

        FloatingPetController.start(
            context = context,
            enableAutoStart = false,
        )
    }

    private companion object {
        const val ACTION_QUICK_BOOT = "android.intent.action.QUICKBOOT_POWERON"
    }
}
