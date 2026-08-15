package cn.petpet.app

import android.app.Application
import cn.petpet.app.data.CrashLogHandler

class PetPetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 全局崩溃日志（T-016）：记录后交还系统默认处理器
        Thread.setDefaultUncaughtExceptionHandler(CrashLogHandler(this))
    }
}
