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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dev.cyberdeck.wetbulbapp.openmeteo.Location

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    modifier: Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    val wetBulb = when (val state = uiState) {
        DashboardViewModel.State.Empty -> "--"
        DashboardViewModel.State.NoLocation -> "need location" // TODO
        is DashboardViewModel.State.Ready -> "%.1f%n".format(state.conditions.wetBulbEstimate)
    }

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
                Text(
                    text = wetBulb,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

    }
}