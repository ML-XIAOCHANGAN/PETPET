package cn.petpet.app.ui.fitcut

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.fitcut.FitCutFoodLibrary
import cn.petpet.app.data.fitcut.FoodItem
import cn.petpet.app.data.fitcut.MealRecord
import cn.petpet.app.data.fitcut.MealType
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

/** 饮食记录页：三餐分组 + 当日汇总。 */
@Composable
fun FitCutDietScreen(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plan ?: return
    val today = viewModel.today()
    val todayMeals = state.data.mealRecords.filter { it.date == today }
    val eaten = todayMeals.sumOf { it.calories }
    val remaining = (plan.recommendedCalories - eaten).coerceAtLeast(0.0)

    var addFor by remember { mutableStateOf<MealType?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 今日汇总卡
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("今日摄入 ${eaten.toInt()} / ${plan.recommendedCalories.toInt()} kcal", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        if (remaining > 0) "剩余 ${remaining.toInt()} kcal" else "已达目标 🎉",
                        fontSize = 13.sp,
                        color = if (remaining > 0) FitCutGreen else FitCutOrange,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SummaryChip("蛋白质", todayMeals.sumOf { it.protein }.toInt(), plan.targetProteinG.toInt(), FitCutGreen)
                        SummaryChip("碳水", todayMeals.sumOf { it.carb }.toInt(), plan.targetCarbG.toInt(), Color(0xFF3B82F6))
                        SummaryChip("脂肪", todayMeals.sumOf { it.fat }.toInt(), plan.targetFatG.toInt(), FitCutOrange)
                    }
                }
            }
        }

        // 各餐分组
        MealType.entries.forEach { mealType ->
            val meals = todayMeals.filter { it.mealType == mealType }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${mealType.emoji} ${mealType.label}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${meals.sumOf { it.calories }.toInt()} kcal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    VoxelButton(
                        text = "+ 添加",
                        onClick = { addFor = mealType },
                        compact = true,
                    )
                }
            }
            if (meals.isEmpty()) {
                item {
                    Text(
                        "还没有记录",
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(meals, key = { it.id }) { meal ->
                    MealRow(meal) { viewModel.removeMeal(meal.id) }
                }
            }
        }
    }

    addFor?.let { mealType ->
        AddFoodDialog(
            mealType = mealType,
            customFoods = state.data.customFoods,
            onDismiss = { addFor = null },
            onAdd = { food, grams -> viewModel.addMeal(mealType, food, grams); addFor = null },
            onAddCustom = { name, cal, p, c, f, grams ->
                viewModel.addCustomFoodWithMeal(mealType, name, cal, p, c, f, grams)
                addFor = null
            },
        )
    }
}

@Composable
private fun SummaryChip(label: String, current: Int, target: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "$current/${target}g",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun MealRow(meal: MealRecord, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(meal.foodName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${meal.grams.toInt()}g · 蛋白${meal.protein.toInt()}g 碳水${meal.carb.toInt()}g 脂肪${meal.fat.toInt()}g",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${meal.calories.toInt()} kcal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FitCutGreen)
            Spacer(Modifier.width(10.dp))
            Text(
                "删除",
                modifier = Modifier.clickable(onClick = onRemove),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** 添加食物弹窗：搜索内置库 / 自定义。 */
@Composable
private fun AddFoodDialog(
    mealType: MealType,
    customFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onAdd: (FoodItem, Double) -> Unit,
    onAddCustom: (String, Double, Double, Double, Double, Double) -> Unit,
) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var grams by rememberSaveable { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${mealType.emoji} 添加${mealType.label}", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                if (showCustom) {
                    CustomFoodForm(
                        onCancel = { showCustom = false },
                        onSubmit = onAddCustom,
                    )
                } else if (selectedFood == null) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("搜索食物（如：鸡胸肉、米饭）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    val results = remember(keyword, customFoods) {
                        FitCutFoodLibrary.search(keyword) + customFoods.filter { f ->
                            keyword.isBlank() || f.name.contains(keyword.trim(), ignoreCase = true)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(results.take(30), key = { it.id }) { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFood = food }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${food.caloriesPer100g.toInt()} kcal/100g · 蛋白${food.proteinPer100g.toInt()}g",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (food.isCustom) {
                                    Text("自定义", fontSize = 10.sp, color = FitCutOrange)
                                }
                            }
                        }
                        item {
                            VoxelButton(
                                text = "＋ 自定义食物…",
                                onClick = { showCustom = true },
                                compact = true,
                                style = VoxelButtonStyle.SECONDARY,
                            )
                        }
                    }
                } else {
                    // 已选食物：输入克数
                    val food = selectedFood!!
                    Text("已选：${food.name}（${food.caloriesPer100g.toInt()} kcal/100g）", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { v -> grams = v.filter { it.isDigit() || it == '.' } },
                        label = { Text("食用量（克）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    val g = grams.toDoubleOrNull() ?: 0.0
                    val est = food.caloriesPer100g * g / 100.0
                    Text("约 ${est.toInt()} kcal", fontSize = 13.sp, color = FitCutGreen, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoxelButton(text = "返回", onClick = { selectedFood = null }, compact = true, style = VoxelButtonStyle.NEUTRAL)
                        VoxelButton(
                            text = "添加",
                            onClick = { if (g > 0) onAdd(food, g) },
                            compact = true,
                            enabled = g > 0,
                        )
                    }
                }
            }
        }
    }
}

/** 自定义食物表单。 */
@Composable
private fun CustomFoodForm(
    onCancel: () -> Unit,
    onSubmit: (String, Double, Double, Double, Double, Double) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var cal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carb by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("100") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("自定义食物（每 100g 营养）", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("食物名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = cal, onValueChange = { cal = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("热量 kcal/100g") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = protein, onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("蛋白 g") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = carb, onValueChange = { carb = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("碳水 g") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = fat, onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("脂肪 g") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        OutlinedTextField(value = grams, onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("本次食用量（克）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VoxelButton(text = "取消", onClick = onCancel, compact = true, style = VoxelButtonStyle.NEUTRAL)
            VoxelButton(
                text = "保存并添加",
                onClick = {
                    val n = name.trim()
                    val c = cal.toDoubleOrNull() ?: 0.0
                    val p = protein.toDoubleOrNull() ?: 0.0
                    val cb = carb.toDoubleOrNull() ?: 0.0
                    val f = fat.toDoubleOrNull() ?: 0.0
                    val g = grams.toDoubleOrNull() ?: 0.0
                    if (n.isNotEmpty() && c > 0 && g > 0) onSubmit(n, c, p, cb, f, g)
                },
                compact = true,
                enabled = name.isNotBlank() && (cal.toDoubleOrNull() ?: 0.0) > 0 && (grams.toDoubleOrNull() ?: 0.0) > 0,
            )
        }
    }
}
