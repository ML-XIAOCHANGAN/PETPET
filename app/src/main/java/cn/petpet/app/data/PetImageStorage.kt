package cn.petpet.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * 图片存储工具：把用户选择的图片（content:// URI）复制并压缩到应用内部目录，
 * 返回可持久化的文件路径。内部目录路径在应用生命周期内稳定，重启/更新不丢失；
 * 与 URI 相比没有权限过期问题。
 */
object PetImageStorage {

    /** 图片根目录（filesDir/pet_images/）。 */
    private fun root(context: Context): File =
        File(context.filesDir, "pet_images").apply { mkdirs() }

    /**
     * 保存一张图片：读取 content:// URI → 解码 → 压缩到最长边 1080px → JPEG 80% 写入内部目录。
     * @return 保存后的绝对路径；失败返回 null。
     */
    fun saveImage(context: Context, uri: Uri, prefix: String = "img"): String? {
        return runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return null

            // 等比缩放：最长边 1080px，控制体积
            val maxSide = 1080
            val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
            val outBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bitmap
            }

            val file = File(root(context), "${prefix}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { fos ->
                outBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }
            if (outBitmap != bitmap) outBitmap.recycle()
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    /** 删除内部图片文件（删除日记/动态时清理）。 */
    fun deleteImage(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
