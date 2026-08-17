package cn.petpet.app.ui.pethealth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.pethealth.BreedOption
import cn.petpet.app.data.pethealth.HealthPetProfile
import cn.petpet.app.data.pethealth.PetBreedLibrary
import cn.petpet.app.data.pethealth.PetGender
import cn.petpet.app.data.pethealth.PetKind
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

/** 模块主色调（温和健康蓝绿）。 */
val PetHealthBlue = Color(0xFF0EA5E9)
val PetHealthBlueDark = Color(0xFF0369A1)
val PetHealthRed = Color(0xFFEF4444)
val PetHealthYellow = Color(0xFFF59E0B)
val PetHealthGreen = Color(0xFF22C55E)

private data class HealthTab(val label: String, val icon: String)

private val healthTabs = listOf(
    HealthTab("概览", "🏠"),
    HealthTab("记录", "📝"),
    HealthTab("计划", "📅"),
    HealthTab("预警", "🚨"),
)

/**
 * Petpet 健康助手主界面：宠物切换栏 + 底部 3 tab。
 */
@Composable
fun PetHealthScreen(
    viewModel: PetHealthViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🐾 Petpet 健康助手", fontSize = 20.sp, fontWeight = FontWeight.Black, color = PetHealthBlueDark)
            Spacer(Modifier.weight(1f))
            VoxelButton(text = "关闭", onClick = onClose, compact = true, style = VoxelButtonStyle.NEUTRAL)
        }

        // 宠物切换栏（空态也显示，保留「添加」入口）
        PetSwitcher(
            pets = state.data.pets,
            currentId = state.currentPetId,
            onSelect = viewModel::selectPet,
            onAdd = viewModel::showAddPet,
        )

        // 免责声明横幅
        Text(
            "健康建议仅供参考，不能替代专业兽医诊断",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> HealthDashboard(viewModel)
                1 -> HealthRecordsScreen(viewModel)
                2 -> HealthPlanScreen(viewModel)
                3 -> HealthAlertScreen(viewModel)
            }
        }

        // 底部 tab
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                healthTabs.forEachIndexed { index, t ->
                    val selected = tab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tab = index }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(t.icon, fontSize = 18.sp)
                        Text(
                            t.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) PetHealthBlueDark else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (state.showAddPet) {
        AddPetDialog(viewModel = viewModel)
    }
}

