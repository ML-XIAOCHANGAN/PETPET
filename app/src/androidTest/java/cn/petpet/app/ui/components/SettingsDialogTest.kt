package cn.petpet.app.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.petpet.app.model.ChatProvider
import cn.petpet.app.model.PetBreeds
import cn.petpet.app.model.PetProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** T-011：设置对话框渲染（宠物档案/模型配置/隐私说明展开）。 */
class SettingsDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testPet = PetProfile(
        name = "测试",
        breedId = "siamese",
        affection = 5,
        adoptedAt = "2026-08-15",
    )

    @Test
    fun rendersPetProfileAndModelConfig() {
        composeRule.setContent {
            SettingsDialog(
                pet = testPet,
                darkTheme = false,
                chatProvider = ChatProvider.ZHIPU,
                chatApiKey = "",
                onDarkThemeChange = {},
                onChatConfigChange = { _, _ -> },
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("⚙️ 设置").assertIsDisplayed()
        composeRule.onNodeWithText("AI 对话模型", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("ChatGPT").assertIsDisplayed()
        composeRule.onNodeWithText("DeepSeek").assertIsDisplayed()
        composeRule.onNodeWithText("GLM").assertIsDisplayed()
        composeRule.onNodeWithText("Kimi").assertIsDisplayed()
        composeRule.onNodeWithText("保存对话模型").assertIsDisplayed()
        // 不再有重新领养
        composeRule.onAllNodesWithText("重新领养一只新伙伴").assertCountEquals(0)
    }

    @Test
    fun saveModelConfigInvokesCallback() {
        var savedProvider: ChatProvider? = null
        var savedKey: String? = null
        composeRule.setContent {
            SettingsDialog(
                pet = testPet,
                darkTheme = false,
                chatProvider = ChatProvider.ZHIPU,
                chatApiKey = "",
                onDarkThemeChange = {},
                onChatConfigChange = { p, k ->
                    savedProvider = p
                    savedKey = k
                },
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("DeepSeek").performClick()
        composeRule.onNodeWithText("保存对话模型").performClick()
        assertEquals(ChatProvider.DEEPSEEK, savedProvider)
        assertTrue(savedKey != null)
    }

    @Test
    fun privacySectionExpands() {
        composeRule.setContent {
            SettingsDialog(
                pet = testPet,
                darkTheme = false,
                chatProvider = ChatProvider.ZHIPU,
                chatApiKey = "",
                onDarkThemeChange = {},
                onChatConfigChange = { _, _ -> },
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("隐私与权限说明").assertIsDisplayed()
        composeRule.onNodeWithText("查看").performClick()
        composeRule.onNodeWithText("数据全部保存在你的设备本地", substring = true).assertIsDisplayed()
    }
}
