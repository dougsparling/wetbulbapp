package dev.cyberdeck.wetbulbapp.openmeteo

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoServiceTest {

    @Test
    fun testShibuya() = runBlocking {
        val service = OpenMeteoService()
        val res = service.current(
            Location(
                lat = 35.661777,
                long = 139.704056
            )
        )

        assertTrue(res.temperature > -10)
        assertTrue(res.temperature < 50)
        assertTrue(res.humidity > 0)
        assertTrue(res.humidity <= 100)
        assertTrue(res.wind >= 0)
        assertTrue(res.wind < 100)
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