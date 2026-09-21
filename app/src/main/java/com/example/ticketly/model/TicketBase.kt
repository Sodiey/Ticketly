package com.example.ticketly.model

/**
 * Fields shared by every ticket-like domain model. Data classes can't extend each
 * other in Kotlin, so this is a shared contract they implement instead - lets code
 * that only cares about label/price (e.g. a shared display row) accept either
 * [Ticket] or [OwnedTicket] without overloads.
 */
sealed interface TicketBase {
    val id: String
    val label: String
    val priceCents: Int
    val currency: String
}
