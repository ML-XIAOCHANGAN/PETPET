package cn.petpet.app.domain

data class RewardResult(
    val nextCount: Int,
    val gained: Int,
    val rewarded: Boolean,
)

object AffectionRules {
    const val CHAT_TARGET = 3
    const val CHAT_REWARD = 5
    const val INTERACTION_DAILY_LIMIT = 5
    const val INTERACTION_REWARD = 1

    fun afterValidChat(validCount: Int, alreadyRewarded: Boolean): RewardResult {
        val normalizedCount = validCount.coerceIn(0, CHAT_TARGET)
        val nextCount = (normalizedCount + 1).coerceAtMost(CHAT_TARGET)
        val shouldReward = !alreadyRewarded && nextCount >= CHAT_TARGET
        return RewardResult(
            nextCount = nextCount,
            gained = if (shouldReward) CHAT_REWARD else 0,
            rewarded = alreadyRewarded || shouldReward,
        )
    }

    fun afterInteraction(currentCount: Int): RewardResult {
        val normalizedCount = currentCount.coerceIn(0, INTERACTION_DAILY_LIMIT)
        if (normalizedCount >= INTERACTION_DAILY_LIMIT) {
            return RewardResult(
                nextCount = INTERACTION_DAILY_LIMIT,
                gained = 0,
                rewarded = true,
            )
        }

        val nextCount = normalizedCount + 1
        return RewardResult(
            nextCount = nextCount,
            gained = INTERACTION_REWARD,
            rewarded = nextCount >= INTERACTION_DAILY_LIMIT,
        )
    }
}
