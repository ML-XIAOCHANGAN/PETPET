package cn.petpet.app.ui.fitcut

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.fitcut.ActivityLevel
import cn.petpet.app.data.fitcut.FitCutCalculator
import cn.petpet.app.data.fitcut.FitCutExerciseLibrary
import cn.petpet.app.data.fitcut.Gender
import cn.petpet.app.data.fitcut.UserProfile
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

/** FitCut 引导页主色调（健康绿）。 */
val FitCutGreen = Color(0xFF22C55E)
val FitCutGreenDark = Color(0xFF16A34A)
val FitCutOrange = Color(0xFFF97316)

@Composable
fun FitCutOnboarding(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("FitCut 减脂助手", fontSize = 26.sp, fontWeight = FontWeight.Black, color = FitCutGreen)
        Text(
            "科学计算你的减脂计划 · 第 ${state.onboardingStep + 1} 步 / 共 3 步",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        when (state.onboardingStep) {
            0 -> ProfileInputStep(viewModel)
            1 -> CalculationStep(viewModel)
            2 -> PlanConfirmStep(viewModel)
        }
    }
}

// ---------------------------------------------------------------------------
// 第 1 步：档案输入
// ---------------------------------------------------------------------------

@Composable
private fun ProfileInputStep(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("身体档案", fontWeight = FontWeight.Bold)

            // 性别
            Text("性别", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Gender.values().forEach { g ->
                    ChoiceChip(
                        text = g.label,
                        selected = state.draftGender == g,
                        onClick = { viewModel.setDraftGender(g) },
                    )
                }
            }

            // 年龄 / 身高 两列
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.draftAge,
                    onValueChange = viewModel::setDraftAge,
                    label = { Text("年龄") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.draftHeight,
                    onValueChange = viewModel::setDraftHeight,
                    label = { Text("身高cm") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            // 体重 / 目标 两列
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.draftWeight,
                    onValueChange = viewModel::setDraftWeight,
                    label = { Text("体重kg") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.draftTargetWeight,
                    onValueChange = viewModel::setDraftTargetWeight,
                    label = { Text("目标kg") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            // 活动水平：2×2 网格
            Text("活动水平", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActivityLevel.values().toList().chunked(2).forEach { rowLevels ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowLevels.forEach { level ->
                        ActivityOption(
                            level = level,
                            selected = state.draftActivity == level,
                            onClick = { viewModel.setDraftActivity(level) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            VoxelButton(
                text = "下一步：计算我的计划",
                onClick = viewModel::nextOnboardingStep,
            )
        }
    }
}

/** 活动水平选项（网格卡片）。 */
@Composable
private fun ActivityOption(
    level: ActivityLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) FitCutGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (selected) FitCutGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (selected) "◉" else "○",
                color = if (selected) FitCutGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    level.label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    "×${level.factor}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 第 2 步：计算结果展示
// ---------------------------------------------------------------------------

@Composable
private fun CalculationStep(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = draftProfile(state)
    val plan = profile?.let { FitCutCalculator.buildPlan(it) }

    if (plan == null) {
        Text("请先在上一步填好档案", color = MaterialTheme.colorScheme.onSurfaceVariant)
        VoxelButton(text = "返回", onClick = viewModel::prevOnboardingStep, style = VoxelButtonStyle.NEUTRAL)
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("你的减脂计划", fontWeight = FontWeight.Bold)

            StatRow("基础代谢 BMR", "${plan.bmr.toInt()} kcal/天")
            StatRow("每日总消耗 TDEE", "${plan.tdee.toInt()} kcal/天")
            StatRow(
                if (plan.isMaintenance) "热量缺口" else "每日热量缺口",
                if (plan.isMaintenance) "维持模式" else "${plan.calorieDeficit.toInt()} kcal/天",
                highlight = !plan.isMaintenance,
            )
            StatRow("每日推荐摄入", "${plan.recommendedCalories.toInt()} kcal", highlight = true)
            Spacer(Modifier.height(4.dp))
            Text("三大营养素目标", fontWeight = FontWeight.Bold)
            StatRow("蛋白质", "${plan.targetProteinG.toInt()} g/天（30%）")
            StatRow("碳水化合物", "${plan.targetCarbG.toInt()} g/天（40%）")
            StatRow("脂肪", "${plan.targetFatG.toInt()} g/天（30%）")
            Spacer(Modifier.height(4.dp))
            Text("减脂节奏", fontWeight = FontWeight.Bold)
            StatRow("计划周期", "${plan.totalWeeks} 周")
            StatRow("每周目标", "${FitCutCalculator.round1(plan.weeklyTargetKg)} kg/周")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VoxelButton(
                    text = "上一步",
                    onClick = viewModel::prevOnboardingStep,
                    style = VoxelButtonStyle.NEUTRAL,
                    compact = true,
                )
                VoxelButton(
                    text = "下一步：运动安排",
                    onClick = viewModel::nextOnboardingStep,
                    compact = true,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 第 3 步：计划确认（运动安排）
// ---------------------------------------------------------------------------

@Composable
private fun PlanConfirmStep(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("每周运动安排", fontWeight = FontWeight.Bold)
            Text(
                "建议每周 3-4 次运动：有氧 2 次 + 力量 1-2 次，交替进行。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            Text("🏃 有氧推荐", fontWeight = FontWeight.Bold)
            FitCutExerciseLibrary.byCategory("有氧").take(2).forEach { e ->
                Text("· ${e.name}（${e.setsReps}）", fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))

            Text("💪 力量推荐", fontWeight = FontWeight.Bold)
            FitCutExerciseLibrary.byCategory("力量").forEach { e ->
                Text(
                    "· ${e.name}（${e.targetMuscle} · ${e.setsReps}）",
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(4.dp))

            Text(
                "小提示：减脂不是饿肚子。保持 300-500 kcal 缺口 + 蛋白质充足，才能减脂不伤代谢。",
                fontSize = 12.sp,
                color = FitCutOrange,
            )
            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VoxelButton(
                    text = "上一步",
                    onClick = viewModel::prevOnboardingStep,
                    style = VoxelButtonStyle.NEUTRAL,
                    compact = true,
                )
                VoxelButton(
                    text = "开始我的减脂之旅 🎉",
                    onClick = viewModel::saveProfile,
                    compact = true,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 小组件
// ---------------------------------------------------------------------------

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) FitCutGreen else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (highlight) FitCutGreen else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 从引导草稿构造临时档案（任一字段不完整返回 null）。 */
private fun draftProfile(state: FitCutUiState): UserProfile? {
    val age = state.draftAge.toIntOrNull() ?: return null
    val height = state.draftHeight.toIntOrNull() ?: return null
    val weight = state.draftWeight.toDoubleOrNull() ?: return null
    val target = state.draftTargetWeight.toDoubleOrNull() ?: return null
    return UserProfile(
        gender = state.draftGender,
        age = age,
        heightCm = height,
        weightKg = weight,
        targetWeightKg = target,
        activity = state.draftActivity,
        createdAt = "",
    )
}