/** 宠物切换栏（头像横向滚动）。 */
@Composable
private fun PetSwitcher(
    pets: List<HealthPetProfile>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pets.forEach { pet ->
            val selected = pet.id == currentId
            val emoji = if (pet.kind == PetKind.CAT) "🐱" else "🐶"
            Column(
                modifier = Modifier
                    .clickable { onSelect(pet.id) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = if (selected) PetHealthBlue else PetHealthBlue.copy(alpha = 0.15f),
                    border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, PetHealthBlueDark) else null,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (pet.avatarEmoji.isNotBlank()) pet.avatarEmoji else emoji, fontSize = 20.sp)
                    }
                }
                Text(
                    pet.name,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) PetHealthBlueDark else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // 添加宠物
        Column(
            modifier = Modifier
                .clickable(onClick = onAdd)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("＋", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("添加", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 添加宠物弹窗：先选择来源（当前电子宠物 / 自定义），自定义才展开表单。 */
@Composable
private fun AddPetDialog(viewModel: PetHealthViewModel) {
    var mode by remember { mutableStateOf(0) } // 0=选择来源, 1=自定义表单
    var kind by remember { mutableStateOf(PetKind.CAT) }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(PetGender.MALE) }
    var neutered by remember { mutableStateOf(false) }
    var birthDate by remember { mutableStateOf("") }
    var breedId by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedBans by remember { mutableStateOf(setOf<String>()) }
    var customBan by remember { mutableStateOf("") }

    val breedOptions = remember(kind) { PetBreedLibrary.byKind(kind) }

    Dialog(onDismissRequest = viewModel::hideAddPet) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (mode == 0) {
                // ── 选择来源 ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("添加宠物健康档案", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("选择要添加的宠物", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // 添加当前电子宠物（宿主已领养）
                    Surface(
                        onClick = viewModel::addHostPet,
                        shape = RoundedCornerShape(16.dp),
                        color = PetHealthBlue.copy(alpha = 0.10f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🐱", fontSize = 26.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("添加当前电子宠物", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PetHealthBlueDark)
                                Text("把我在 My Pet 领养的宠物加入健康管理", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("›", fontSize = 20.sp, color = PetHealthBlueDark)
                        }
                    }

                    // 自定义添加
                    Surface(
                        onClick = { mode = 1 },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🐾", fontSize = 26.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("自定义添加宠物", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("手动填写品种、生日、体重等信息", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    VoxelButton(text = "取消", onClick = viewModel::hideAddPet, compact = true, style = VoxelButtonStyle.NEUTRAL)
                }
            } else {
                // ── 自定义表单 ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("添加宠物健康档案", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                // 类型选择
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KindChip("🐱 猫咪", kind == PetKind.CAT) { kind = PetKind.CAT }
                    KindChip("🐶 狗狗", kind == PetKind.DOG) { kind = PetKind.DOG }
                }

                OutlinedTextField(
                    value = name, onValueChange = { if (it.length <= 12) name = it },
                    label = { Text("名字 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderChip("公", gender == PetGender.MALE) { gender = PetGender.MALE }
                    GenderChip("母", gender == PetGender.FEMALE) { gender = PetGender.FEMALE }
                    Spacer(Modifier.weight(1f))
                    Text("已绝育", fontSize = 13.sp)
                    Text(
                        if (neutered) "✅" else "⬜",
                        modifier = Modifier.clickable { neutered = !neutered },
                    )
                }

                OutlinedTextField(
                    value = birthDate, onValueChange = { birthDate = it },
                    label = { Text("出生日期（yyyy-MM-dd）*") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )

                // 品种选择（猫：列表；狗：按体型分组）
                Text("品种 *", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val options = breedOptions
                var selectedBreed by remember(kind) { mutableStateOf(options.first().id) }
                if (kind == PetKind.DOG) {
                    PetBreedLibrary.dogsBySize().forEach { (size, sizeBreeds) ->
                        Text("$size 型", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            sizeBreeds.forEach { b ->
                                BreedChip(b.name, selectedBreed == b.id) { selectedBreed = b.id }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        options.forEach { b ->
                            BreedChip(b.name, selectedBreed == b.id) { selectedBreed = b.id }
                        }
                    }
                }

                OutlinedTextField(
                    value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("体重（kg）*") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )

                // 食物禁忌
                Text("食物禁忌（可多选）", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val bans = when (kind) {
                        PetKind.CAT -> listOf("洋葱/大蒜", "巧克力", "葡萄/葡萄干", "百合", "木糖醇")
                        PetKind.DOG -> listOf("洋葱/大蒜", "巧克力", "葡萄/葡萄干", "木糖醇", "牛油果")
                    }
                    bans.forEach { b ->
                        BreedChip(b, b in selectedBans) {
                            selectedBans = if (b in selectedBans) selectedBans - b else selectedBans + b
                        }
                    }
                }
                OutlinedTextField(
                    value = customBan, onValueChange = { customBan = it },
                    label = { Text("自定义禁忌（选填）") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "上一步", onClick = { mode = 0 }, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(text = "取消", onClick = viewModel::hideAddPet, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "保存档案",
                        onClick = {
                            val bans = selectedBans + customBan.trim().takeIf { it.isNotEmpty() }.let { listOfNotNull(it) }
                            viewModel.addPet(kind, name, gender, neutered, birthDate.trim(), selectedBreed, weight.toDoubleOrNull() ?: 0.0, bans.toList())
                        },
                        compact = true,
                        enabled = name.isNotBlank() && birthDate.isNotBlank() && (weight.toDoubleOrNull() ?: 0.0) > 0,
                    )
                }
                } // ── 自定义表单 Column 结束 ──
            } // else 分支结束
        }
    }
}

private fun optionsContains(options: List<BreedOption>, id: String): Boolean =
    options.any { it.id == id }

@Composable
private fun KindChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PetHealthBlue else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 13.sp)
    }
}

@Composable
private fun GenderChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) PetHealthBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) PetHealthBlueDark else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 13.sp)
    }
}

@Composable
private fun BreedChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) PetHealthBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (selected) PetHealthBlueDark else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp)
    }
}

/** 空态提示。 */
@Composable
fun HealthEmptyHint(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🐾", fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
