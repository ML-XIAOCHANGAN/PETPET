package cn.petpet.app.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器（T-016 可观测性，轻量落地）：
 * 崩溃时将时间、线程与完整堆栈写入应用私有目录 crash/，便于离线定位；
 * 不收集聊天内容、宠物数据等隐私信息，仅记录崩溃本身。
 * 写盘后仍交还系统默认处理器（保持原生崩溃弹窗与 ANR 行为）。
 */
class CrashLogHandler(
    context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler(),
) : Thread.UncaughtExceptionHandler {

    private val crashDir = File(context.filesDir, "crash").apply { mkdirs() }
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val fileName = "crash_${timeFormat.format(Date())}.txt"
            val writer = StringWriter()
            PrintWriter(writer).use {
                it.println("时间: ${Date()}")
                it.println("线程: ${thread.name} (${thread.id})")
                it.println("进程: ${android.os.Process.myPid()}")
                it.println("异常: ${throwable.javaClass.name}: ${throwable.message}")
                it.println("堆栈:")
                throwable.printStackTrace(it)
            }
            File(crashDir, fileName).writeText(writer.toString())
            Log.w(TAG, "已记录崩溃日志: $fileName")
        }.onFailure { e ->
            Log.e(TAG, "写入崩溃日志失败", e)
        }
        // 交还系统默认处理器，保持原生崩溃行为
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        private const val TAG = "PetPetCrash"
    }
}
