package dev.cyberdeck.wetbulbapp.openmeteo

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

}