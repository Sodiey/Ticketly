package com.example.ticketly.repository

import com.example.ticketly.model.Event
import com.example.ticketly.model.Ticket
import com.example.ticketly.model.Venue
import com.example.ticketly.network.TicketlyApiService
import com.example.ticketly.network.apiCall
import com.example.ticketly.network.dto.EventDto
import com.example.ticketly.network.dto.TicketDto
import com.example.ticketly.network.dto.VenueDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepositoryImpl @Inject constructor(
    private val api: TicketlyApiService,
) : EventsRepository {
    // Written by every successful getEvents() call; only getCachedEvents() reads it.
    // The backend loads its catalog from a JSON file once at startup and never mutates
    // it, so this never goes stale for the life of the process.
    private var cachedEvents: List<Event>? = null

    override suspend fun getEvents(): Result<List<Event>> =
        apiCall { api.getEvents().events.map { it.toDomain() } }
            .onSuccess { cachedEvents = it }

    override suspend fun getCachedEvents(): Result<List<Event>> {
        cachedEvents?.let { return Result.success(it) }
        return getEvents()
    }

    override suspend fun getEvent(id: String): Result<Event> =
        apiCall { api.getEvent(id).event.toDomain() }

    override suspend fun searchEvents(query: String): Result<List<Event>> =
        apiCall { api.getEvents(query = query).events.map { it.toDomain() } }

    private fun EventDto.toDomain() = Event(
        id = id,
        title = title,
        description = description,
        startsAt = startsAt,
        imageUrl = imageUrl,
        venue = venue.toDomain(),
        tickets = tickets.map { it.toDomain() },
    )

    private fun VenueDto.toDomain() = Venue(
        id = id,
        name = name,
        imageUrl = imageUrl,
    )

    private fun TicketDto.toDomain() = Ticket(
        id = id,
        label = label,
        priceCents = priceCents,
        currency = currency,
        quantityAvailable = quantityAvailable,
    )
}
