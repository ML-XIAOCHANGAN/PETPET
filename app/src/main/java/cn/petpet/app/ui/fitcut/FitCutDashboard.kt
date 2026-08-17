package cn.petpet.app.ui.fitcut

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.fitcut.FitCutCalculator
import cn.petpet.app.data.fitcut.FitCutTodo
import cn.petpet.app.ui.components.FitLineChart
import cn.petpet.app.ui.components.ProgressRing
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

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

    var showAddTodo by remember { mutableStateOf(false) }
    var editTodo by remember { mutableStateOf<FitCutTodo?>(null) }
    var deleteTodo by remember { mutableStateOf<FitCutTodo?>(null) }

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

        // 今日待办（用户自建，同健康助手体验）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("今日待办", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        "＋ 添加",
                        modifier = Modifier.clickable { showAddTodo = true },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FitCutGreen,
                    )
                }
                if (state.data.todos.isEmpty()) {
                    Text(
                        "还没有待办事项，点右上角「＋ 添加」记一条吧",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // 按日期分组：今天 / 明天 / 未来 / 历史
                    val grouped = state.data.todos.groupBy { todo ->
                        val days = runCatching {
                            java.time.temporal.ChronoUnit.DAYS.between(
                                java.time.LocalDate.parse(today),
                                java.time.LocalDate.parse(todo.date),
                            )
                        }.getOrDefault(0L).toInt()
                        when {
                            days == 0 -> "今天"
                            days == 1 -> "明天"
                            days > 1 -> "未来"
                            else -> "历史"
                        }
                    }
                    listOf("今天", "明天", "未来", "历史").forEach { group ->
                        val groupTodos = grouped[group] ?: return@forEach
                        if (groupTodos.isNotEmpty()) {
                            Text(
                                group,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FitCutGreen,
                            )
                            groupTodos.forEach { todo ->
                                FitCutTodoRow(
                                    todo = todo,
                                    onToggle = { viewModel.toggleTodo(todo.id) },
                                    onEdit = { editTodo = todo },
                                    onDelete = { deleteTodo = todo },
                                )
                            }
                        }
                    }
                }
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

    // 待办弹窗
    if (showAddTodo) {
        FitCutAddTodoDialog(viewModel) { showAddTodo = false }
    }
    editTodo?.let { todo ->
        FitCutEditTodoDialog(
            viewModel = viewModel,
            todo = todo,
            onDismiss = { editTodo = null },
        )
    }
    deleteTodo?.let { todo ->
        FitCutDeleteTodoDialog(
            viewModel = viewModel,
            todo = todo,
            onDismiss = { deleteTodo = null },
        )
    }
}

@Composable
private fun FitCutTodoRow(
    todo: FitCutTodo,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // 勾选完成 → 透明度从 100% 渐变到 70%（动画过渡）
    val textAlpha by animateFloatAsState(
        targetValue = if (todo.done) 0.70f else 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "待办完成透明度",
    )
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = todo.done,
            onCheckedChange = { onToggle() },
        )
        Column(Modifier.weight(1f)) {
            Text(
                todo.text,
                fontSize = 13.sp,
                modifier = Modifier.graphicsLayer { alpha = textAlpha },
                textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                todo.date,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        // ⋮ 菜单
        Box {
            Text(
                "⋮",
                modifier = Modifier
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("编辑", fontSize = 13.sp) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除", fontSize = 13.sp, color = FitCutOrange) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/** 添加待办弹窗（含日期选择，默认今天）。 */
@Composable
private fun FitCutAddTodoDialog(viewModel: FitCutViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(viewModel.today()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("添加待办", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = text, onValueChange = { if (it.length <= 30) text = it },
                    label = { Text("待办内容（如：跑步、喝水 8 杯）*") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                FitCutDateField(date = date, onDateChange = { date = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "添加",
                        onClick = {
                            if (text.isNotBlank()) {
                                viewModel.addTodo(text, date)
                                onDismiss()
                            }
                        },
                        compact = true,
                        enabled = text.isNotBlank(),
                    )
                }
            }
        }
    }
}

/** 编辑待办弹窗（文本 + 日期）。 */
@Composable
private fun FitCutEditTodoDialog(
    viewModel: FitCutViewModel,
    todo: FitCutTodo,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(todo.text) }
    var date by remember { mutableStateOf(todo.date) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("编辑待办", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = text, onValueChange = { if (it.length <= 30) text = it },
                    label = { Text("待办内容 *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                FitCutDateField(date = date, onDateChange = { date = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "保存",
                        onClick = {
                            if (text.isNotBlank()) {
                                viewModel.updateTodo(todo.id, text, date)
                                onDismiss()
                            }
                        },
                        compact = true,
                        enabled = text.isNotBlank(),
                    )
                }
            }
        }
    }
}

/** 删除待办确认弹窗。 */
@Composable
private fun FitCutDeleteTodoDialog(
    viewModel: FitCutViewModel,
    todo: FitCutTodo,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("删除待办", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("确定删除「${todo.text}」吗？删除后不可恢复。", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "删除",
                        onClick = {
                            viewModel.removeTodo(todo.id)
                            onDismiss()
                        },
                        compact = true,
                    )
                }
            }
        }
    }
}

