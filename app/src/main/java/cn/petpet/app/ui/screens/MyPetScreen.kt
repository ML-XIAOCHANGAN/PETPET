package cn.petpet.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.petpet.app.R
import cn.petpet.app.domain.AffectionRules
import cn.petpet.app.model.PetPetUiState
import cn.petpet.app.overlay.FloatingPetImageButton
import cn.petpet.app.ui.components.InteractiveBlackCatAvatar
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.drawCalendarIcon
import cn.petpet.app.ui.components.drawChatBubbleIcon
import cn.petpet.app.ui.components.drawHeartIcon
import cn.petpet.app.ui.components.drawPawIcon
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 单屏宠物首页。
 *
 * 页面没有纵向滚动：标题与功能按钮、宠物互动区、左侧陪伴参数和底部聊天框
 * 会在底部导航栏上方固定排布。
 */
@Composable
fun MyPetScreen(
    state: PetPetUiState,
    onInteract: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    chatFocusRequest: Long = 0L,
) {
    val pet = requireNotNull(state.pet)
    var chatInput by remember { mutableStateOf("") }
    var greetingVisible by remember { mutableStateOf(false) }
    var greetingRequest by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 对话弹窗：小猫回复后弹出气泡，显示 5 秒后自动隐藏
    val bubble = state.chatBubble
    var bubbleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(bubble) {
        if (bubble == null) {
            bubbleVisible = false
            return@LaunchedEffect
        }
        bubbleVisible = true
        delay(5000)
        bubbleVisible = false
    }

    LaunchedEffect(greetingRequest) {
        if (greetingRequest == 0) return@LaunchedEffect
        greetingVisible = true
        delay(2800)
        greetingVisible = false
    }

    LaunchedEffect(chatFocusRequest) {
        if (chatFocusRequest <= 0L) return@LaunchedEffect
        delay(420)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MyPet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(2.dp))
                FloatingPetImageButton(modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.weight(1f))
            ThemeToggleButton(
                darkTheme = state.darkTheme,
                onClick = { onDarkThemeChange(!state.darkTheme) },
            )
        }
        Text(
            text = pet.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val avatarSize = minOf(maxWidth * 0.70f, maxHeight * 0.66f, 292.dp)

            InteractiveBlackCatAvatar(
                onInteract = {
                    onInteract()
                    greetingRequest += 1
                },
                thinking = state.petTyping,
                breedId = pet.breedId,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 30.dp)
                    .size(avatarSize),
            )

            CompanionSummary(
                state = state,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = 34.dp),
            )

            // 对话弹窗：优先显示小猫回复（5秒），思考中显示打字动画，否则问候语
            androidx.compose.animation.AnimatedVisibility(
                visible = bubbleVisible || greetingVisible || state.petTyping,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(tween(180)) + scaleIn(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialScale = 0.72f,
                    transformOrigin = TransformOrigin(0.28f, 1f),
                ),
                exit = fadeOut(tween(180)) + scaleOut(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetScale = 0.86f,
                    transformOrigin = TransformOrigin(0.28f, 1f),
                ),
            ) {
                when {
                    state.petTyping -> TypingBubble()
                    bubbleVisible && bubble != null -> GreetingBubble(text = bubble.second)
                    greetingVisible -> GreetingBubble(text = state.greeting)
                    else -> GreetingBubble(text = state.greeting)
                }
            }
        }

        ChatInputBar(
            input = chatInput,
            onInputChange = { if (it.length <= 200) chatInput = it },
            onSend = {
                onSendMessage(chatInput)
                chatInput = ""
            },
            focusRequester = focusRequester,
        )
    }
}

@Composable
private fun ThemeToggleButton(
    darkTheme: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconColor = if (darkTheme) Color(0xFFFFC24B) else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(24.dp)) {
                if (darkTheme) {
                    drawSunIcon(iconColor)
                } else {
                    drawMoonIcon(iconColor)
                }
            }
        }
    }
}

