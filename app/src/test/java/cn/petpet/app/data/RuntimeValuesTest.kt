package cn.petpet.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RuntimeValuesTest {
    @Test
    fun `固定时钟会提供可预测的业务日期和时间`() {
        val clock = Clock.fixed(
            Instant.parse("2026-08-09T14:30:45Z"),
            ZoneId.of("Asia/Shanghai"),
        )
        val runtimeValues = RuntimeValues(clock, IdentifierSource { 42L })

        assertEquals(LocalDate.of(2026, 8, 9), runtimeValues.today())
        assertEquals(LocalDateTime.of(2026, 8, 9, 22, 30, 45), runtimeValues.now())
        assertEquals(42L, runtimeValues.nextId())
    }

    @Test
    fun `同毫秒和时钟回拨时标识仍严格递增`() {
        val times = longArrayOf(1_000L, 1_000L, 999L, 1_003L)
        var index = 0
        val identifiers = MonotonicIdentifierSource { times[index++] }

        assertEquals(1_000L, identifiers.nextId())
        assertEquals(1_001L, identifiers.nextId())
        assertEquals(1_002L, identifiers.nextId())
        assertEquals(1_003L, identifiers.nextId())
    }
}
