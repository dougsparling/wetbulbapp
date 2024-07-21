package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.math.MathUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.cyberdeck.wetbulbapp.R
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions
import java.time.LocalTime
import java.time.temporal.ChronoField
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
        } else {
            locationPermissionState.launchPermissionRequest()
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

                    }

                    DashboardViewModel.State.NoLocation -> {
                        Text(
                            text = stringResource(R.string.need_location_permission),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    is DashboardViewModel.State.Ready -> {
                        CurrentConditions(state.conditions)
                        ForecastedConditions(state.forecast)
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastedConditions(forecast: List<Conditions>) {
    val now = LocalTime.now()
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.padding(16.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.hourly_forecast),
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Surface(
                shape = RoundedCornerShape(8.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(forecast) { item ->
                        ShortForecast(item, now, modifier = Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortForecast(
    conditions: Conditions,
    now: LocalTime,
    modifier: Modifier = Modifier,
) {
    val text = "%.0f°".format(conditions.wetBulbEstimate)
    val guideline = Guideline.forConditions(conditions)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(color = guideline.color)
            .padding(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color.Black
            )
        )
        Text(
            text = guideline.emoji(),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "%dh".format(
                now.plusHours(conditions.offsetHours.toLong())
                    .get(ChronoField.HOUR_OF_DAY)
            ),
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.Black
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CurrentConditions(conditions: Conditions) {
    Text(
        text = stringResource(R.string.title_current_conditions),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )

    TempMeter(
        modifier = Modifier.fillMaxWidth(),
        conditions = conditions
    )

    val guideline = Guideline.forConditions(conditions)

    Text(
        text = guideline.emoji() + " " + guideline.subHeader() + " " + guideline.emoji(),
        style = MaterialTheme.typography.titleMedium.copy(
            color = guideline.color,
            fontWeight = FontWeight.Bold
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    )

    Text(
        text = guideline.description(),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .wrapContentSize()
            .padding(horizontal = 16.dp)
    )
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

@Preview
@Composable
fun CurrentConditionsPreview() {
    Column {
        CurrentConditions(
            conditions = Conditions(temperature = 26.0, humidity = 80.0, wind = 5.0),
        )
    }
}

@Preview
@Composable
fun ForecastedConditionsPreview() {
    Column {
        val forecast = (24 until 48).mapIndexed { index, temp ->
            Conditions(
                temperature = temp.toDouble(),
                humidity = 50.0 + temp,
                wind = 5.0,
                offsetHours = index + 1
            )
        }
        ForecastedConditions(forecast = forecast)
    }
}