/** 标准月牙 + 两颗小星星：日间显示，提示可切换到夜晚。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoonIcon(color: Color) {
    val r = size.minDimension / 2f
    val center = this.center
    // 标准月牙：外圆 + 偏移内圆差集（Path 直接构造）
    val crescent = Path().apply {
        // 外圆大部分（右下→左上）
        arcTo(
            rect = Rect(center.x - r * 0.92f, center.y - r * 0.92f, center.x + r * 0.92f, center.y + r * 0.92f),
            startAngleDegrees = 40f,
            sweepAngleDegrees = 280f,
            forceMoveTo = true,
        )
        // 内圆反向（挖出月牙内侧）
        arcTo(
            rect = Rect(center.x - r * 0.15f, center.y - r * 0.78f, center.x + r * 1.05f, center.y + r * 0.78f),
            startAngleDegrees = 40f + 280f,
            sweepAngleDegrees = -280f,
            forceMoveTo = false,
        )
        close()
    }
    drawPath(crescent, color)
    // 点缀小星星
    drawCircle(color, r * 0.09f, center + Offset(-r * 0.66f, -r * 0.70f))
    drawCircle(color, r * 0.06f, center + Offset(r * 0.20f, -r * 0.88f))
}

/** 太阳圆 + 八道光芒：夜间显示，提示可切换到白天。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunIcon(color: Color) {
    val r = size.minDimension / 2f
    val center = this.center
    drawCircle(color, r * 0.40f, center)
    val rays = 8
    repeat(rays) { i ->
        val angle = (360f / rays * i) * (PI / 180f).toFloat()
        val dir = Offset(cos(angle), sin(angle))
        drawLine(
            color = color,
            start = center + dir * (r * 0.60f),
            end = center + dir * (r * 0.92f),
            strokeWidth = r * 0.14f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun GreetingBubble(text: String) {
    Box(
        modifier = Modifier
            .width(270.dp)
            .height(154.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.speech_bubble),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-9).dp)
                .padding(horizontal = 32.dp),
            color = androidx.compose.ui.graphics.Color.Black,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

@Composable
private fun TypingBubble() {
    var dots by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(380)
            dots = (dots + 1) % 4
        }
    }
    val dotsText = ".".repeat(dots)
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(70.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.speech_bubble),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Text(
            text = "小猫正在想$dotsText",
            modifier = Modifier
                .offset(y = (-6).dp)
                .padding(horizontal = 20.dp),
            color = androidx.compose.ui.graphics.Color.Black,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompanionSummary(
    state: PetPetUiState,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val pet = requireNotNull(state.pet)
    val interactionSource = remember { MutableInteractionSource() }
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .animateContentSize(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            )
            .width(if (expanded) 148.dp else 52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { expanded = !expanded },
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        if (expanded) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("陪伴", fontWeight = FontWeight.Bold)
                // 好感度：自绘爱心 + 加粗数字
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(20.dp)) {
                        drawHeartIcon(accent, size.minDimension)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = pet.affection.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
                // 聊天进度：自绘气泡 + 数字
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(18.dp)) {
                        drawChatBubbleIcon(muted, size.minDimension)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${state.dailyProgress.chatCount}/${AffectionRules.CHAT_TARGET}",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
                // 互动进度：自绘爪印 + 数字
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(18.dp)) {
                        drawPawIcon(muted, size.minDimension)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${state.dailyProgress.interactionCount}/${AffectionRules.INTERACTION_DAILY_LIMIT}",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
                // 领养日期：自绘日历
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(16.dp)) {
                        drawCalendarIcon(muted, size.minDimension)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = pet.adoptedAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                    )
                }
                Text(
                    text = "‹ 收起",
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "陪\n伴",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                // 收起态：自绘爱心
                Canvas(Modifier.size(16.dp)) {
                    drawHeartIcon(accent, size.minDimension)
                }
                Text(
                    text = pet.affection.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "›",
                    color = accent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("今天过得还好吗") },
            leadingIcon = { Text("💬") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
        )
        VoxelButton(
            text = "发送",
            onClick = onSend,
            enabled = input.isNotBlank(),
            compact = true,
        )
    }
}
