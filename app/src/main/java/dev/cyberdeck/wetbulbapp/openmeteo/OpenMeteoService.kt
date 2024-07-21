package dev.cyberdeck.wetbulbapp.openmeteo

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.atan
import kotlin.math.pow

class OpenMeteoService {

    suspend fun current(location: Location): Conditions {
        val res = api.forecast(location.lat, location.long)
        val body = res.body()
        require(res.isSuccessful && body != null && body.current != null) { "no current weather @ $location"}
        return Conditions(
            temperature = body.current.temperature_2m,
            humidity = body.current.relative_humidity_2m,
            wind = body.current.wind_speed_10m
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

data class Conditions(
    val temperature: Double,
    val humidity: Double,
    val wind: Double
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