package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardScreen(
    modifier: Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    val temp = when(val state = uiState) {
        DashboardViewModel.State.Empty -> "--"
        is DashboardViewModel.State.Ready -> "%.1f%n".format(state.conditions.temperature)
    }

    Surface(modifier = modifier) {
        Text(
            text = temp,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}