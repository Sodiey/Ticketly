package com.example.ticketly.model

/**
 * Plain domain models, independent of the REST API's wire DTOs. The repository is
 * the only place that maps between the two, so the rest of the app never depends
 * on network response shapes.
 */
data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val startsAt: String,
    val imageUrl: String,
    val venue: Venue,
    val tickets: List<Ticket>,
)

data class Venue(
    val id: String,
    val name: String,
    val imageUrl: String,
)

data class Ticket(
    override val id: String,
    override val label: String,
    override val priceCents: Int,
    override val currency: String,
    val quantityAvailable: Int,
) : TicketBase
