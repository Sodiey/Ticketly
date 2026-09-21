package com.example.ticketly.repository.fakes

import com.example.ticketly.model.CartLine
import com.example.ticketly.model.OwnedTicket
import com.example.ticketly.model.User
import com.example.ticketly.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Hand-written fake for [AuthRepository]; see TESTING.md for why no mocking library is used. */
class FakeAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    var loginResult: Result<Unit> = Result.success(Unit)
    var logoutResult: Result<Unit> = Result.success(Unit)

    /** Lets a test push a login/logout without going through [login]/[logout]'s Result plumbing. */
    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    override suspend fun login(username: String, password: String): Result<Unit> = loginResult

    override suspend fun logout(): Result<Unit> = logoutResult

    override fun updateCart(cart: List<CartLine>) {
        _currentUser.value = _currentUser.value?.copy(cart = cart)
    }

    override fun addToWallet(tickets: List<OwnedTicket>) {
        _currentUser.value = _currentUser.value?.let { it.copy(ticketWallet = it.ticketWallet + tickets) }
    }
}
