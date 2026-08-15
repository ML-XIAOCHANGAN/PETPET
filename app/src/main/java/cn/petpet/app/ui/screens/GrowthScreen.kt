package cn.petpet.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.petpet.app.R
import cn.petpet.app.model.DiaryEntry
import cn.petpet.app.model.PetBreeds
import cn.petpet.app.model.PetProfile
import cn.petpet.app.model.PetType
import cn.petpet.app.ui.components.InteractiveBlackCatAvatar
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle
import cn.petpet.app.ui.components.drawHeartFilledIcon
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun GrowthScreen(
    pet: PetProfile,
    diaryEntries: List<DiaryEntry>,
    onAddDiary: (String, String) -> Unit,
) {
    var showComposer by rememberSaveable { mutableStateOf(false) }
    val isDog = PetBreeds.byId(pet.breedId)?.type == PetType.DOG

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "成长",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "收藏相处的片刻，而不是催促彼此长大。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            MyProfileCard(pet)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isDog) "小狗日记" else "小猫日记",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "共 ${diaryEntries.size} 篇相处记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VoxelButton(
                    text = "写日记",
                    onClick = { showComposer = true },
                    compact = true,
                )
            }
        }

        if (diaryEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(if (isDog) R.drawable.dog_paw else R.drawable.cat_paw),
                            contentDescription = if (isDog) "小狗爪印" else "小猫爪印",
                            modifier = Modifier.size(112.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            text = "还没有日记。\n第一次见面、一次聊天或普通的好心情，都值得被记下来。",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(diaryEntries, key = DiaryEntry::id) { entry ->
                DiaryCard(entry)
            }
        }
    }

    if (showComposer) {
        DiaryComposerDialog(
            onDismiss = { showComposer = false },
            onSave = { title, content ->
                onAddDiary(title, content)
                showComposer = false
            },
        )
    }
}

@Composable
private fun MyProfileCard(pet: PetProfile) {
    val togetherDays = remember(pet.adoptedAt) {
        runCatching {
            ChronoUnit.DAYS.between(LocalDate.parse(pet.adoptedAt), LocalDate.now()) + 1
        }.getOrDefault(1)
    }
    val isDog = PetBreeds.byId(pet.breedId)?.type == PetType.DOG

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("我的", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                InteractiveBlackCatAvatar(
                    onInteract = {},
                    breedId = pet.breedId,
                    modifier = Modifier.size(104.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(if (isDog) "小狗主人" else "猫咪主人", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    // 相处天数：突出展示（大数字 + 爱心 + 文字）
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = togetherDays.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = primaryColor,
                        )
                        Spacer(Modifier.width(4.dp))
                        Canvas(Modifier.size(14.dp)) {
                            drawHeartFilledIcon(primaryColor, size.minDimension)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "天相伴",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = "好感度 ♡ ${pet.affection}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("领养于 ${pet.adoptedAt}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DiaryCard(entry: DiaryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                entry.createdAt,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                entry.content,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun DiaryComposerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("写一篇小猫日记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 30) title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题") },
                    supportingText = { Text("${title.length}/30") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 1000) content = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("今天发生了什么？") },
                    supportingText = { Text("${content.length}/1000") },
                    minLines = 5,
                    maxLines = 10,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VoxelButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        style = VoxelButtonStyle.NEUTRAL,
                        compact = true,
                    )
                    VoxelButton(
                        text = "保存",
                        onClick = { onSave(title, content) },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        compact = true,
                    )
                }
            }
        }
    }
}
