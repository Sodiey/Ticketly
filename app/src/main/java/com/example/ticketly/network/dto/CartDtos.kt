package com.example.ticketly.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CartLineDto(
    val ticketId: String,
    val eventId: String,
    val eventTitle: String,
    val ticketLabel: String,
    val priceCents: Int,
    val currency: String,
)

@Serializable
data class CartDto(val lines: List<CartLineDto> = emptyList())

@Serializable
data class CartResponseDto(val cart: CartDto)

@Serializable
data class AddCartItemRequestDto(val ticketId: String)

@Serializable
data class CheckoutRequestDto(val creditCardNumber: String)

@Serializable
data class CheckoutResponseDto(val purchasedTickets: List<OwnedTicketDto> = emptyList())

@Serializable
data class OwnedTicketDto(
    val id: String,
    val label: String,
    val priceCents: Int,
    val currency: String,
    val barcode: String,
)
