package com.example.ticketly.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorEnvelopeDto(val error: ErrorDetailDto? = null)

@Serializable
data class ErrorDetailDto(val text: String)
