package cn.petpet.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.petpet.app.model.HomeSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** T-011：底部导航三入口切换与 emoji 图标（Compose 界面测试）。 */
class VoxelNavigationBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsAllTabsWithEmoji() {
        composeRule.setContent {
            VoxelNavigationBar(
                selected = HomeSection.MY_PET,
                onSelect = {},
                petType = cn.petpet.app.model.PetType.CAT,
            )
        }
        composeRule.onNodeWithText("My Pet").assertIsDisplayed()
        composeRule.onNodeWithText("成长").assertIsDisplayed()
        composeRule.onNodeWithText("社区").assertIsDisplayed()
        composeRule.onNodeWithText("🐱").assertIsDisplayed()
        composeRule.onNodeWithText("📖").assertIsDisplayed()
        composeRule.onNodeWithText("👥").assertIsDisplayed()
    }

    @Test
    fun clickTabTriggersSelect() {
        var selected: HomeSection? = null
        composeRule.setContent {
            VoxelNavigationBar(
                selected = HomeSection.MY_PET,
                onSelect = { selected = it },
            )
        }
        composeRule.onNodeWithText("社区").performClick()
        assertEquals(HomeSection.COMMUNITY, selected)
    }

    @Test
    fun dogPetTypeShowsDogEmoji() {
        composeRule.setContent {
            VoxelNavigationBar(
                selected = HomeSection.MY_PET,
                onSelect = {},
                petType = cn.petpet.app.model.PetType.DOG,
            )
        }
        composeRule.onNodeWithText("🐶").assertIsDisplayed()
    }
}