/** 日期选择行：显示日期文本，点击弹出中文日期选择器。 */
@Composable
private fun FitCutDateField(date: String, onDateChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📅", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(date, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text("选择日期", fontSize = 12.sp, color = FitCutGreen)
    }
    if (showPicker) {
        FitCutDatePickerDialog(
            initialDate = runCatching { java.time.LocalDate.parse(date) }.getOrDefault(java.time.LocalDate.now()),
            onDismiss = { showPicker = false },
            onConfirm = { onDateChange(it.toString()); showPicker = false },
        )
    }
}

/** 中文年月日选择器（自绘，不依赖系统 locale）。 */
@Composable
private fun FitCutDatePickerDialog(
    initialDate: java.time.LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalDate) -> Unit,
) {
    val now = java.time.LocalDate.now()
    var year by remember { mutableStateOf(initialDate.year) }
    var month by remember { mutableStateOf(initialDate.monthValue) }
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }

    val monthNames = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("选择日期", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", modifier = Modifier.clickable { year -= 1 }, fontSize = 20.sp, color = FitCutGreen)
                    Spacer(Modifier.weight(1f))
                    Text("$year 年", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("›", modifier = Modifier.clickable { year += 1 }, fontSize = 20.sp, color = FitCutGreen)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", modifier = Modifier.clickable {
                        month = if (month == 1) { year -= 1; 12 } else month - 1
                    }, fontSize = 20.sp, color = FitCutGreen)
                    Spacer(Modifier.weight(1f))
                    Text(monthNames[month - 1], fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("›", modifier = Modifier.clickable {
                        month = if (month == 12) { year += 1; 1 } else month + 1
                    }, fontSize = 20.sp, color = FitCutGreen)
                }

                Row(Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { w ->
                        Text(
                            w,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val firstDay = java.time.LocalDate.of(year, month, 1)
                val daysInMonth = firstDay.lengthOfMonth()
                val leading = firstDay.dayOfWeek.value % 7
                val cells = List(leading) { 0 } + (1..daysInMonth)
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            if (d == 0) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val isToday = d == now.dayOfMonth && month == now.monthValue && year == now.year
                                val selected = d == day && month == initialDate.monthValue && year == initialDate.year
                                Surface(
                                    onClick = { day = d },
                                    shape = RoundedCornerShape(50),
                                    color = when {
                                        selected -> FitCutGreen
                                        isToday -> FitCutGreen.copy(alpha = 0.15f)
                                        else -> androidx.compose.ui.graphics.Color.Transparent
                                    },
                                    contentColor = when {
                                        selected -> androidx.compose.ui.graphics.Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("$d", modifier = Modifier.padding(vertical = 8.dp), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "确定",
                        onClick = {
                            onConfirm(
                                runCatching { java.time.LocalDate.of(year, month, day) }
                                    .getOrDefault(initialDate),
                            )
                        },
                        compact = true,
                    )
                }
            }
        }
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
