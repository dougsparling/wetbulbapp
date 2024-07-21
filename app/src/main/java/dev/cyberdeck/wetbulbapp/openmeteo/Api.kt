package dev.cyberdeck.wetbulbapp.openmeteo

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

// https://open-meteo.com/en/docs
interface Api {
    // example query:
    // https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m
    @GET("v1/forecast")
    @Headers("Content-Type: application/json")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,wind_speed_10m,relative_humidity_2m",
        @Query("hourly") hourly: String? = "temperature_2m,wind_speed_10m,relative_humidity_2m",
        @Query("forecast_days") days: Int? = 1
    ): Response<Result>
}

data class Result(
    val current: Current,
    val hourly: Hourly?,
    val utc_offset_seconds: Int
)

// TODO: nasty
data class Current(
    val temperature_2m: Double,
    val wind_speed_10m: Double,
    val relative_humidity_2m: Double
)


// TODO: nasty
data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val wind_speed_10m: List<Double>,
    val relative_humidity_2m: List<Double>
)
