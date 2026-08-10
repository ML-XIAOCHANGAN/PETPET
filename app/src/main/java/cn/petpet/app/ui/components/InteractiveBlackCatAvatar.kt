package cn.petpet.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.petpet.app.R
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 展示用户提供的小猫图片。
 *
 * 程序只对图片执行缩放、位移、旋转和淡入淡出，不再用画布绘制小猫。
 * 点击后会短暂切换为用户提供的表情图片，然后恢复主头像。
 */
@Composable
fun InteractiveBlackCatAvatar(
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    thinking: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "小猫持续呼吸")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "小猫呼吸缩放",
    )
    val floatingY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "小猫上下浮动",
    )
    val interactionProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val floatingPixels = with(LocalDensity.current) { floatingY.dp.toPx() }
    val expressionImages = remember {
        listOf(
            R.drawable.cat_wink,
            R.drawable.cat_shy,
            R.drawable.cat_expression_happy,
            R.drawable.cat_expression_curious,
            R.drawable.cat_expression_smile,
            R.drawable.cat_expression_squint,
        )
    }
    @DrawableRes var currentImage by remember { mutableIntStateOf(R.drawable.cat_main) }
    var interactionSequence by remember { mutableIntStateOf(0) }
    val progress = interactionProgress.value
    val interactionScale = 1f + sin(progress * PI).toFloat() * 0.12f
    val interactionRotation = sin(progress * PI * 2).toFloat() * 7f

    // 思考中：显示好奇表情，用户等待回复时小猫保持"在想"的样子
    LaunchedEffect(thinking) {
        if (thinking) {
            currentImage = R.drawable.cat_expression_curious
        } else if (currentImage == R.drawable.cat_expression_curious) {
            currentImage = R.drawable.cat_main
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = floatingPixels
                scaleX = breathingScale * interactionScale
                scaleY = breathingScale * interactionScale
                rotationZ = interactionRotation
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    onInteract()
                    interactionSequence += 1
                    val currentSequence = interactionSequence
                    currentImage = expressionImages.random()
                    scope.launch {
                        interactionProgress.snapTo(0f)
                        interactionProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(680, easing = FastOutSlowInEasing),
                        )
                        if (currentSequence == interactionSequence) {
                            currentImage = R.drawable.cat_main
                            interactionProgress.snapTo(0f)
                        }
                    }
                },
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = currentImage,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "小猫表情切换",
        ) { imageResource ->
            Image(
                painter = painterResource(imageResource),
                contentDescription = if (imageResource == R.drawable.cat_main) {
                    "小猫头像，点击与它互动"
                } else {
                    "小猫正在回应你的互动"
                },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
