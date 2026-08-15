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
import cn.petpet.app.model.PetBreeds
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 展示宠物头像（按品种）。
 *
 * 程序只对图片执行缩放、位移、旋转和淡入淡出。点击后黑猫会短暂切换表情，
 * 其他品种只做互动动效（它们暂无表情包）。
 */
@Composable
fun InteractiveBlackCatAvatar(
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    thinking: Boolean = false,
    breedId: String = "black_cat",
) {
    val mainRes = remember(breedId) {
        PetBreeds.byId(breedId)?.avatarRes ?: R.drawable.cat_main
    }
    val infiniteTransition = rememberInfiniteTransition(label = "宠物持续呼吸")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "宠物呼吸缩放",
    )
    val floatingY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "宠物上下浮动",
    )
    val interactionProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val floatingPixels = with(LocalDensity.current) { floatingY.dp.toPx() }
    // 黑猫有专属表情包；其他品种只复用主头像做互动动效
    val expressionImages = remember(breedId) {
        if (breedId == "black_cat") {
            listOf(
                R.drawable.cat_wink,
                R.drawable.cat_shy,
                R.drawable.cat_expression_happy,
                R.drawable.cat_expression_curious,
                R.drawable.cat_expression_smile,
                R.drawable.cat_expression_squint,
            )
        } else {
            listOf(mainRes)
        }
    }
    @DrawableRes var currentImage by remember { mutableIntStateOf(mainRes) }
    var interactionSequence by remember { mutableIntStateOf(0) }
    val progress = interactionProgress.value
    val interactionScale = 1f + sin(progress * PI).toFloat() * 0.12f
    val interactionRotation = sin(progress * PI * 2).toFloat() * 7f

    // 品种变化时重置主头像
    LaunchedEffect(mainRes) {
        currentImage = mainRes
    }

    // 思考中：黑猫显示好奇表情，其他品种保持主头像（加轻微摇摆）
    LaunchedEffect(thinking, mainRes) {
        if (thinking && breedId == "black_cat") {
            currentImage = R.drawable.cat_expression_curious
        } else if (currentImage == R.drawable.cat_expression_curious || currentImage != mainRes) {
            currentImage = mainRes
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
                            currentImage = mainRes
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
            label = "宠物表情切换",
        ) { imageResource ->
            Image(
                painter = painterResource(imageResource),
                contentDescription = if (imageResource == mainRes) {
                    "宠物头像，点击与它互动"
                } else {
                    "宠物正在回应你的互动"
                },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
