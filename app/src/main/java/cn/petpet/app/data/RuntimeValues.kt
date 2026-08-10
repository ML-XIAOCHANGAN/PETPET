package cn.petpet.app.data

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

fun interface IdentifierSource {
    fun nextId(): Long
}

/**
 * 即使系统时钟短暂回拨或同一毫秒内连续创建数据，也生成进程内严格递增的标识。
 */
class MonotonicIdentifierSource(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : IdentifierSource {
    private var lastId = Long.MIN_VALUE

    @Synchronized
    override fun nextId(): Long {
        val current = currentTimeMillis()
        val next = when {
            current > lastId -> current
            lastId < Long.MAX_VALUE -> lastId + 1L
            else -> error("标识已达到可表示的最大值")
        }
        lastId = next
        return next
    }
}

/**
 * 集中提供业务时间与标识，测试可注入固定时钟和可预测标识源。
 */
class RuntimeValues(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val identifiers: IdentifierSource = MonotonicIdentifierSource(),
) {
    fun today(): LocalDate = LocalDate.now(clock)

    fun now(): LocalDateTime = LocalDateTime.now(clock)

    fun nextId(): Long = identifiers.nextId()
}

internal object ApplicationRuntimeValues {
    val instance = RuntimeValues()
}
