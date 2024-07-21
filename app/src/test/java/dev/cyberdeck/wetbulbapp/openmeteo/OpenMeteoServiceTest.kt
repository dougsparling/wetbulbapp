package dev.cyberdeck.wetbulbapp.openmeteo

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoServiceTest {

    @Test
    fun testShibuya() = runBlocking {
        val service = OpenMeteoService()
        val res = service.forecast(
            Location(
                lat = 35.661777,
                long = 139.704056
            )
        )
        val current = res.current
        assertTrue(current.temperature > -10)
        assertTrue(current.temperature < 50)
        assertTrue(current.humidity > 0)
        assertTrue(current.humidity <= 100)
        assertTrue(current.wind >= 0)
        assertTrue(current.wind < 100)

        val forecast = res.forecast
        println(forecast)
        assertTrue(forecast.size >= 24)
        assertTrue(forecast.first().offsetHours >= 0)
    }

    @Test
    fun testWetBulbEstimate() {
        val estimate = Conditions(
            temperature = 26.0,
            humidity = 80.0,
            wind = 0.0
        ).wetBulbEstimate

        assertTrue(estimate > 23.2)
        assertTrue(estimate < 23.3)
    }
}