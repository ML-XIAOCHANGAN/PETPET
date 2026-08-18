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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.petpet.app.model.ChatProvider
import cn.petpet.app.model.PetBreeds
import cn.petpet.app.model.PetProfile
import cn.petpet.app.model.PetType
import cn.petpet.app.ui.components.drawHeartFilledIcon

/**
 * 设置对话框：点击首页左上角"MyPet"进入。
 * 展示宠物档案 + 主题切换 + AI 对话模型配置 + 重新领养。
 */
@Composable
fun SettingsDialog(
    pet: PetProfile,
    darkTheme: Boolean,
    chatProvider: ChatProvider,
    chatApiKey: String,
    petSoundEnabled: Boolean = false,
    onDarkThemeChange: (Boolean) -> Unit,
    onChatConfigChange: (ChatProvider, String) -> Unit,
    onPetSoundEnabledChange: (Boolean) -> Unit = {},
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

                // 宠物点击音效开关（默认关闭）
                SettingRow(
                    title = "宠物音效",
                    subtitle = "点击宠物时随机播放叫声，连点小猫会哈气",
                ) {
                    Switch(
                        checked = petSoundEnabled,
                        onCheckedChange = { onPetSoundEnabledChange(it) },
                    )
                }

                // ── AI 对话模型配置 ──
                var selectedProvider by remember { mutableStateOf(chatProvider) }
                var apiKeyInput by remember { mutableStateOf(chatApiKey) }
                var showApiKey by remember { mutableStateOf(false) }
                Text(
                    text = "🤖 AI 对话模型",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "填入密钥后直连模型服务（手机有网即可用）；未填密钥时走本机网关，均失败则本地小猫陪你。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 模型选择（四个 Provider 一行）
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChatProvider.entries.forEach { provider ->
                        val isSelected = provider == selectedProvider
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedProvider = provider },
                                ),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Text(
                                text = provider.displayName,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
                // API Key 输入（掩码 + 明文切换）
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(selectedProvider.hint) },
                    singleLine = true,
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        Text(
                            text = if (showApiKey) "🙈" else "👁️",
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showApiKey = !showApiKey },
                                )
                                .padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                )
                Text(
                    text = "当前模型：${selectedProvider.defaultModel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VoxelButton(
                    text = "保存对话模型",
                    onClick = { onChatConfigChange(selectedProvider, apiKeyInput) },
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // ── 隐私与权限说明（T-007 合规轻量落地）──
                var showPrivacy by remember { mutableStateOf(false) }
                SettingRow(
                    title = "隐私与权限说明",
                    subtitle = if (showPrivacy) "点击收起" else "数据存储、AI 对话与权限用途",
                ) {
                    VoxelButton(
                        text = if (showPrivacy) "收起" else "查看",
                        onClick = { showPrivacy = !showPrivacy },
                        compact = true,
                    )
                }
                if (showPrivacy) {
                    Text(
                        text = "• 数据全部保存在你的设备本地（宠物、好感度、社区、日记），没有账号体系，不会上传到云端。\n" +
                            "• AI 对话：填入 API Key 后直连对应模型服务（Key 仅存本机）；未填时走本机网关或离线回复。模型为生成式 AI，回复仅供参考。\n" +
                            "• 悬浮窗权限：用于将宠物显示在其他应用上层陪伴你；通知权限：用于悬浮服务常驻提示。两项均可在系统设置中随时关闭。\n" +
                            "• 应用不含广告，不收集位置、通讯录等敏感信息。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

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
