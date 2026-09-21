package com.example.ticketly.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class LoginResponseDto(val accessToken: String)

@Serializable
data class MeResponseDto(val user: UserDto)

@Serializable
data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val username: String,
    val cart: List<CartLineDto> = emptyList(),
    val walletTicketCount: Int = 0,
    val ticketWallet: List<OwnedTicketDto> = emptyList(),
    val creditCardNumber: String? = null,
)
