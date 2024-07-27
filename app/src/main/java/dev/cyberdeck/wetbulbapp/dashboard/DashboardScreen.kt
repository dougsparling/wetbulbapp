package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.cyberdeck.wetbulbapp.R
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions
import dev.cyberdeck.wetbulbapp.openmeteo.TestData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
                        Text(
                            text = stringResource(R.string.title_current_conditions),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        LocationEditor(
                            location = state.location,
                            modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                            provideSuggestions = { text ->
                                viewModel.findLocations(text)
                            },
                            onLocationChange = { loc ->
                                viewModel.refresh(at = loc)
                            }
                        )
                        CurrentConditions(state.conditions)
                        ForecastedConditions(state.forecast)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentConditions(
    conditions: Conditions
) {
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
fun ForecastedConditions(forecast: List<Conditions>) {
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
                    val now = LocalDateTime.now()
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
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val text = "%.1f°".format(conditions.wetBulbEstimate) // half-even rounding mode
    val guideline = Guideline.forConditions(conditions)
    val timeFormatter = remember { DateTimeFormatter.ofPattern("k'h'") }

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
            text = when {
                conditions.time.isBefore(now) -> stringResource(id = R.string.now)
                else -> timeFormatter.format(conditions.time)
            },
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.Black
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview
@Composable
fun CurrentConditionsPreview() {
    Column {
        CurrentConditions(
            conditions = TestData.increasingTemps.first(),
        )
    }
}

@Preview
@Composable
fun ForecastedConditionsPreview() {
    Column {
        val forecast = TestData.increasingTemps
        ForecastedConditions(forecast = forecast)
    }
}