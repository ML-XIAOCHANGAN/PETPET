package cn.petpet.app.ui.pethealth

import androidx.compose.foundation.clickable
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
import cn.petpet.app.data.pethealth.HealthAlert
import cn.petpet.app.data.pethealth.PetHealthCalculator
import cn.petpet.app.ui.components.ProgressRing

/** 概览页：宠物健康卡片 + 今日待办 + 健康评分 + 异常提醒 + 计划速览。 */
@Composable
fun HealthDashboard(viewModel: PetHealthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pet = viewModel.currentPet()

    if (pet == null) {
        HealthEmptyHint("还没有宠物档案\n点击上方「添加」创建第一份健康档案")
        return
    }

    val plan = viewModel.planOf(pet)
    val body = PetHealthCalculator.bodyCondition(pet)
    val score = viewModel.healthScore(pet.id)
    val todos = viewModel.todayTodos(pet)
    val daysIdle = viewModel.daysSinceLastRecord(pet.id)

    val today = viewModel.today()
    val meals = state.data.meals.filter { it.petId == pet.id && it.date == today }
    val waters = state.data.waters.filter { it.petId == pet.id && it.date == today }
    val weights = state.data.weights.filter { it.petId == pet.id }.sortedBy { it.date }

    val openAlerts = state.data.alerts
        .filter { it.petId == pet.id && !it.resolved }
        .sortedByDescending { it.date }

    val redCount = openAlerts.count { it.priority == "红" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 宠物健康卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (pet.kind == cn.petpet.app.data.pethealth.PetKind.CAT) "🐱" else "🐶", fontSize = 34.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(pet.name, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(
                        "${pet.breedName} · ${PetHealthCalculator.ageText(pet.birthDate)} · ${pet.weightKg}kg",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "体型：${body.level}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (body.level) {
                            "偏瘦" -> PetHealthYellow
                            "标准" -> PetHealthGreen
                            "偏胖" -> PetHealthYellow
                            else -> PetHealthRed
                        },
                    )
                }
                // 健康评分环
                ProgressRing(
                    progress = score / 100f,
                    color = when {
                        score >= 80 -> PetHealthGreen
                        score >= 50 -> PetHealthYellow
                        else -> PetHealthRed
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    size = 64f,
                    strokeWidth = 7f,
                    centerText = "$score",
                    subText = "本周评分",
                )
            }
        }

        // 今日待办
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("今日待办", fontWeight = FontWeight.Bold)
                todos.forEach { todo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☑", fontSize = 13.sp, color = PetHealthBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(todo, fontSize = 13.sp)
                    }
                }
                if (daysIdle > 0) {
                    Text(
                        "${daysIdle} 天未记录啦，今天的毛孩状态怎么样？",
                        fontSize = 12.sp,
                        color = PetHealthYellow,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // 异常提醒（红色优先）
        if (openAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (redCount > 0) PetHealthRed.copy(alpha = 0.08f) else PetHealthYellow.copy(alpha = 0.08f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "🚨 健康提醒（${openAlerts.size} 条未处理）",
                        fontWeight = FontWeight.Bold,
                        color = if (redCount > 0) PetHealthRed else PetHealthYellow,
                    )
                    openAlerts.take(3).forEach { alert ->
                        AlertRow(alert) { viewModel.resolveAlert(alert.id) }
                    }
                    if (openAlerts.size > 3) {
                        Text("… 还有 ${openAlerts.size - 3} 条，见「预警」页", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✅", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("暂无健康异常，继续保持记录吧", fontSize = 13.sp)
                }
            }
        }

        // 今日饮食/饮水/体重速览
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("今日记录", fontWeight = FontWeight.Bold)
                val eaten = meals.sumOf { it.calories }
                Text(
                    "🍽 已摄入 ${eaten.toInt()} / 推荐 ${plan.dailyCalories.toInt()} kcal",
                    fontSize = 13.sp,
                )
                val waterMl = waters.sumOf { it.ml }
                val waterTarget = if (pet.kind == cn.petpet.app.data.pethealth.PetKind.CAT) {
                    (pet.weightKg * 50).toInt()
                } else {
                    (pet.weightKg * 40).toInt()
                }
                Text(
                    "💧 饮水 ${waterMl} / 推荐 ${waterTarget} ml",
                    fontSize = 13.sp,
                    color = if (waterMl > 0 && waterMl < waterTarget * 0.8) PetHealthYellow else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "⚖ 最新体重 ${weights.lastOrNull()?.weightKg ?: pet.weightKg} kg",
                    fontSize = 13.sp,
                )
            }
        }

        // 体型建议
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("体型评估建议", fontWeight = FontWeight.Bold)
                Text(body.advice, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AlertRow(alert: HealthAlert, onResolve: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (alert.priority == "红") "🔴" else "🟡",
            fontSize = 14.sp,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                alert.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (alert.priority == "红") PetHealthRed else PetHealthYellow,
            )
            Text(alert.detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        Text(
            "已处理",
            modifier = Modifier.clickable(onClick = onResolve),
            fontSize = 11.sp,
            color = PetHealthBlueDark,
        )
    }
}
