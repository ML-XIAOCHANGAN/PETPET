package cn.petpet.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 陪伴数据板块用的小图标（Canvas 自绘，风格统一、简洁）。
 * 与主题色联动，不依赖任何图标库。
 */

/** 爱心：两段圆弧 + 底部尖角。 */
fun DrawScope.drawHeartIcon(color: Color, size: Float, center: Offset = this.center) {
    val r = size / 2f
    val path = Path().apply {
        // 左圆
        moveTo(center.x, center.y + r * 0.72f)
        cubicTo(
            center.x - r * 1.1f, center.y + r * 0.05f,
            center.x - r * 0.7f, center.y - r * 0.85f,
            center.x, center.y - r * 0.28f,
        )
        // 右圆
        cubicTo(
            center.x + r * 0.7f, center.y - r * 0.85f,
            center.x + r * 1.1f, center.y + r * 0.05f,
            center.x, center.y + r * 0.72f,
        )
        close()
    }
    drawPath(path, color)
}

/** 聊天气泡：圆角方形 + 底部小尾巴。 */
fun DrawScope.drawChatBubbleIcon(color: Color, size: Float, center: Offset = this.center) {
    val w = size
    val h = size * 0.78f
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    val radius = w * 0.28f
    val path = Path().apply {
        // 圆角矩形主体
        arcTo(Rect(left, top, left + radius * 2f, top + radius * 2f), 180f, 90f, true)
        arcTo(Rect(left + w - radius * 2f, top, left + w, top + radius * 2f), 270f, 90f, false)
        arcTo(Rect(left + w - radius * 2f, top + h - radius * 2f, left + w, top + h), 0f, 90f, false)
        arcTo(Rect(left, top + h - radius * 2f, left + radius * 2f, top + h), 90f, 90f, false)
        close()
        // 底部尾巴
        moveTo(left + w * 0.30f, top + h)
        lineTo(left + w * 0.22f, top + h + size * 0.26f)
        lineTo(left + w * 0.52f, top + h)
        close()
    }
    drawPath(path, color)
}

/** 爪印：三颗脚趾 + 掌垫。 */
fun DrawScope.drawPawIcon(color: Color, size: Float, center: Offset = this.center) {
    val r = size / 2f
    val toeR = r * 0.20f
    // 三颗脚趾
    drawCircle(color, toeR, center + Offset(-r * 0.42f, -r * 0.38f))
    drawCircle(color, toeR, center + Offset(0f, -r * 0.52f))
    drawCircle(color, toeR, center + Offset(r * 0.42f, -r * 0.38f))
    // 掌垫（横椭圆）
    val padW = r * 0.72f
    val padH = r * 0.46f
    val pad = Path().apply {
        addOval(Rect(center.x - padW / 2f, center.y + r * 0.06f - padH / 2f, center.x + padW / 2f, center.y + r * 0.06f + padH / 2f))
    }
    drawPath(pad, color)
}

