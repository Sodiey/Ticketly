package com.example.ticketly.model

data class OwnedTicket(
    override val id: String,
    override val label: String,
    override val priceCents: Int,
    override val currency: String,
    val barcode: String,
) : TicketBase
