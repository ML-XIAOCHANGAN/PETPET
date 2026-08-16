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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.pethealth.HealthAlert
import cn.petpet.app.data.pethealth.HealthAlertRules
import cn.petpet.app.data.pethealth.SelfCheckLibrary
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

/** 预警页：自动预警 + 症状自查。 */
@Composable
fun HealthAlertScreen(viewModel: PetHealthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pet = viewModel.currentPet()
    if (pet == null) {
        HealthEmptyHint("请先添加宠物档案")
        return
    }

    var showSelfCheck by remember { mutableStateOf(false) }

    val alerts = state.data.alerts
        .filter { it.petId == pet.id }
        .sortedWith(compareByDescending<HealthAlert> { it.priority == "红" }.thenByDescending { it.date })
    val selfChecks = state.data.selfChecks
        .filter { it.petId == pet.id }
        .sortedByDescending { it.date }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PetHealthBlue.copy(alpha = 0.08f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🩺 症状自查", fontWeight = FontWeight.Bold)
                    Text(
                        "选择宠物出现的症状，获取可能原因与就医建议",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VoxelButton(text = "开始自查", onClick = { showSelfCheck = true })
                }
            }
        }

        item {
            Text("🚨 健康预警（${alerts.count { !it.resolved }} 未处理）", fontWeight = FontWeight.Bold)
        }
        if (alerts.isEmpty()) {
            item { Text("暂无预警记录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(alerts, key = { it.id }) { alert ->
                AlertCard(alert) { viewModel.resolveAlert(alert.id) }
            }
        }

        item {
            Text("📋 自查历史", fontWeight = FontWeight.Bold)
        }
        if (selfChecks.isEmpty()) {
            item { Text("暂无自查记录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(selfChecks.take(10), key = { it.id }) { check ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (check.level) { "红" -> "🔴"; "黄" -> "🟡"; else -> "🟢" },
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "症状：${check.symptoms.joinToString("、")}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(check.possibleCauses, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(check.advice, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                HealthAlertRules.disclaimer(),
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showSelfCheck) {
        SelfCheckDialog(viewModel) { showSelfCheck = false }
    }
}

@Composable
private fun AlertCard(alert: HealthAlert, onResolve: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.priority) {
                "红" -> PetHealthRed.copy(alpha = 0.08f)
                "黄" -> PetHealthYellow.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (alert.priority == "红") "🔴" else "🟡", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    alert.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alert.priority == "红") PetHealthRed else PetHealthYellow,
                    modifier = Modifier.weight(1f),
                )
                if (!alert.resolved) {
                    Text(
                        "已处理",
                        modifier = Modifier.clickable(onClick = onResolve),
                        fontSize = 11.sp,
                        color = PetHealthBlueDark,
                    )
                } else {
                    Text("✓", fontSize = 13.sp, color = PetHealthGreen)
                }
            }
            Text(alert.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${alert.date} · ${if (alert.source == "auto") "自动监测" else "症状自查"}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 症状自查弹窗。 */
@Composable
private fun SelfCheckDialog(viewModel: PetHealthViewModel, onDismiss: () -> Unit) {
    val pet = viewModel.currentPet()
    val allSymptoms = listOf(
        "呕吐", "腹泻", "咳嗽", "打喷嚏", "挠痒", "掉毛异常",
        "眼屎增多", "泪痕", "跛行", "抽搐", "呼吸困难", "萎靡",
    )
    var selected by remember { mutableStateOf(setOf<String>()) }
    var result by remember { mutableStateOf<cn.petpet.app.data.pethealth.SelfCheckResult?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (result == null) {
                    Text("症状自查（${pet?.name ?: ""}）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("请选择宠物出现的症状（可多选）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allSymptoms.chunked(2).forEach { rowSymptoms ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowSymptoms.forEach { s ->
                                    SymptomChip(s, s in selected) {
                                        selected = if (s in selected) selected - s else selected + s
                                    }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                        VoxelButton(
                            text = "评估",
                            onClick = {
                                if (selected.isNotEmpty()) {
                                    result = SelfCheckLibrary.evaluate(pet?.kind ?: cn.petpet.app.data.pethealth.PetKind.CAT, selected.toList())
                                }
                            },
                            compact = true,
                            enabled = selected.isNotEmpty(),
                        )
                    }
                } else {
                    val r = result!!
                    Text(
                        when (r.level) { "红" -> "🔴 红色 · 紧急就医"; "黄" -> "🟡 黄色 · 建议就医"; else -> "🟢 绿色 · 可观察" },
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = when (r.level) { "红" -> PetHealthRed; "黄" -> PetHealthYellow; else -> PetHealthGreen },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("可能原因", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(r.possibleCauses, fontSize = 13.sp)
                    Text("建议", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(r.advice, fontSize = 13.sp)
                    Text(
                        HealthAlertRules.disclaimer(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoxelButton(
                            text = "返回",
                            onClick = { result = null },
                            compact = true,
                            style = VoxelButtonStyle.NEUTRAL,
                        )
                        VoxelButton(
                            text = "保存结果",
                            onClick = {
                                viewModel.selfCheck(r.symptoms)
                                onDismiss()
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
private fun SymptomChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) PetHealthRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (selected) PetHealthRed else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp)
    }
}
