package cn.petpet.app.ui.pethealth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.pethealth.HealthPlan
import cn.petpet.app.data.pethealth.PetHealthCalculator

/** 健康计划页：饮食 / 运动 / 疫苗 / 驱虫 / 体检（猫狗差异化）。 */
@Composable
fun HealthPlanScreen(viewModel: PetHealthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pet = viewModel.currentPet()
    if (pet == null) {
        HealthEmptyHint("请先添加宠物档案")
        return
    }
    val plan = viewModel.planOf(pet)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 饮食计划
        PlanCard(title = "🍽 饮食计划") {
            PlanRow("每日推荐热量", "${plan.dailyCalories.toInt()} kcal（RER ${plan.rer.toInt()}×系数）")
            PlanRow("喂食次数", "${plan.feedingTimes} 次/天")
            PlanRow("单次份量建议", "约 ${plan.mealSizeG.toInt()} g（按干粮估算）")
            if (plan.isSenior) PlanRow("老年配方", "已自动切换老年宠饮食配方", highlight = true)
            if (plan.isOverweight) PlanRow("减重模式", "热量已下调约 15%，循序渐进", highlight = true)
            Spacer(Modifier.height(4.dp))
            Text("⚠️ 食物禁忌", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PetHealthRed)
            Text(plan.foodBans.joinToString("、"), fontSize = 12.sp, color = PetHealthRed)
        }

        // 运动计划
        PlanCard(title = "🏃 运动计划") {
            PlanRow("每日建议时长", "${plan.exerciseMinutes} 分钟")
            Text(plan.exerciseText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // 疫苗计划
        PlanCard(title = "💉 疫苗计划") {
            plan.vaccinePlan.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.vaccineName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (item.dueDate.isBlank()) "待咨询" else "${item.dueDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.dueDate.isBlank()) PetHealthYellow else PetHealthBlueDark,
                        )
                    }
                    if (item.note.isNotBlank()) {
                        Text(item.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // 驱虫计划
        PlanCard(title = "🪱 驱虫计划") {
            plan.dewormPlan.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.type, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(
                        item.dueDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetHealthBlueDark,
                    )
                }
                if (item.note.isNotBlank()) {
                    Text(item.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // 体检计划
        PlanCard(title = "🏥 体检计划") {
            Text(plan.checkupNote, fontSize = 13.sp)
        }

        // 健康日历（未来 30 天待办）
        PlanCard(title = "📅 未来 30 天待办") {
            val calendar = buildCalendar(plan)
            calendar.forEach { (date, items) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(date, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PetHealthBlueDark, modifier = Modifier.width(90.dp))
                    Column {
                        items.forEach { Text("· $it", fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (calendar.isEmpty()) {
                Text("未来 30 天暂无待办", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            "所有计划基于 ${pet.breedName}（${if (pet.kind == cn.petpet.app.data.pethealth.PetKind.CAT) "猫" else "狗"}）的档案数据生成，仅供参考。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 汇总未来 30 天疫苗/驱虫待办。 */
private fun buildCalendar(plan: HealthPlan): List<Pair<String, List<String>>> {
    val today = java.time.LocalDate.now()
    val map = mutableMapOf<String, MutableList<String>>()
    plan.vaccinePlan.filter { it.dueDate.isNotBlank() }.forEach { v ->
        val date = runCatching { java.time.LocalDate.parse(v.dueDate) }.getOrNull() ?: return@forEach
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
        if (days in 0..30) map.getOrPut(date.toString()) { mutableListOf() }.add("疫苗：${v.vaccineName}")
    }
    plan.dewormPlan.filter { it.dueDate.isNotBlank() }.forEach { d ->
        val date = runCatching { java.time.LocalDate.parse(d.dueDate) }.getOrNull() ?: return@forEach
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
        if (days in 0..30) map.getOrPut(date.toString()) { mutableListOf() }.add("驱虫：${d.type}")
    }
    return map.toSortedMap().map { it.key to it.value }
}

@Composable
private fun PlanCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Medium,
            color = if (highlight) PetHealthYellow else MaterialTheme.colorScheme.onSurface,
        )
    }
}
