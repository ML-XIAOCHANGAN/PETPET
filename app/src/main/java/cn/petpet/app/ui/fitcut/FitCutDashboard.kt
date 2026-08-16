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
import cn.petpet.app.data.fitcut.MealType
import cn.petpet.app.ui.components.FitLineChart
import cn.petpet.app.ui.components.ProgressRing

/** 今日概览。 */
@Composable
fun FitCutDashboard(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.data
    val plan = state.plan ?: return

    val today = viewModel.today()
    val todayMeals = data.mealRecords.filter { it.date == today }
    val eaten = todayMeals.sumOf { it.calories }
    val remaining = (plan.recommendedCalories - eaten).coerceAtLeast(0.0)
    val protein = todayMeals.sumOf { it.protein }
    val carb = todayMeals.sumOf { it.carb }
    val fat = todayMeals.sumOf { it.fat }

    val todayWorkouts = data.workoutRecords.filter { it.date == today }
    val workoutCalories = todayWorkouts.sumOf { it.calories }
    val streak = state.achievements.firstOrNull { it.id == "streak_7" }?.progress ?: 0

    val weights = data.weightRecords.sortedBy { it.date }
    val weightSeries = weights.map { it.weightKg.toFloat() }

    val totalTarget = plan.recommendedCalories.toFloat()
    val ringColor = when {
        eaten <= plan.recommendedCalories -> FitCutGreen
        else -> FitCutOrange
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 今日热量进度
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("今日热量", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                ProgressRing(
                    progress = (eaten / totalTarget).toFloat(),
                    color = ringColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    size = 130f,
                    strokeWidth = 12f,
                    centerText = "${eaten.toInt()}",
                    subText = "已摄入 / 目标 ${plan.recommendedCalories.toInt()} kcal",
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (remaining > 0) "还可摄入 ${remaining.toInt()} kcal" else "今日热量已达标，注意别吃撑哦",
                    fontSize = 13.sp,
                    color = if (remaining > 0) FitCutGreen else FitCutOrange,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // 今日运动
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
                    Text("今日运动", fontWeight = FontWeight.Bold)
                    Text(
                        if (todayWorkouts.isEmpty()) "还没打卡，动起来吧！" else "已打卡 ${todayWorkouts.size} 次",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "🔥 $streak 天",
                    fontWeight = FontWeight.Black,
                    color = FitCutOrange,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${workoutCalories.toInt()} kcal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FitCutGreen,
                )
            }
        }

        // 体重趋势
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("体重趋势", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (weightSeries.size >= 2) {
                    FitLineChart(
                        values = weightSeries,
                        color = FitCutGreen,
                        height = 110,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "最新 ${weights.last().weightKg} kg · ${weights.last().date}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "记录体重后展示趋势曲线（在我的页可录入今日体重）",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 今日待办
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("今日待办", fontWeight = FontWeight.Bold)
                val mealsDone = MealType.entries.all { type ->
                    todayMeals.any { it.mealType == type }
                }
                TodoItem(done = todayMeals.isNotEmpty(), text = if (todayMeals.isNotEmpty()) "已记录饮食 ✓" else "记录三餐")
                TodoItem(done = todayWorkouts.isNotEmpty(), text = if (todayWorkouts.isNotEmpty()) "已完成运动打卡 ✓" else "完成一次运动打卡")
                TodoItem(done = false, text = "喝水 8 杯（约 2L）")
            }
        }

        // 营养概览
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("今日营养", fontWeight = FontWeight.Bold)
                MacroStat("蛋白质", protein, plan.targetProteinG, FitCutGreen)
                MacroStat("碳水", carb, plan.targetCarbG, Color(0xFF3B82F6))
                MacroStat("脂肪", fat, plan.targetFatG, FitCutOrange)
            }
        }
    }
}

@Composable
private fun TodoItem(done: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (done) "✅" else "⬜", fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 13.sp,
            color = if (done) FitCutGreen else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MacroStat(label: String, current: Double, target: Double, color: Color) {
    val ratio = if (target > 0) (current / target).coerceIn(0.0, 1.2) else 0.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(56.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${current.toInt()} / ${target.toInt()} g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.weight(1f))
        Text("${(ratio * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
