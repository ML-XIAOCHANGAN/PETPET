package cn.petpet.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.petpet.app.model.CommunityPost
import cn.petpet.app.model.PetProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** T-011 补充：社区帖子三点菜单（举报/收藏/不喜欢）交互。 */
class CommunityPostMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testPet = PetProfile(name = "测试", affection = 0, adoptedAt = "2026-08-15")

    private val posts = listOf(
        CommunityPost(
            id = 2,
            author = "布丁的主人",
            text = "今天和布丁聊了好久。",
            time = "今天 09:30",
            likeCount = 12,
        ),
    )

    private fun setContent(
        onReport: () -> Unit = {},
        onCollect: (Long) -> Unit = {},
        onDislike: (Long) -> Unit = {},
        collected: Boolean = false,
        disliked: Boolean = false,
    ) {
        val post = posts[0].copy(collected = collected, disliked = disliked)
        composeRule.setContent {
            CommunityScreen(
                pet = testPet,
                posts = listOf(post),
                onRefreshNetwork = {},
                onCreatePost = {},
                onToggleLike = {},
                onAddComment = { _, _ -> },
                onReport = onReport,
                onToggleCollect = onCollect,
                onToggleDislike = onDislike,
            )
        }
    }

    @Test
    fun menuShowsReportCollectDislike() {
        setContent()
        composeRule.onNodeWithText("•••").performClick()
        composeRule.onNodeWithText("举报").assertIsDisplayed()
        composeRule.onNodeWithText("收藏").assertIsDisplayed()
        composeRule.onNodeWithText("不喜欢").assertIsDisplayed()
    }

    @Test
    fun collectTriggersCallback() {
        var collectedId: Long? = null
        setContent(onCollect = { collectedId = it })
        composeRule.onNodeWithText("•••").performClick()
        composeRule.onNodeWithText("收藏").performClick()
        assertEquals(2L, collectedId)
    }

    @Test
    fun dislikeTriggersCallback() {
        var dislikedId: Long? = null
        setContent(onDislike = { dislikedId = it })
        composeRule.onNodeWithText("•••").performClick()
        composeRule.onNodeWithText("不喜欢").performClick()
        assertEquals(2L, dislikedId)
    }

    @Test
    fun collectedAndDislikedStatesReflectedInMenu() {
        setContent(collected = true, disliked = true)
        composeRule.onNodeWithText("•••").performClick()
        composeRule.onNodeWithText("取消收藏").assertIsDisplayed()
        composeRule.onNodeWithText("取消不喜欢").assertIsDisplayed()
        // 收藏状态显示 ⭐ 标记
        composeRule.onNodeWithText("⭐").assertIsDisplayed()
        assertTrue(true)
    }
}
