package dev.cyberdeck.wetbulbapp.openmeteo

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalField
import java.time.temporal.TemporalUnit
import kotlin.math.atan
import kotlin.math.pow

class OpenMeteoService {

    suspend fun forecast(location: Location): Forecast {
        val res = api.forecast(location.lat, location.long, days = 2)

        val current = res.takeIf { it.isSuccessful }?.body()?.current
            ?: error("no current weather @ $location")
        val now = LocalDateTime.now()

        return Forecast(
            current = Conditions(
                temperature = current.temperature_2m,
                humidity = current.relative_humidity_2m,
                wind = current.wind_speed_10m,
                time = now,
            ),
            forecast = res.body()?.hourly?.let { hourly ->
                hourly.time
                    .mapIndexed { index, time ->
                        // TODO: maybe use location local datetime?
                        val instant = LocalDateTime.parse(time).atOffset(ZoneOffset.UTC).toLocalDateTime()
                        Conditions(
                            temperature = hourly.temperature_2m[index],
                            humidity = hourly.relative_humidity_2m[index],
                            wind = hourly.wind_speed_10m[index],
                            time = instant
                        )
                    }
                    .filter { it.time.isAfter(now.minusHours(1)) }
                    .sortedBy { it.time }
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
    val name: String,
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
    val time: LocalDateTime,
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

object TestData {
    val increasingTemps = (24 until 48).mapIndexed { index, temp ->
        Conditions(
            temperature = temp.toDouble(),
            humidity = 50.0 + temp,
            wind = 5.0,
            time = LocalDateTime.now().plus(index.toLong(), ChronoUnit.HOURS)
        )
    }

    val shibuya = Location(
        name = "Shibuya",
        lat = 35.661777,
        long = 139.704056
    )

    val winnipeg = Location(
        name = "Winnipeg",
        lat = 49.895138,
        long = -97.138374
    )

    val vancouver = Location(
        name = "Vancouver",
        lat = 49.282730,
        long = -123.120735
    )

    val locations = listOf(
        shibuya, winnipeg, vancouver
    )
}