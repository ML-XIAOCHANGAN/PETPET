package cn.petpet.app.ui.fitcut

import androidx.compose.foundation.Canvas
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
import cn.petpet.app.data.fitcut.FitCutCalculator
import cn.petpet.app.ui.components.FitBarCompare
import cn.petpet.app.ui.components.FitLineChart
import cn.petpet.app.ui.components.MacroRing

/** 数据统计页：趋势 + 对比 + 营养素 + BMI + 成就。 */
@Composable
fun FitCutStatsScreen(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.data
    val plan = state.plan ?: return
    val profile = data.profile ?: return

    val weights = data.weightRecords.sortedBy { it.date }
    val weightSeries = weights.map { it.weightKg.toFloat() }
    val currentWeight = weights.lastOrNull()?.weightKg ?: profile.weightKg
    val bmiValue = FitCutCalculator.bmi(currentWeight, profile.heightCm)

    // 近 7 天热量：摄入 vs 消耗（消耗按 TDEE 计）
    val days = (0L..6L).map { java.time.LocalDate.now().minusDays(it).toString() }.reversed()
    val eatenByDay = data.mealRecords.groupBy { it.date }.mapValues { (_, ms) -> ms.sumOf { it.calories } }
    val burnedByDay = data.workoutRecords.groupBy { it.date }.mapValues { (_, ws) -> ws.sumOf { it.calories } }
    val eatenSeries = days.map { eatenByDay[it]?.toFloat() ?: 0f }
    val burnedSeries = days.map { (burnedByDay[it] ?: 0.0).toFloat() }

    val today = viewModel.today()
    val todayMeals = data.mealRecords.filter { it.date == today }
    val p = todayMeals.sumOf { it.protein }
    val c = todayMeals.sumOf { it.carb }
    val f = todayMeals.sumOf { it.fat }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 体重趋势
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("体重变化（${weights.size} 条记录）", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (weightSeries.size >= 2) {
                    FitLineChart(values = weightSeries, color = FitCutGreen, height = 120)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "起始 ${weights.first().weightKg} kg → 当前 ${weights.last().weightKg} kg",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("至少需要 2 条体重记录才能绘制趋势", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // BMI
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("BMI", fontWeight = FontWeight.Bold)
                    Text(
                        "当前 ${FitCutCalculator.round1(bmiValue)}（${FitCutCalculator.bmiCategory(bmiValue)}）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = FitCutGreen,
                    )
                    Text(
                        "目标体重 ${profile.targetWeightKg}kg 时 BMI 约 ${FitCutCalculator.round1(FitCutCalculator.bmi(profile.targetWeightKg, profile.heightCm))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "⚖️",
                    fontSize = 28.sp,
                )
            }
        }

        // 近 7 天摄入 vs 消耗
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("近 7 天摄入 vs 消耗", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                FitBarCompare(
                    leftValue = eatenSeries.sum(),
                    rightValue = burnedSeries.sum(),
                    leftLabel = "摄入",
                    rightLabel = "运动消耗",
                    leftColor = FitCutOrange,
                    rightColor = FitCutGreen,
                    height = 90,
                )
            }
        }

        // 今日营养素占比
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MacroRing(
                    proteinRatio = p.toFloat(),
                    carbRatio = c.toFloat(),
                    fatRatio = f.toFloat(),
                    proteinColor = FitCutGreen,
                    carbColor = Color(0xFF3B82F6),
                    fatColor = FitCutOrange,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendDot(FitCutGreen, "蛋白质 ${p.toInt()}g")
                    LegendDot(Color(0xFF3B82F6), "碳水 ${c.toInt()}g")
                    LegendDot(FitCutOrange, "脂肪 ${f.toInt()}g")
                    Text(
                        "目标：${plan.targetProteinG.toInt()}/${plan.targetCarbG.toInt()}/${plan.targetFatG.toInt()} g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 成就
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("🏆 成就", fontWeight = FontWeight.Bold)
                state.achievements.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (a.unlocked) a.icon else "🔒",
                            fontSize = 18.sp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                a.title,
                                fontSize = 13.sp,
                                fontWeight = if (a.unlocked) FontWeight.Bold else FontWeight.Normal,
                                color = if (a.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${a.progress.coerceAtMost(a.target)} / ${a.target}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (a.unlocked) {
                            Text("已达成", fontSize = 11.sp, color = FitCutGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(12.dp).height(12.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp)
    }
}
