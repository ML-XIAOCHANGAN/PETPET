package cn.petpet.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * FitCut 自绘图表组件集（Canvas，零依赖）。
 * ⚠️ 项目约定：Canvas lambda 内不能调 MaterialTheme.colorScheme —— 颜色在调用处先算好传入。
 */

/** 进度环：展示 摄入/目标 类比例。 */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    size: Float = 96f,
    strokeWidth: Float = 10f,
    centerText: String? = null,
    subText: String? = null,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
            // 底环
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            // 进度弧（从 12 点方向顺时针）
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerText != null) {
                Text(centerText, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
            }
            if (subText != null) {
                Text(subText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 简单折线图：展示体重 / 热量趋势。 */
@Composable
fun FitLineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Int = 120,
    strokeWidth: Float = 3f,
    fill: Boolean = true,
) {
    val fillColor = color.copy(alpha = 0.12f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
    ) {
        if (values.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val maxV = max(values.max(), 0f) * 1.1f
        val minV = min(values.min(), 0f) * 0.9f
        val range = (maxV - minV).coerceAtLeast(0.01f)
        val stepX = w / (values.size - 1)
        fun yFor(v: Float): Float = h - ((v - minV) / range) * h

        val points = values.mapIndexed { i, v -> Offset(i * stepX, yFor(v)) }
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        if (fill) {
            val fillPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(fillPath, fillColor)
        }
        drawPath(path, color, style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round))
        // 端点圆点
        drawCircle(color, radius = 4.dp.toPx(), center = points.last())
    }
}

/** 双柱对比：摄入 vs 消耗。 */
@Composable
fun FitBarCompare(
    leftValue: Float,
    rightValue: Float,
    leftLabel: String,
    rightLabel: String,
    leftColor: Color,
    rightColor: Color,
    modifier: Modifier = Modifier,
    height: Int = 100,
) {
    val maxV = max(max(leftValue, rightValue), 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Bar(modifier = Modifier.weight(1f), ratio = leftValue / maxV, color = leftColor)
            Spacer(Modifier.width(12.dp))
            Bar(modifier = Modifier.weight(1f), ratio = rightValue / maxV, color = rightColor)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(leftLabel, Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rightLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(leftValue.toInt().toString(), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = leftColor)
            Text(rightValue.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = rightColor)
        }
    }
}

@Composable
private fun Bar(modifier: Modifier, ratio: Float, color: Color) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxWidth()) {
            val barH = size.height * ratio.coerceIn(0f, 1f)
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, size.height - barH),
                size = Size(size.width, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            )
        }
    }
}

/** 营养素占比环（多段圆环）：蛋白 / 碳水 / 脂肪。 */
@Composable
fun MacroRing(
    proteinRatio: Float,
    carbRatio: Float,
    fatRatio: Float,
    proteinColor: Color,
    carbColor: Color,
    fatColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    size: Float = 120f,
    strokeWidth: Float = 14f,
) {
    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Butt)
            val total = (proteinRatio + carbRatio + fatRatio).coerceAtLeast(0.0001f)
            var start = -90f
            val segments = listOf(
                proteinRatio / total to proteinColor,
                carbRatio / total to carbColor,
                fatRatio / total to fatColor,
            )
            drawArc(trackColor, 0f, 360f, false, style = stroke)
            segments.forEach { (ratio, color) ->
                if (ratio > 0f) {
                    drawArc(color, start, 360f * ratio, false, style = stroke)
                    start += 360f * ratio
                }
            }
        }
        Text(
            "三大\n营养素",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
