package cn.petpet.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.petpet.app.R
import cn.petpet.app.model.CommunityPost
import cn.petpet.app.model.PetBreeds
import cn.petpet.app.model.PetProfile
import cn.petpet.app.model.PetType
import cn.petpet.app.ui.components.ImagePickerRow
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.drawCommentIcon
import cn.petpet.app.ui.components.drawHeartFilledIcon
import cn.petpet.app.ui.components.drawHeartOutlineIcon
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private data class CommunityFrame(
    @DrawableRes val image: Int,
    val caption: String,
    val emoticon: String,
)

private val communityFrames = listOf(
    CommunityFrame(R.drawable.cat_wink, "偷到一点好心情", "(˶ᵔ ᵕ ᵔ˶)"),
    CommunityFrame(R.drawable.cat_shy, "今天也被温柔接住", "ฅ^•ﻌ•^ฅ"),
    CommunityFrame(R.drawable.cat_sleeping, "午后暂停一下", "₍^. .^₎⟆"),
    CommunityFrame(R.drawable.cat_expression_curious, "发现一件小事", "(=^･ω･^=)"),
)

@Composable
fun CommunityScreen(
    pet: PetProfile,
    posts: List<CommunityPost>,
    online: Boolean = false,
    onRefreshNetwork: () -> Unit = {},
    onCreatePost: (String, List<String>) -> Unit,
    onToggleLike: (Long) -> Unit,
    onAddComment: (Long, String) -> Unit,
    onReport: () -> Unit,
    onToggleCollect: (Long) -> Unit = {},
    onToggleDislike: (Long) -> Unit = {},
    onDeletePost: (Long) -> Unit = {},
) {
    var postText by rememberSaveable { mutableStateOf("") }
    var composerExpanded by rememberSaveable { mutableStateOf(false) }
    var postImages by remember { mutableStateOf<List<String>>(emptyList()) }
    // 发布防连点：发布进行中禁用按钮，避免重复发帖
    var publishing by remember { mutableStateOf(false) }
    // 垃圾桶视图开关：true 显示已删除帖子的精简卡片（仅头像/名字/标题）
    var showTrash by rememberSaveable { mutableStateOf(false) }
    val isDog = PetBreeds.byId(pet.breedId)?.type == PetType.DOG

    // 首次进入尝试拉取在线动态（失败静默，保持离线模式）
    LaunchedEffect(Unit) {
        onRefreshNetwork()
    }

    val visiblePosts = if (showTrash) posts.filter { it.inTrash } else posts.filter { !it.inTrash }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (showTrash) "垃圾桶" else "社区",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(10.dp))
                // 🗑️ 垃圾桶：点击切换社区 / 垃圾桶视图
                Text(
                    text = "🗑️",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showTrash = !showTrash }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .semantics {
                            contentDescription = if (showTrash) "返回社区" else "打开垃圾桶"
                        },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                // 在线/离线标识：离线用本地数据，在线可同步
                Surface(
                    shape = CircleShape,
                    color = if (online) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = if (online) "在线" else "离线",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (online) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        if (showTrash) {
            // ── 垃圾桶：已删除帖子的精简展示（头像/名字/标题，正文与图片隐藏） ──
            if (visiblePosts.isEmpty()) {
                item {
                    Text(
                        text = "垃圾桶空空如也",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(visiblePosts, key = CommunityPost::id) { post ->
                    TrashPostCard(post = post)
                }
            }
        } else {
            item { CommunityReel() }

            item {
                CommunityComposer(
                    isDog = isDog,
                    expanded = composerExpanded,
                    text = postText,
                    images = postImages,
                    publishing = publishing,
                    onImagesChange = { postImages = it },
                    onExpandedChange = { composerExpanded = it },
                    onTextChange = { if (it.length <= 500) postText = it },
                    onPublish = {
                        if (publishing) return@CommunityComposer
                        publishing = true
                        onCreatePost(postText, postImages)
                        postText = ""
                        postImages = emptyList()
                        composerExpanded = false
                        publishing = false
                    },
                )
            }

            items(visiblePosts, key = CommunityPost::id) { post ->
                PostCard(
                    post = post,
                    onToggleLike = { onToggleLike(post.id) },
                    onAddComment = { onAddComment(post.id, it) },
                    onReport = onReport,
                    onToggleCollect = { onToggleCollect(post.id) },
                    onToggleDislike = { onToggleDislike(post.id) },
                    onDelete = { onDeletePost(post.id) },
                )
            }
        }
    }
}

@Composable
private fun CommunityReel() {
    var currentFrame by rememberSaveable { mutableIntStateOf(0) }
    var playing by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(playing) {
        while (playing) {
            delay(2300)
            currentFrame = (currentFrame + 1) % communityFrames.size
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentFrame,
                modifier = Modifier.align(Alignment.CenterEnd),
                transitionSpec = {
                    (fadeIn(tween(360)) + scaleIn(tween(420), initialScale = 0.94f)) togetherWith
                        (fadeOut(tween(260)) + scaleOut(tween(300), targetScale = 1.04f))
                },
                label = "社区片刻轮播",
            ) { frameIndex ->
                Image(
                    painter = painterResource(communityFrames[frameIndex].image),
                    contentDescription = communityFrames[frameIndex].caption,
                    modifier = Modifier
                        .size(198.dp)
                        .offset(x = 18.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp, end = 150.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = "片刻放映",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                )
                Text(
                    text = communityFrames[currentFrame].caption,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = communityFrames[currentFrame].emoticon,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 17.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                communityFrames.indices.forEach { index ->
                    Surface(
                        modifier = Modifier
                            .width(if (index == currentFrame) 18.dp else 5.dp)
                            .height(5.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = if (index == currentFrame) 0.75f else 0.20f,
                        ),
                    ) {}
                }
            }

            RoundActionButton(
                text = if (playing) "Ⅱ" else "▶",
                description = if (playing) "暂停片刻放映" else "播放片刻放映",
                onClick = { playing = !playing },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
            )
        }
    }
}

@Composable
private fun CommunityComposer(
    isDog: Boolean,
    expanded: Boolean,
    text: String,
    images: List<String>,
    publishing: Boolean = false,
    onImagesChange: (List<String>) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onPublish: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { if (!expanded) onExpandedChange(true) },
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    ) {
        if (expanded) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("分享一个小片刻…") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                )
                // 配图（最多 3 张）
                ImagePickerRow(
                    images = images,
                    onImagesChange = onImagesChange,
                    maxCount = 3,
                    hint = "图片",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    VoxelButton(
                        text = "取消",
                        onClick = { onExpandedChange(false) },
                        compact = true,
                        style = cn.petpet.app.ui.components.VoxelButtonStyle.NEUTRAL,
                    )
                    Spacer(Modifier.width(8.dp))
                    VoxelButton(
                        text = "发布",
                        onClick = onPublish,
                        enabled = text.isNotBlank() && !publishing,
                        compact = true,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Image(
                        painter = painterResource(if (isDog) R.drawable.dog_paw else R.drawable.cat_paw),
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "分享此刻…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "＋",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PostCard(
    post: CommunityPost,
    onToggleLike: () -> Unit,
    onAddComment: (String) -> Unit,
    onReport: () -> Unit,
    onToggleCollect: () -> Unit,
    onToggleDislike: () -> Unit,
    onDelete: () -> Unit = {},
) {
    var showCommentInput by rememberSaveable(post.id) { mutableStateOf(false) }
    var comment by rememberSaveable(post.id) { mutableStateOf("") }
    // 三点菜单展开状态
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (post.disliked) 0.55f else 1f },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Image(
                        painter = painterResource(R.drawable.cat_main),
                        contentDescription = null,
                        modifier = Modifier.padding(3.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = post.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (post.collected) {
                    Text(
                        "⭐",
                        modifier = Modifier.padding(end = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Box {
                    RoundActionButton(
                        text = "•••",
                        description = if (menuExpanded) "收起操作菜单" else "更多操作：举报、收藏、不喜欢、删除",
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("举报") },
                            onClick = {
                                menuExpanded = false
                                onReport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (post.collected) "取消收藏" else "收藏") },
                            onClick = {
                                menuExpanded = false
                                onToggleCollect()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (post.disliked) "取消不喜欢" else "不喜欢") },
                            onClick = {
                                menuExpanded = false
                                onToggleDislike()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "删除",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // 配图展示（本地内部路径）
                if (post.images.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        post.images.take(3).forEach { path ->
                            val bitmap = remember(path) {
                                runCatching { android.graphics.BitmapFactory.decodeFile(path) }.getOrNull()
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LikeChip(
                        count = post.likeCount,
                        liked = post.liked,
                        onClick = onToggleLike,
                    )
                    CommentChip(
                        count = post.comments.size,
                        active = showCommentInput,
                        onClick = { showCommentInput = !showCommentInput },
                    )
                }

                post.comments.lastOrNull()?.let { latest ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ) {
                        Text(
                            text = "${latest.author}  ${latest.text}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (showCommentInput) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { if (it.length <= 120) comment = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("说点什么…") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        VoxelButton(
                            text = "发送",
                            onClick = {
                                onAddComment(comment)
                                comment = ""
                                showCommentInput = false
                            },
                            enabled = comment.isNotBlank(),
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 垃圾桶里的帖子卡片：仅显示头像、名字、标题（正文首行截断），
 * 图片与正文内容一律不展示。
 */
@Composable
private fun TrashPostCard(post: CommunityPost) {
    // 标题 = 正文第一行（截断过长部分）
    val title = post.text.lineSequence().firstOrNull()?.trim()
        ?.take(24) ?: "（无内容）"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Image(
                    painter = painterResource(R.drawable.cat_main),
                    contentDescription = null,
                    modifier = Modifier.padding(3.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = post.author,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "🗑️",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** 点赞 chip：自绘爱心（空心/实心）+ 数字。 */
@Composable
private fun LikeChip(
    count: Int,
    liked: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconColor = if (liked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(13.dp),
        color = if (liked) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Canvas(Modifier.size(15.dp)) {
                if (liked) {
                    drawHeartFilledIcon(iconColor, size.minDimension)
                } else {
                    drawHeartOutlineIcon(iconColor, size.minDimension)
                }
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** 评论 chip：自绘聊天气泡 + 数字。 */
@Composable
private fun CommentChip(
    count: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(13.dp),
        color = if (active) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val iconColor = if (active) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Canvas(Modifier.size(15.dp)) {
                drawCommentIcon(iconColor, size.minDimension)
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(13.dp),
        color = if (active) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RoundActionButton(
    text: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .semantics { contentDescription = description }
            .size(38.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
