package cn.petpet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AffectionRulesTest {
    @Test
    fun `第三次有效聊天发放一次奖励`() {
        val result = AffectionRules.afterValidChat(validCount = 2, alreadyRewarded = false)

        assertEquals(3, result.nextCount)
        assertEquals(5, result.gained)
        assertTrue(result.rewarded)
    }

    @Test
    fun `已经发放过聊天奖励时不重复增加`() {
        val result = AffectionRules.afterValidChat(validCount = 3, alreadyRewarded = true)

        assertEquals(3, result.nextCount)
        assertEquals(0, result.gained)
        assertTrue(result.rewarded)
    }

    @Test
    fun `互动未到上限时增加一点`() {
        val result = AffectionRules.afterInteraction(currentCount = 3)

        assertEquals(4, result.nextCount)
        assertEquals(1, result.gained)
        assertFalse(result.rewarded)
    }

    @Test
    fun `互动达到上限后不再增加`() {
        val result = AffectionRules.afterInteraction(currentCount = 5)

        assertEquals(5, result.nextCount)
        assertEquals(0, result.gained)
        assertTrue(result.rewarded)
    }

    @Test
    fun `损坏的负数聊天计数会从零开始修正`() {
        val result = AffectionRules.afterValidChat(validCount = -4, alreadyRewarded = false)

        assertEquals(1, result.nextCount)
        assertEquals(0, result.gained)
        assertFalse(result.rewarded)
    }

    @Test
    fun `损坏的负数互动计数会从零开始修正`() {
        val result = AffectionRules.afterInteraction(currentCount = -2)

        assertEquals(1, result.nextCount)
        assertEquals(1, result.gained)
        assertFalse(result.rewarded)
    }
}
