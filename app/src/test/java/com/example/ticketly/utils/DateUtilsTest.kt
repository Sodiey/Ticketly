package com.example.ticketly.utils

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `upcomingDates returns one entry per day inclusive of both ends`() {
        val from = LocalDate.of(2026, 1, 1)

        val dates = upcomingDates(months = 1, from = from)

        assertEquals(from, dates.first())
        assertEquals(from.plusMonths(1), dates.last())
        assertEquals(32, dates.size) // Jan 1 through Feb 1 inclusive
    }

    @Test
    fun `upcomingDates with zero months returns just the from date`() {
        val from = LocalDate.of(2026, 3, 15)

        val dates = upcomingDates(months = 0, from = from)

        assertEquals(listOf(from), dates)
    }

    @Test
    fun `upcomingDates defaults from to today`() {
        val dates = upcomingDates(months = 6)

        assertEquals(LocalDate.now(), dates.first())
    }

    @Test
    fun `Long toLocalDate converts an epoch second in the given zone`() {
        // 2026-01-01T00:30:00Z
        val epochSeconds = 1767227400L

        val date = epochSeconds.toLocalDate(zoneId = ZoneId.of("UTC"))

        assertEquals(LocalDate.of(2026, 1, 1), date)
    }

    @Test
    fun `Long toLocalDate can land on a different calendar day depending on zone`() {
        // 2026-01-01T00:30:00Z is still Dec 31 in a negative-offset zone.
        val epochSeconds = 1767227400L

        val date = epochSeconds.toLocalDate(zoneId = ZoneId.of("America/Los_Angeles"))

        assertEquals(LocalDate.of(2025, 12, 31), date)
    }

    @Test
    fun `String toLocalDate parses an ISO-8601 instant`() {
        val date = "2026-06-15T20:00:00Z".toLocalDate(zoneId = ZoneId.of("UTC"))

        assertEquals(LocalDate.of(2026, 6, 15), date)
    }
}
