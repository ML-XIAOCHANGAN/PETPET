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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.pethealth.HealthMealRecord
import cn.petpet.app.data.pethealth.PetHealthCalculator
import cn.petpet.app.data.pethealth.PetKind
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

/** 记录页：饮食 / 饮水 / 排泄 / 体重 四类记录。 */
@Composable
fun HealthRecordsScreen(viewModel: PetHealthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pet = viewModel.currentPet()
    if (pet == null) {
        HealthEmptyHint("请先添加宠物档案")
        return
    }
    val plan = viewModel.planOf(pet)
    val today = viewModel.today()

    var recordType by remember { mutableStateOf("饮食") }
    val types = listOf("饮食", "饮水", "排泄", "体重")
    var addMealDialog by remember { mutableStateOf(false) }
    var addWaterDialog by remember { mutableStateOf(false) }
    var addPoopDialog by remember { mutableStateOf(false) }
    var addWeightDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 类型切换
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                types.forEach { t ->
                    RecordTypeChip(t, recordType == t) { recordType = t }
                }
            }
        }

        when (recordType) {
            "饮食" -> {
                val meals = state.data.meals.filter { it.petId == pet.id && it.date == today }
                val eaten = meals.sumOf { it.calories }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "今日摄入 ${eaten.toInt()} / ${plan.dailyCalories.toInt()} kcal",
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                "推荐 ${plan.feedingTimes} 次喂食 · 单次约 ${plan.mealSizeG.toInt()}g",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (plan.foodBans.isNotEmpty()) {
                                Text(
                                    "⚠️ 禁忌：${plan.foodBans.joinToString("、")}",
                                    fontSize = 11.sp,
                                    color = PetHealthRed,
                                )
                            }
                        }
                    }
                }
                item {
                    VoxelButton(text = "＋ 记录一餐", onClick = { addMealDialog = true }, compact = false)
                }
                if (meals.isEmpty()) {
                    item { Text("今天还没有饮食记录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(meals, key = { it.id }) { meal -> MealRow(meal) }
                }
            }
            "饮水" -> {
                val waters = state.data.waters.filter { it.petId == pet.id && it.date == today }
                val total = waters.sumOf { it.ml }
                val target = if (pet.kind == PetKind.CAT) (pet.weightKg * 50).toInt() else (pet.weightKg * 40).toInt()
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("今日饮水 $total / $target ml", fontWeight = FontWeight.Black)
                            Text(
                                if (pet.kind == PetKind.CAT && total < target * 0.8) {
                                    "💡 猫咪易患泌尿疾病，饮水量偏低，建议增加饮水"
                                } else {
                                    "保持饮水记录，观察趋势"
                                },
                                fontSize = 12.sp,
                                color = if (total < target * 0.8) PetHealthYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    VoxelButton(text = "＋ 记录饮水", onClick = { addWaterDialog = true })
                }
                if (waters.isEmpty()) {
                    item { Text("今天还没有饮水记录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(waters, key = { it.id }) { w ->
                        Text("💧 ${w.ml} ml", modifier = Modifier.padding(vertical = 2.dp), fontSize = 13.sp)
                    }
                }
            }
            "排泄" -> {
                val poops = state.data.poops.filter { it.petId == pet.id && it.date == today }
                item {
                    VoxelButton(text = "＋ 记录排泄", onClick = { addPoopDialog = true })
                }
                if (poops.isEmpty()) {
                    item { Text("今天还没有排泄记录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(poops, key = { it.id }) { p ->
                        Text(
                            "${if (p.type == "排便") "💩" else "💧"} ${p.type} ×${p.count}" +
                                (if (p.bristol > 0) " · 布里斯托 ${p.bristol} 型" else "") +
                                (if (p.urineAmount.isNotBlank()) " · 尿量${p.urineAmount}" else "") +
                                (if (p.hasBlood) " · ⚠️带血" else "") +
                                (if (p.abnormal) " · ⚠️异常" else ""),
                            modifier = Modifier.padding(vertical = 3.dp),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            "体重" -> {
                val weights = state.data.weights.filter { it.petId == pet.id }.sortedByDescending { it.date }
                val body = PetHealthCalculator.bodyCondition(pet)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("当前 ${weights.firstOrNull()?.weightKg ?: pet.weightKg} kg · ${body.level}", fontWeight = FontWeight.Black)
                            Text(body.advice, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("建议每周固定时间称重", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    VoxelButton(text = "＋ 记录体重", onClick = { addWeightDialog = true })
                }
                items(weights.take(10), key = { it.id }) { w ->
                    Text(
                        "⚖ ${w.weightKg} kg · ${w.date}",
                        modifier = Modifier.padding(vertical = 3.dp),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }

    // 弹窗
    if (addMealDialog) AddMealDialog(viewModel) { addMealDialog = false }
    if (addWaterDialog) AddWaterDialog(viewModel) { addWaterDialog = false }
    if (addPoopDialog) AddPoopDialog(viewModel) { addPoopDialog = false }
    if (addWeightDialog) AddWeightDialog(viewModel) { addWeightDialog = false }
}

@Composable
private fun RecordTypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) PetHealthBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (selected) Color_White else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 12.sp)
    }
}

private val Color_White = androidx.compose.ui.graphics.Color.White

@Composable
private fun MealRow(meal: HealthMealRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(meal.foodName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${meal.grams.toInt()}g · ${meal.calories.toInt()} kcal · ${meal.appetite}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                when (meal.appetite) {
                    "拒食" -> "🔴"
                    "食欲下降" -> "🟡"
                    else -> "🟢"
                },
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun AddMealDialog(viewModel: PetHealthViewModel, onDismiss: () -> Unit) {
    var food by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("100") }
    var appetite by rememberSaveable { mutableStateOf("正常") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("记录一餐", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = food, onValueChange = { food = it },
                    label = { Text("食物名称 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = grams, onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("份量（g）") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Text("食欲状态", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("正常", "食欲下降", "拒食").forEach { a ->
                        RecordTypeChip(a, appetite == a) { appetite = a }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "保存",
                        onClick = {
                            val g = grams.toDoubleOrNull() ?: 0.0
                            if (food.isNotBlank() && g > 0) {
                                viewModel.addMeal(food.trim(), g, (g * 3.5).toInt().toDouble(), appetite)
                                onDismiss()
                            }
                        },
                        compact = true,
                        enabled = food.isNotBlank() && (grams.toDoubleOrNull() ?: 0.0) > 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWaterDialog(viewModel: PetHealthViewModel, onDismiss: () -> Unit) {
    var ml by rememberSaveable { mutableStateOf("200") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("记录饮水", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("参考：常见水碗约 200-300ml / 碗", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = ml, onValueChange = { ml = it.filter(Char::isDigit) },
                    label = { Text("饮水量（ml）*") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "保存",
                        onClick = { ml.toIntOrNull()?.takeIf { it > 0 }?.let { viewModel.addWater(it); onDismiss() } },
                        compact = true,
                        enabled = (ml.toIntOrNull() ?: 0) > 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPoopDialog(viewModel: PetHealthViewModel, onDismiss: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("排便") }
    var count by rememberSaveable { mutableStateOf("1") }
    var bristol by rememberSaveable { mutableStateOf(4) }
    var urineAmount by rememberSaveable { mutableStateOf("正常") }
    var hasBlood by rememberSaveable { mutableStateOf(false) }
    var abnormal by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("记录排泄", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("排便", "排尿").forEach { t ->
                        RecordTypeChip(t, type == t) {
                            type = t
                            if (t == "排尿") bristol = 0
                        }
                    }
                }
                OutlinedTextField(
                    value = count, onValueChange = { count = it.filter(Char::isDigit) },
                    label = { Text("次数 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                if (type == "排便") {
                    Text("布里斯托分级（1 便秘 → 7 腹泻）", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // 1-7 分两行展示，避免 6/7 溢出屏幕
                    (1..7).toList().chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            row.forEach { b ->
                                Surface(
                                    onClick = { bristol = b },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (bristol == b) PetHealthBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    contentColor = if (bristol == b) Color_White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        "$b",
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("尿量", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("多", "正常", "少").forEach { u ->
                            RecordTypeChip(u, urineAmount == u) { urineAmount = u }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("带血", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Checkbox(checked = hasBlood, onCheckedChange = { hasBlood = it })
                    Spacer(Modifier.width(16.dp))
                    Text("异常标注", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Checkbox(checked = abnormal, onCheckedChange = { abnormal = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "保存",
                        onClick = {
                            count.toIntOrNull()?.takeIf { it > 0 }?.let {
                                viewModel.addPoop(type, it, bristol, urineAmount, hasBlood, false, abnormal)
                                onDismiss()
                            }
                        },
                        compact = true,
                        enabled = (count.toIntOrNull() ?: 0) > 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWeightDialog(viewModel: PetHealthViewModel, onDismiss: () -> Unit) {
    var weight by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("记录体重", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("体重（kg）*") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "保存",
                        onClick = { weight.toDoubleOrNull()?.takeIf { it in 0.5..100.0 }?.let { viewModel.addWeight(it); onDismiss() } },
                        compact = true,
                        enabled = (weight.toDoubleOrNull() ?: 0.0) in 0.5..100.0,
                    )
                }
            }
        }
    }
}
