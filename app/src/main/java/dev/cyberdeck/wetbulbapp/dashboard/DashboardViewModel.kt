package dev.cyberdeck.wetbulbapp.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions
import dev.cyberdeck.wetbulbapp.openmeteo.Location
import dev.cyberdeck.wetbulbapp.openmeteo.OpenMeteoService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.location.Location as AndroidLocation


class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val service = OpenMeteoService()
    private val locationClient = LocationServices.getFusedLocationProviderClient(app)

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Empty)
    val state = _state.asStateFlow()

    fun refresh() {
        if (!_refreshing.compareAndSet(expect = false, update = true)) return

        viewModelScope.launch {
            // TODO: errors
            val location = runCatching { queryLocation() }.getOrElse {
                it.printStackTrace()
                _state.value = State.NoLocation
                return@launch
            }

            val conditions = runCatching { service.current(location) }.getOrElse {
                it.printStackTrace()
                _state.value = State.Empty // TODO: error state?
                return@launch
            }

            _state.value = State.Ready(
                updatedAt = Instant.now(),
                conditions = conditions
            )
        }.invokeOnCompletion {
            _refreshing.value = false
        }
    }

    private suspend fun queryLocation() = suspendCoroutine { cont ->
        try {
            val locationTask = locationClient.lastLocation
            locationTask.addOnSuccessListener { location: AndroidLocation ->
                cont.resume(Location(
                    lat = location.latitude,
                    long = location.longitude
                ))
            }
            locationTask.addOnFailureListener { ex ->
                cont.resumeWithException(ex)
            }
            locationTask.addOnCanceledListener {
                cont.resumeWithException(CancellationException())
            }
        } catch (s: SecurityException) {
            cont.resumeWithException(s)
        }
    }

    sealed interface State {
        data object Empty : State
        data object NoLocation: State
        data class Ready(
            val updatedAt: Instant,
            val conditions: Conditions
        ) : State
    }

}
