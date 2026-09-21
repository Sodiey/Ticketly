package com.example.ticketly.repository

import com.example.ticketly.model.CartLine

interface CartRepository {
    suspend fun addTicket(ticketId: String): Result<List<CartLine>>
    suspend fun removeTicket(ticketId: String): Result<List<CartLine>>

    /** Pays for the current cart. On success the server clears it - local state is cleared to match. */
    suspend fun checkout(creditCardNumber: String): Result<Unit>
}