/** 日历：圆角矩形 + 顶部两个小耳朵。 */
fun DrawScope.drawCalendarIcon(color: Color, size: Float, center: Offset = this.center) {
    val w = size
    val h = size * 0.80f
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    val radius = w * 0.18f
    val body = Path().apply {
        addRoundRect(
            roundRect = androidx.compose.ui.geometry.RoundRect(
                rect = Rect(left, top, left + w, top + h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            ),
        )
    }
    drawPath(body, color)
    // 两个挂环
    drawLine(
        color = color,
        start = Offset(left + w * 0.24f, top),
        end = Offset(left + w * 0.24f, top - size * 0.14f),
        strokeWidth = size * 0.07f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(left + w * 0.76f, top),
        end = Offset(left + w * 0.76f, top - size * 0.14f),
        strokeWidth = size * 0.07f,
        cap = StrokeCap.Round,
    )
    // 中间横线
    drawLine(
        color = color,
        start = Offset(left + w * 0.12f, top + h * 0.32f),
        end = Offset(left + w * 0.88f, top + h * 0.32f),
        strokeWidth = size * 0.05f,
        cap = StrokeCap.Round,
    )
}

/** 小圆点指示器（用于未展开状态的爱心符号替代）。 */
fun DrawScope.drawDotIcon(color: Color, size: Float, center: Offset = this.center) {
    drawCircle(color, size * 0.10f, center)
}

/** 空心爱心（未点赞）：轮廓线条。 */
fun DrawScope.drawHeartOutlineIcon(color: Color, size: Float, center: Offset = this.center) {
    val r = size / 2f
    val path = Path().apply {
        moveTo(center.x, center.y + r * 0.72f)
        cubicTo(
            center.x - r * 1.1f, center.y + r * 0.05f,
            center.x - r * 0.7f, center.y - r * 0.85f,
            center.x, center.y - r * 0.28f,
        )
        cubicTo(
            center.x + r * 0.7f, center.y - r * 0.85f,
            center.x + r * 1.1f, center.y + r * 0.05f,
            center.x, center.y + r * 0.72f,
        )
        close()
    }
    drawPath(path, color, style = Stroke(width = size * 0.10f, cap = StrokeCap.Round))
}

/** 实心爱心（已点赞）。 */
fun DrawScope.drawHeartFilledIcon(color: Color, size: Float, center: Offset = this.center) {
    drawHeartIcon(color, size, center)
}

/** 评论/聊天气泡（实心小版，用于动态操作栏）。 */
fun DrawScope.drawCommentIcon(color: Color, size: Float, center: Offset = this.center) {
    val w = size
    val h = size * 0.72f
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    val radius = w * 0.22f
    val path = Path().apply {
        arcTo(Rect(left, top, left + radius * 2f, top + radius * 2f), 180f, 90f, true)
        arcTo(Rect(left + w - radius * 2f, top, left + w, top + radius * 2f), 270f, 90f, false)
        arcTo(Rect(left + w - radius * 2f, top + h - radius * 2f, left + w, top + h), 0f, 90f, false)
        arcTo(Rect(left, top + h - radius * 2f, left + radius * 2f, top + h), 90f, 90f, false)
        close()
        moveTo(left + w * 0.30f, top + h)
        lineTo(left + w * 0.22f, top + h + size * 0.24f)
        lineTo(left + w * 0.52f, top + h)
        close()
    }
    drawPath(path, color)
}

/** 举报：三个小圆点（竖排）。 */
fun DrawScope.drawReportIcon(color: Color, size: Float, center: Offset = this.center) {
    val dotR = size * 0.10f
    drawCircle(color, dotR, center + Offset(0f, -size * 0.22f))
    drawCircle(color, dotR, center)
    drawCircle(color, dotR, center + Offset(0f, size * 0.22f))
}

/** 底部导航：小猫脸（My Pet）。eyeColor 用导航栏底色模拟"挖空"眼睛。 */
fun DrawScope.drawCatFaceIcon(
    color: Color,
    size: Float,
    center: Offset = this.center,
    eyeColor: Color = Color.White,
) {
    val r = size / 2f
    // 左耳（尖耳，更明显）
    val leftEar = Path().apply {
        moveTo(center.x - r * 0.80f, center.y - r * 0.10f)
        lineTo(center.x - r * 0.42f, center.y - r * 0.66f)
        lineTo(center.x - r * 0.06f, center.y - r * 0.42f)
        close()
    }
    // 右耳
    val rightEar = Path().apply {
        moveTo(center.x + r * 0.80f, center.y - r * 0.10f)
        lineTo(center.x + r * 0.42f, center.y - r * 0.66f)
        lineTo(center.x + r * 0.06f, center.y - r * 0.42f)
        close()
    }
    drawPath(leftEar, color)
    drawPath(rightEar, color)
    // 脸（圆）
    drawCircle(color, r * 0.54f, center + Offset(0f, r * 0.12f))
    // 眼睛：用底色画"眯眯眼"横线，模拟挖空（深浅底都正确）
    drawLine(eyeColor, Offset(center.x - r * 0.28f, center.y + r * 0.12f), Offset(center.x - r * 0.08f, center.y + r * 0.12f), r * 0.10f, StrokeCap.Round)
    drawLine(eyeColor, Offset(center.x + r * 0.08f, center.y + r * 0.12f), Offset(center.x + r * 0.28f, center.y + r * 0.12f), r * 0.10f, StrokeCap.Round)
    // 小鼻子
    drawLine(eyeColor.copy(alpha = 0.75f), Offset(center.x, center.y + r * 0.30f), Offset(center.x, center.y + r * 0.42f), r * 0.08f, StrokeCap.Round)
}

/** 底部导航：日记本（成长）。 */
fun DrawScope.drawDiaryIcon(color: Color, size: Float, center: Offset = this.center) {
    val w = size * 0.64f
    val h = size * 0.70f
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    // 打开的书：两页对称
    val book = Path().apply {
        // 左页
        moveTo(left, top)
        cubicTo(left + w * 0.18f, top + h * 0.06f, left + w * 0.18f, top + h * 0.94f, left, top + h)
        lineTo(center.x, top + h * 0.92f)
        lineTo(center.x, top + h * 0.08f)
        close()
        // 右页
        moveTo(left + w, top)
        cubicTo(left + w * 0.82f, top + h * 0.06f, left + w * 0.82f, top + h * 0.94f, left + w, top + h)
        lineTo(center.x, top + h * 0.92f)
        lineTo(center.x, top + h * 0.08f)
        close()
    }
    drawPath(book, color)
}

/** 底部导航：朋友/社区（两个并排小人头）。 */
fun DrawScope.drawCommunityIcon(color: Color, size: Float, center: Offset = this.center) {
    val r = size / 2f
    // 左边人：头 + 肩
    drawCircle(color, r * 0.16f, center + Offset(-r * 0.28f, -r * 0.22f))
    drawCircle(color, r * 0.26f, center + Offset(-r * 0.28f, r * 0.28f))
    // 右边人：头 + 肩（略小、靠后）
    drawCircle(color, r * 0.13f, center + Offset(r * 0.30f, -r * 0.28f))
    drawCircle(color, r * 0.22f, center + Offset(r * 0.30f, r * 0.22f))
}
