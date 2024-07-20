package dev.cyberdeck.wetbulbapp.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions
import dev.cyberdeck.wetbulbapp.openmeteo.Location
import dev.cyberdeck.wetbulbapp.openmeteo.OpenMeteoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class DashboardViewModel : ViewModel() {
    private val service = OpenMeteoService()

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Empty)
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if(refreshing.value) return

        viewModelScope.launch {
            _refreshing.value = true

            // TODO: errors
            val conditions = service.current(
                Location(
                    lat = 35.661777,
                    long = 139.704056
                )
            )

            _state.value = State.Ready(
                updatedAt = Instant.now(),
                conditions = conditions
            )

            _refreshing.value = false

        }
    }

    sealed interface State {
        data object Empty : State
        data class Ready(
            val updatedAt: Instant,
            val conditions: Conditions
        ) : State
    }

}
