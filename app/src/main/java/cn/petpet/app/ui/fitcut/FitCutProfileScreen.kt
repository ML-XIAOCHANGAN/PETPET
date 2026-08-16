package cn.petpet.app.ui.fitcut

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.fitcut.FitCutCalculator
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

/** 我的：档案 / 计划 / 今日体重录入 / 重置。 */
@Composable
fun FitCutProfileScreen(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.data.profile ?: return
    val plan = state.plan ?: return

    var weightInput by remember { mutableStateOf(profile.weightKg.toString()) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 档案卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("我的档案", fontWeight = FontWeight.Bold)
                ProfileRow("性别", profile.gender.label)
                ProfileRow("年龄", "${profile.age} 岁")
                ProfileRow("身高", "${profile.heightCm} cm")
                ProfileRow("当前体重", "${profile.weightKg} kg")
                ProfileRow("目标体重", "${profile.targetWeightKg} kg")
                ProfileRow("活动水平", "${profile.activity.label}（×${profile.activity.factor}）")
                Spacer(Modifier.height(4.dp))
                VoxelButton(
                    text = "修改档案",
                    onClick = viewModel::editProfile,
                    compact = true,
                    style = VoxelButtonStyle.SECONDARY,
                )
            }
        }

        // 今日体重录入
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("记录今日体重", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { v -> weightInput = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("体重（kg）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                VoxelButton(
                    text = "保存体重",
                    onClick = {
                        weightInput.toDoubleOrNull()?.takeIf { it in 30.0..300.0 }?.let { w ->
                            viewModel.logWeight(w)
                            weightInput = w.toString()
                        }
                    },
                    compact = true,
                    enabled = (weightInput.toDoubleOrNull() ?: 0.0) in 30.0..300.0,
                )
            }
        }

        // 计划卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("我的减脂计划", fontWeight = FontWeight.Bold)
                ProfileRow("BMR", "${plan.bmr.toInt()} kcal/天")
                ProfileRow("TDEE", "${plan.tdee.toInt()} kcal/天")
                ProfileRow("每日推荐摄入", "${plan.recommendedCalories.toInt()} kcal")
                ProfileRow("热量缺口", if (plan.isMaintenance) "维持模式" else "${plan.calorieDeficit.toInt()} kcal/天")
                ProfileRow("计划周期", "${plan.totalWeeks} 周")
                ProfileRow("每周目标", "${FitCutCalculator.round1(plan.weeklyTargetKg)} kg")
                ProfileRow("蛋白/碳水/脂肪", "${plan.targetProteinG.toInt()}/${plan.targetCarbG.toInt()}/${plan.targetFatG.toInt()} g")
            }
        }

        // 关于 + 重置
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("关于", fontWeight = FontWeight.Bold)
                Text(
                    "FitCut 减脂助手 · PETPET 内嵌模块\nBMR 采用 Mifflin-St Jeor 公式，食物热量为常见估算值，仅供参考。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!confirmReset) {
                    VoxelButton(
                        text = "重置全部数据",
                        onClick = { confirmReset = true },
                        compact = true,
                        style = VoxelButtonStyle.NEUTRAL,
                    )
                } else {
                    Text("确定清空所有 FitCut 数据并重新建档？", fontSize = 13.sp, color = FitCutOrange, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoxelButton(text = "取消", onClick = { confirmReset = false }, compact = true, style = VoxelButtonStyle.NEUTRAL)
                        VoxelButton(
                            text = "确认重置",
                            onClick = {
                                confirmReset = false
                                viewModel.resetAll()
                            },
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
