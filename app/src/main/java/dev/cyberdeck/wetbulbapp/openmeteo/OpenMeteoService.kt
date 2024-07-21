package dev.cyberdeck.wetbulbapp.openmeteo

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.atan
import kotlin.math.pow

class OpenMeteoService {

    suspend fun forecast(location: Location): Forecast {
        val res = api.forecast(location.lat, location.long, days = 2)

        val current = res.takeIf { it.isSuccessful }?.body()?.current
            ?: error("no current weather @ $location")
        val now = Instant.now()
        val offset = ZoneOffset.ofTotalSeconds(res.body()!!.utc_offset_seconds)

        return Forecast(
            current = Conditions(
                temperature = current.temperature_2m,
                humidity = current.relative_humidity_2m,
                wind = current.wind_speed_10m
            ),
            forecast = res.body()?.hourly?.let { hourly ->
                hourly.time
                    .mapIndexed { index, time ->
                        val instant = LocalDateTime.parse(time).toInstant(offset)
                        Conditions(
                            temperature = hourly.temperature_2m[index],
                            humidity = hourly.relative_humidity_2m[index],
                            wind = hourly.wind_speed_10m[index],
                            offsetHours = now.until(instant, ChronoUnit.HOURS).toInt()
                        )
                    }
                    .filter { it.offsetHours > 0 }
                    .sortedBy { it.offsetHours }
                    .take(24)
            } ?: emptyList()
        )
    }

    companion object {
        val api: Api = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api::class.java)
    }
}

data class Location(
    val lat: Double,
    val long: Double
)

data class Forecast(
    val current: Conditions,
    val forecast: List<Conditions>
)

data class Conditions(
    val temperature: Double,
    val humidity: Double,
    val wind: Double,
    val offsetHours: Int = 0
) {
    /**
     * Estimates wet-bulb temperature using the regression described here:
     * https://journals.ametsoc.org/view/journals/apme/50/11/jamc-d-11-0143.1.xml
     *
     * T. = T atan[0.151 977(RH% + 8.313 659) 121 + atan(T + RH%) - atan(RH% - 1.676331)
     *      + 0.003 918 38(RH%)32 atan(0.023 101RH%) - 4.686 035.
     *
     * TODO: might be nice to find a regression that uses wind speed as well
     */
    val wetBulbEstimate by lazy {
        val term1 = temperature * atan(0.151977 * (humidity + 8.313659).pow(0.5))
        val term2 = atan(temperature + humidity)
        val term3 = atan(humidity - 1.676331)
        val term4 = 0.00391838 * humidity.pow(1.5) * atan(0.023101 * humidity)
        val term5 = 4.686035

        term1 + term2 - term3 + term4 - term5
    }
}