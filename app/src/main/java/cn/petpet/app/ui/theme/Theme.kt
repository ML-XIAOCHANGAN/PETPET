package cn.petpet.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF171A18),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE9E0),
    onPrimaryContainer = Color(0xFF17251B),
    secondary = Color(0xFF607466),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE8DF),
    onSecondaryContainer = Color(0xFF1A2A20),
    tertiary = Color(0xFF8A5D47),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE1D2),
    onTertiaryContainer = Color(0xFF351106),
    background = Color(0xFFF7F8F5),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFE5EAE5),
    onSurfaceVariant = Color(0xFF444944),
    outline = Color(0xFF747974),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEAF0EB),
    onPrimary = Color(0xFF202421),
    primaryContainer = Color(0xFF344039),
    onPrimaryContainer = Color(0xFFDDE9E0),
    secondary = Color(0xFFBBCBBE),
    onSecondary = Color(0xFF26352B),
    secondaryContainer = Color(0xFF394A3F),
    onSecondaryContainer = Color(0xFFD7E7DA),
    tertiary = Color(0xFFFFBFA1),
    onTertiary = Color(0xFF51200F),
    tertiaryContainer = Color(0xFF6D3622),
    onTertiaryContainer = Color(0xFFFFDBCA),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE1E4E0),
    surface = Color(0xFF181D19),
    onSurface = Color(0xFFE1E4E0),
    surfaceVariant = Color(0xFF282E29),
    onSurfaceVariant = Color(0xFFC4C9C3),
    outline = Color(0xFF8E938E),
)

/**
 * 对主要 Material3 色彩逐项插值，使日夜模式不是瞬间跳变。
 */
@Composable
fun PetPetTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val target = if (darkTheme) DarkColors else LightColors
    val animated = animatedColorScheme(target)
    val view = LocalView.current

    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = animated,
        content = content,
    )
}

@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val animation = tween<Color>(
        durationMillis = 500,
        easing = FastOutSlowInEasing,
    )
    val primary by animateColorAsState(target.primary, animation, label = "主题主色")
    val onPrimary by animateColorAsState(target.onPrimary, animation, label = "主题主色文字")
    val primaryContainer by animateColorAsState(target.primaryContainer, animation, label = "主题主色容器")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, animation, label = "主题主色容器文字")
    val secondary by animateColorAsState(target.secondary, animation, label = "主题辅色")
    val onSecondary by animateColorAsState(target.onSecondary, animation, label = "主题辅色文字")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, animation, label = "主题辅色容器")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, animation, label = "主题辅色容器文字")
    val tertiary by animateColorAsState(target.tertiary, animation, label = "主题第三色")
    val onTertiary by animateColorAsState(target.onTertiary, animation, label = "主题第三色文字")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, animation, label = "主题第三色容器")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, animation, label = "主题第三色容器文字")
    val background by animateColorAsState(target.background, animation, label = "主题背景")
    val onBackground by animateColorAsState(target.onBackground, animation, label = "主题背景文字")
    val surface by animateColorAsState(target.surface, animation, label = "主题表面")
    val onSurface by animateColorAsState(target.onSurface, animation, label = "主题表面文字")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, animation, label = "主题表面变体")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, animation, label = "主题表面变体文字")
    val outline by animateColorAsState(target.outline, animation, label = "主题轮廓")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
    )
}
