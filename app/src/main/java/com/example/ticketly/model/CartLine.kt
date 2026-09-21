package com.example.ticketly.model

data class CartLine(
    val ticketId: String,
    val eventId: String,
    val eventTitle: String,
    val ticketLabel: String,
    val priceCents: Int,
    val currency: String,
)
