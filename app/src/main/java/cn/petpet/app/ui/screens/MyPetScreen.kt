package cn.petpet.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.graphics.TransformOrigin
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
import kotlinx.coroutines.delay

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

            androidx.compose.animation.AnimatedVisibility(
                visible = greetingVisible,
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
                GreetingBubble(text = state.greeting)
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
            Text(
                text = if (darkTheme) "☀" else "☾",
                style = MaterialTheme.typography.titleLarge,
            )
        }
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
private fun CompanionSummary(
    state: PetPetUiState,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val pet = requireNotNull(state.pet)
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .animateContentSize(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            )
            .width(if (expanded) 136.dp else 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { expanded = !expanded },
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        if (expanded) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("陪伴", fontWeight = FontWeight.Bold)
                Text("♡  ${pet.affection}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "💬 ${state.dailyProgress.chatCount}/${AffectionRules.CHAT_TARGET}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "🐾 ${state.dailyProgress.interactionCount}/${AffectionRules.INTERACTION_DAILY_LIMIT}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("领养 ${pet.adoptedAt}", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "‹ 收起",
                    color = MaterialTheme.colorScheme.primary,
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
                Text("♡", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = pet.affection.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "›",
                    color = MaterialTheme.colorScheme.primary,
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
