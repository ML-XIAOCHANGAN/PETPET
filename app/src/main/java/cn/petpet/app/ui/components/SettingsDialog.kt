package cn.petpet.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.petpet.app.model.PetBreeds
import cn.petpet.app.model.PetProfile
import cn.petpet.app.model.PetType
import cn.petpet.app.ui.components.drawHeartFilledIcon

/**
 * 设置对话框：点击首页左上角"MyPet"进入。
 * 展示宠物档案 + 主题切换 + 对话模式 + 重新领养。
 */
@Composable
fun SettingsDialog(
    pet: PetProfile,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onResetAndReAdopt: () -> Unit,
    onDismiss: () -> Unit,
) {
    val breed = PetBreeds.byId(pet.breedId)
    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 标题
                Text(
                    text = "⚙️ 设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )

                // 宠物档案
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(breed?.avatarRes ?: cn.petpet.app.R.drawable.cat_main),
                        contentDescription = pet.name,
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "${breed?.name ?: "宠物"} · ${pet.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (breed?.type == PetType.CAT) "🐱 猫咪" else "🐶 狗狗",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "领养于 ${pet.adoptedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(12.dp)) {
                                drawHeartFilledIcon(primaryColor, size.minDimension)
                            }
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "好感度 ${pet.affection}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // 主题切换
                SettingRow(
                    title = "夜间模式",
                    subtitle = "切换浅色 / 深色主题",
                ) {
                    VoxelButton(
                        text = if (darkTheme) "🌙 深色" else "☀️ 浅色",
                        onClick = { onDarkThemeChange(!darkTheme) },
                        compact = true,
                    )
                }

                // 对话模式说明
                Text(
                    text = "对话模式：在线 AI（智谱 GLM-4-Flash 免费模型）可用时自动联网回复，无网络时由本地小猫陪你。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // 重新领养（危险操作，需确认）
                val confirmReset = remember { mutableStateOf(false) }
                if (confirmReset.value) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VoxelButton(
                            text = "取消",
                            onClick = { confirmReset.value = false },
                            style = VoxelButtonStyle.NEUTRAL,
                            modifier = Modifier.weight(1f),
                        )
                        VoxelButton(
                            text = "确认重新领养",
                            onClick = {
                                confirmReset.value = false
                                onResetAndReAdopt()
                            },
                            style = VoxelButtonStyle.DANGER,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    VoxelButton(
                        text = "重新领养一只新伙伴",
                        onClick = { confirmReset.value = true },
                        style = VoxelButtonStyle.NEUTRAL,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 关闭
                VoxelButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        trailing()
    }
}
