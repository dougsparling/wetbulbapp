package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.math.MathUtils
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun TempMeter(
    conditions: Conditions,
    modifier: Modifier = Modifier,
) {
    val guideline = Guideline.forConditions(conditions)
    val estimate = remember(conditions) { conditions.wetBulbEstimate }
    val textMeasurer = rememberTextMeasurer()
    val headline = MaterialTheme.typography.headlineLarge
    val measurement = remember(estimate) {
        textMeasurer.measure(
            text = "%.1f° C".format(estimate),
            softWrap = false,
            style = headline.copy(
                color = guideline.color,
                fontSize = 64.sp
            )
        )
    }

    // drawing counter-clockwise (negative sweep) from starting angle with entire meter occupying arc number of degrees
    val initialAngle = -195f
    val arc = 210f

    val minTemp = Guideline.Safe.range.endExclusive - 1.0
    val maxTemp = Guideline.Death.range.start + 1.0
    val tempRange = maxTemp - minTemp

    Canvas(
        modifier = modifier
            .padding(32.dp)
            .aspectRatio(1.0f)
    ) {
        val ringThickness = 25.0.dp.toPx()
        val indicatorThickness = 12.dp.toPx()

        Guideline.entries.forEach { guideline ->
            val tempStart = max(minTemp, guideline.range.start)
            val tempEnd = min(guideline.range.endExclusive, maxTemp)

            val startPos = (tempStart - minTemp) / tempRange
            val endPos = (tempEnd - minTemp) / tempRange

            val startAngle = initialAngle + startPos * arc
            val sweepAngle = arc * (endPos - startPos)

            drawArc(
                color = guideline.color,
                startAngle = startAngle.toFloat(),
                sweepAngle = sweepAngle.toFloat(),
                useCenter = false,
                style = Stroke(width = ringThickness)
            )
        }

        val conditionsPos =
            (MathUtils.clamp(estimate, minTemp, maxTemp) - minTemp) / tempRange
        val conditionAngle = initialAngle + conditionsPos * arc

        drawLine(
            color = guideline.color,
            start = center,
            end = pointOnCircle(
                -conditionAngle + 90,
                (size.height / 2.0f) - ringThickness,
                center.x,
                center.y
            ),
            strokeWidth = indicatorThickness
        )

        drawText(
            textLayoutResult = measurement,
            color = guideline.color,
            topLeft = Offset(
                x = (size.width - measurement.size.width) / 2f,
                y = (size.height - measurement.size.height) / 1.1f
            ),
        )

    }
}

private fun pointOnCircle(
    thetaInDegrees: Double,
    radius: Float,
    cX: Float,
    cY: Float,
): Offset {
    val x = cX + (radius * sin(Math.toRadians(thetaInDegrees)).toFloat())
    val y = cY + (radius * cos(Math.toRadians(thetaInDegrees)).toFloat())

    return Offset(x, y)
}