package dev.cyberdeck.wetbulbapp.dashboard

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import dev.cyberdeck.wetbulbapp.openmeteo.Conditions
import dev.cyberdeck.wetbulbapp.openmeteo.Location
import dev.cyberdeck.wetbulbapp.openmeteo.OpenMeteoService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.location.Location as AndroidLocation


class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val service = OpenMeteoService()
    private val locationClient = LocationServices.getFusedLocationProviderClient(app)
    private val geo = Geocoder(app.applicationContext, Locale.getDefault())

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Empty)
    val state = _state.asStateFlow()

    private var locationSearchJob: Job? = null

    fun refresh(at: Location? = null) {
        if (!_refreshing.compareAndSet(expect = false, update = true)) return

        viewModelScope.launch {
            val location = at ?: (state.value as? State.Ready)?.location
            ?: runCatching { queryLocation() }.getOrElse {
                it.printStackTrace()
                _state.value = State.NoLocation
                return@launch
            }

            val forecast = runCatching { service.forecast(location) }.getOrElse {
                it.printStackTrace()
                _state.value = State.Empty // TODO: error state?
                return@launch
            }

            _state.value = State.Ready(
                updatedAt = Instant.now(),
                conditions = forecast.current,
                forecast = forecast.forecast,
                location = location
            )
        }.invokeOnCompletion {
            _refreshing.value = false
        }
    }

    private suspend fun queryLocation() = withContext(Dispatchers.IO) {
        suspendCoroutine { cont ->
            try {
                val locationTask = locationClient.lastLocation
                locationTask.addOnSuccessListener { location: AndroidLocation? ->

                    // wtf?
                    if (location == null) {
                        cont.resumeWithException(NullPointerException("location == null"))
                        return@addOnSuccessListener
                    }

                    @Suppress("DEPRECATION") val address =
                        geo.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()

                    cont.resume(
                        Location(
                            name = address?.locality ?: "Current",
                            lat = location.latitude,
                            long = location.longitude
                        )
                    )
                }
                locationTask.addOnFailureListener { ex ->
                    cont.resumeWithException(ex)
                }
                locationTask.addOnCanceledListener {
                    cont.resumeWithException(CancellationException())
                }
            } catch (s: SecurityException) {
                cont.resumeWithException(s)
            } catch (i: IllegalArgumentException) {
                cont.resumeWithException(i)
            }
        }
    }

    suspend fun findLocations(text: String): List<Location> {
        locationSearchJob?.cancel()
        if (text.isEmpty() || text.length < 3) return emptyList()

        return supervisorScope {
            val deferred = async {
                // TODO: dumb way to debounce, but...
                delay(200)
                val results = withContext(Dispatchers.IO) {
                    geo.getFromLocationName(text, 5) ?: emptyList()
                }
                results
                    .map { result ->
                        Location(
                            name = result.locality ?: result.subAdminArea ?: result?.adminArea
                            ?: result?.postalCode ?: "",
                            lat = result.latitude,
                            long = result.longitude
                        )
                    }
                    .distinctBy { it.name }

            }
            locationSearchJob = deferred.job
            deferred.invokeOnCompletion {
                locationSearchJob = null
            }

            deferred.await()
        }
    }

    sealed interface State {
        data object Empty : State
        data object NoLocation : State
        data class Ready(
            val location: Location,
            val updatedAt: Instant,
            val conditions: Conditions,
            val forecast: List<Conditions>
        ) : State
    }
}

