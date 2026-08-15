package cn.petpet.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.petpet.app.model.HomeSection

enum class VoxelButtonStyle {
    PRIMARY,
    SECONDARY,
    NEUTRAL,
    DANGER,
}

/**
 * 保留旧接口名称以避免影响业务层，视觉已由体素立体面收敛为极简圆角按钮。
 */
@Composable
fun VoxelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: VoxelButtonStyle = VoxelButtonStyle.PRIMARY,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        label = "极简按钮按压缩放",
    )
    val containerColor by animateColorAsState(
        targetValue = buttonContainerColor(style),
        label = "极简按钮颜色",
    )
    val contentColor = buttonContentColor(style)
    val shape = RoundedCornerShape(if (compact) 14.dp else 17.dp)

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.42f)
            .defaultMinSize(minWidth = if (compact) 64.dp else 104.dp)
            .height(if (compact) 42.dp else 50.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = if (style == VoxelButtonStyle.NEUTRAL) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        } else {
            null
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = if (compact) 12.dp else 18.dp),
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun VoxelNavigationBar(
    selected: HomeSection,
    onSelect: (HomeSection) -> Unit,
    petType: cn.petpet.app.model.PetType? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeSection.entries.forEach { section ->
                val active = selected == section
                val background by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    label = "底部导航背景",
                )
                val foreground by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "底部导航文字",
                )
                val interactionSource = remember { MutableInteractionSource() }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable(
                            role = Role.Tab,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(section) },
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = background,
                    contentColor = foreground,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // 图标统一用 emoji；My Pet 随领养宠物类型变化（猫🐱/狗🐶）
                        Text(
                            text = when (section) {
                                HomeSection.MY_PET -> if (petType == cn.petpet.app.model.PetType.DOG) "🐶" else "🐱"
                                HomeSection.GROWTH -> "📖"
                                HomeSection.COMMUNITY -> "👥"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun buttonContainerColor(style: VoxelButtonStyle): Color {
    return when (style) {
        VoxelButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        VoxelButtonStyle.SECONDARY -> MaterialTheme.colorScheme.tertiaryContainer
        VoxelButtonStyle.NEUTRAL -> MaterialTheme.colorScheme.surface
        VoxelButtonStyle.DANGER -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun buttonContentColor(style: VoxelButtonStyle): Color {
    return when (style) {
        VoxelButtonStyle.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        VoxelButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onTertiaryContainer
        VoxelButtonStyle.NEUTRAL -> MaterialTheme.colorScheme.onSurface
        VoxelButtonStyle.DANGER -> MaterialTheme.colorScheme.onError
    }
}
