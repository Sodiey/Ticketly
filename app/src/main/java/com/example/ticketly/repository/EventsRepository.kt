package com.example.ticketly.repository

import com.example.ticketly.model.Event

interface EventsRepository {
    /**
     * All events in the catalog. Failure covers both transport-level errors
     * (no connection, server unreachable) and the API's own `error` field
     * (e.g. the mocked server failure rate) - the ViewModel doesn't need to
     * tell those apart, so both collapse into [Result.failure].
     */
    suspend fun getEvents(): Result<List<Event>>

    /** Same failure handling as [getEvents]; "not found" is also reported as a failure. */
    suspend fun getEvent(id: String): Result<Event>

    /** Events whose title contains [query] (case-insensitive). Caller should use [getEvents] for a blank query. */
    suspend fun searchEvents(query: String): Result<List<Event>>

    /**
     * Same catalog as [getEvents], but prefers an in-memory cache warmed by the last
     * successful [getEvents] call, falling back to a real fetch on a cache miss.
     * For secondary lookups (e.g. computing similar events on the Detail screen) that
     * don't need up-to-the-second freshness - the Events screen itself should always
     * call [getEvents] directly, never this.
     */
    suspend fun getCachedEvents(): Result<List<Event>>
}
