package com.example.ticketly.repository.fakes

import com.example.ticketly.model.Event
import com.example.ticketly.repository.EventsRepository
import kotlinx.coroutines.delay

/** Hand-written fake for [EventsRepository]; see TESTING.md for why no mocking library is used. */
class FakeEventsRepository : EventsRepository {
    var events: List<Event> = emptyList()

    var getEventsShouldFail = false
    var getEventShouldFail = false
    var searchEventsShouldFail = false
    var getCachedEventsShouldFail = false

    /** Lets a test hold a search in flight (e.g. to exercise `flatMapLatest` cancellation). */
    var searchEventsDelayMillis: (String) -> Long = { 0L }

    var getEventsCallCount = 0
    var getCachedEventsCallCount = 0
    val searchEventsQueries = mutableListOf<String>()

    override suspend fun getEvents(): Result<List<Event>> {
        getEventsCallCount++
        return if (getEventsShouldFail) {
            Result.failure(RuntimeException("getEvents failed"))
        } else {
            Result.success(events)
        }
    }

    override suspend fun getEvent(id: String): Result<Event> {
        if (getEventShouldFail) return Result.failure(RuntimeException("getEvent failed"))
        val event = events.find { it.id == id } ?: return Result.failure(NoSuchElementException("Event $id not found"))
        return Result.success(event)
    }

    override suspend fun searchEvents(query: String): Result<List<Event>> {
        searchEventsQueries.add(query)
        delay(searchEventsDelayMillis(query))
        return if (searchEventsShouldFail) {
            Result.failure(RuntimeException("searchEvents failed"))
        } else {
            Result.success(events.filter { it.title.contains(query, ignoreCase = true) })
        }
    }

    override suspend fun getCachedEvents(): Result<List<Event>> {
        getCachedEventsCallCount++
        return if (getCachedEventsShouldFail) {
            Result.failure(RuntimeException("getCachedEvents failed"))
        } else {
            Result.success(events)
        }
    }
}
