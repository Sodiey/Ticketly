package com.example.ticketly.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One entry per day from [from] up to (and including) [months] months ahead. */
fun upcomingDates(months: Long, from: LocalDate = LocalDate.now()): List<LocalDate> {
    val end = from.plusMonths(months)
    val dayCount = ChronoUnit.DAYS.between(from, end)
    return (0..dayCount).map { from.plusDays(it) }
}

fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return Instant.ofEpochSecond(this)
        .atZone(zoneId)
        .toLocalDate()
}

/** Parses an ISO-8601 instant string (e.g. an [com.example.ticketly.model.Event.startsAt]) into a local calendar date. */
fun String.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return Instant.parse(this)
        .atZone(zoneId)
        .toLocalDate()
}