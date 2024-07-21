package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions

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
                        TempMeter(state.conditions)
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
    Text(
        text = "%.1f%n".format(conditions.wetBulbEstimate),
        style = MaterialTheme.typography.headlineLarge.copy(
            guideline.color
        )
    )
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