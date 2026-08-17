package cn.petpet.app.ui.fitcut

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

private data class FitCutTab(val label: String, val icon: String)

private val tabs = listOf(
    FitCutTab("概览", "📊"),
    FitCutTab("饮食", "🍽️"),
    FitCutTab("运动", "🏃"),
    FitCutTab("统计", "📈"),
    FitCutTab("我的", "👤"),
)

/**
 * FitCut 减脂助手主界面：未建档走引导，已建档显示 5 tab 主界面。
 */
@Composable
fun FitCutScreen(
    viewModel: FitCutViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.data.profile == null || state.onboardingStep < 4) {
        // 未建档（或正在编辑档案）→ 引导流程（带退出按钮，避免卡在引导页）
        FitCutOnboarding(viewModel = viewModel, onClose = onClose)
        return
    }

    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("FitCut 减脂助手", fontSize = 22.sp, fontWeight = FontWeight.Black, color = FitCutGreen)
            Spacer(Modifier.weight(1f))
            VoxelButton(
                text = "关闭",
                onClick = onClose,
                compact = true,
                style = VoxelButtonStyle.NEUTRAL,
            )
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> FitCutDashboard(viewModel)
                1 -> FitCutDietScreen(viewModel)
                2 -> FitCutExerciseScreen(viewModel)
                3 -> FitCutStatsScreen(viewModel)
                4 -> FitCutProfileScreen(viewModel)
            }
        }

        // 底部 tab 栏
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                tabs.forEachIndexed { index, t ->
                    val selected = tab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tab = index }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(t.icon, fontSize = 18.sp)
                        Text(
                            t.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) FitCutGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
