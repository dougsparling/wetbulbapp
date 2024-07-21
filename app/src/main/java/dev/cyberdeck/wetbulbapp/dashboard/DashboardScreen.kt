package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.math.MathUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    modifier: Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            viewModel.refresh()
        }
    }

    Surface(modifier = modifier) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (val state = uiState) {
                    DashboardViewModel.State.Empty -> {
                        Text("pull to refresh")
                    }

                    DashboardViewModel.State.NoLocation -> {
                        Text("need location permission")
                    }

                    is DashboardViewModel.State.Ready -> {
                        TempMeter(
                            modifier = Modifier.fillMaxWidth(),
                            conditions = state.conditions
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TempMeter(
    conditions: Conditions,
    modifier: Modifier = Modifier,
) {
    val guideline = Guideline.forConditions(conditions)

    val textMeasurer = rememberTextMeasurer()
    val tempString = "%.1f° C".format(conditions.wetBulbEstimate)
    val headline = MaterialTheme.typography.headlineLarge
    val measurement = remember {
        textMeasurer.measure(
            text = tempString,
            softWrap = false,
            style = headline.copy(
                color = guideline.color,
                fontSize = 64.sp
            )
        )
    }

    // drawing counter-clockwise (negative sweep) from starting angle with entire meter occupying arc number of degrees
    val initialAngle = -180f
    val arc = 180f

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
            (MathUtils.clamp(conditions.wetBulbEstimate, minTemp, maxTemp) - minTemp) / tempRange
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
                y = (size.height + measurement.size.height) / 2f
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

// https://www.wbgt.env.go.jp/en/wbgt.php
// plus an entry for 35+ which is where you're pretty much gonna die
enum class Guideline(
    val color: Color,
    val range: OpenEndRange<Double>
) {
    Safe(Color(0xff218cff), Double.NEGATIVE_INFINITY.rangeUntil(21.0)),
    Caution(Color(0xffa0d2ff), 21.0.rangeUntil(25.0)),
    Warning(Color(0xfffaf500), 25.0.rangeUntil(28.0)),
    Severe(Color(0xffff9600), 28.0.rangeUntil(31.0)),
    Danger(Color(0xffff2800), 31.0.rangeUntil(35.0)),
    Death(Color(0xFF000000), 35.0.rangeUntil(Double.POSITIVE_INFINITY));

    companion object {
        fun forConditions(conditions: Conditions): Guideline {
            val webBulb = conditions.wetBulbEstimate
            return entries.find { webBulb in it.range }!!
        }
    }
}

@Preview
@Composable
fun TempMeterPreview() {
    TempMeter(
        conditions = Conditions(temperature = 26.0, humidity = 80.0, wind = 5.0),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}