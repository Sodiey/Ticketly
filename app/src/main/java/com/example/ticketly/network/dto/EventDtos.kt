package com.example.ticketly.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventsResponseDto(val events: List<EventDto>)

@Serializable
data class EventResponseDto(val event: EventDto)

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val startsAt: String,
    val venue: VenueDto,
    val tickets: List<TicketDto> = emptyList(),
    val imageUrl: String,
)

@Serializable
data class VenueDto(
    val id: String,
    val name: String,
    val imageUrl: String,
)

@Serializable
data class TicketDto(
    val id: String,
    val label: String,
    val priceCents: Int,
    val currency: String,
    val quantityAvailable: Int,
)
