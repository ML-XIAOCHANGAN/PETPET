package cn.petpet.app.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.petpet.app.data.PetImageStorage
import java.io.File

/**
 * 图片选择工具：Photo Picker（免权限）选图 → 复制压缩到内部目录 → 返回路径列表。
 * 供日记 / 社区配图复用。
 *
 * @param maxCount 最多可选图片数
 * @param images 当前已选图片路径（展示缩略图，可删除）
 * @param onImagesChange 图片列表变化回调
 * @param hint 空状态提示文字
 */
@Composable
fun ImagePickerRow(
    images: List<String>,
    onImagesChange: (List<String>) -> Unit,
    maxCount: Int = 3,
    hint: String = "添加图片",
) {
    val context = LocalContext.current
    // Photo Picker：系统级选择器，免存储权限；Android 13+ 原生，旧版本自动降级
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxCount.coerceAtLeast(1)),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val existing = images.toMutableList()
            uris.forEach { uri ->
                PetImageStorage.saveImage(context, uri, "pick")?.let { existing.add(it) }
            }
            onImagesChange(existing.take(maxCount))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 已选图片缩略图（带删除角标）
        images.forEach { path ->
            val bitmap = remember(path) { runCatching { android.graphics.BitmapFactory.decodeFile(path) }.getOrNull() }
            Box {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
                // 删除角标
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                        .clickable {
                            onImagesChange(images.filterNot { it == path })
                            PetImageStorage.deleteImage(path)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Color.White, fontSize = 11.sp)
                }
            }
        }
        // 添加按钮（达到上限后隐藏）
        if (images.size < maxCount) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (images.isEmpty()) "＋\n$hint" else "＋",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
