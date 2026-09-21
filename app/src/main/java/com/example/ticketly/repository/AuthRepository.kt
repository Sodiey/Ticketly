package com.example.ticketly.repository

import com.example.ticketly.model.CartLine
import com.example.ticketly.model.OwnedTicket
import com.example.ticketly.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    /** Null when signed out. This is local state - observing it never hits the network. */
    val currentUser: StateFlow<User?>

    suspend fun login(username: String, password: String): Result<Unit>

    suspend fun logout(): Result<Unit>

    /**
     * Updates the signed-in user's cart snapshot in local state. Called by CartRepository
     * after a successful add/remove, so every observer of [currentUser] sees the same cart
     * regardless of which screen triggered the mutation.
     */
    fun updateCart(cart: List<CartLine>)

    /**
     * Appends newly purchased tickets to the signed-in user's wallet in local state.
     * Called by CartRepository after a successful checkout.
     */
    fun addToWallet(tickets: List<OwnedTicket>)
}
