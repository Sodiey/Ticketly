package com.example.ticketly.model

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val cart: List<CartLine> = emptyList(),
    val ticketWallet: List<OwnedTicket> = emptyList(),
)
