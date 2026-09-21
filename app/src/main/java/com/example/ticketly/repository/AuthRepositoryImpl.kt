package com.example.ticketly.repository

import com.example.ticketly.model.CartLine
import com.example.ticketly.model.OwnedTicket
import com.example.ticketly.model.User
import com.example.ticketly.network.TicketlyApiService
import com.example.ticketly.network.apiCall
import com.example.ticketly.network.dto.CartLineDto
import com.example.ticketly.network.dto.LoginRequestDto
import com.example.ticketly.network.dto.OwnedTicketDto
import com.example.ticketly.network.dto.UserDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: TicketlyApiService,
    private val sessionManager: SessionManager,
) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(username: String, password: String): Result<Unit> {
        val loginResult = apiCall { api.login(LoginRequestDto(username, password)) }
        val accessToken = loginResult.getOrElse { return Result.failure(it) }.accessToken

        sessionManager.setAccessToken(accessToken)
        return refreshCurrentUser()
    }

    override suspend fun logout(): Result<Unit> {
        if (sessionManager.accessToken.value != null) {
            apiCall { api.logout() }.onFailure { return Result.failure(it) }
        }
        sessionManager.clear()
        _currentUser.value = null
        return Result.success(Unit)
    }

    private suspend fun refreshCurrentUser(): Result<Unit> =
        apiCall { api.getCurrentUser().user }
            .map { _currentUser.value = it.toDomain() }

    override fun updateCart(cart: List<CartLine>) {
        _currentUser.update { it?.copy(cart = cart) }
    }

    override fun addToWallet(tickets: List<OwnedTicket>) {
        _currentUser.update { it?.copy(ticketWallet = it.ticketWallet + tickets) }
    }

    private fun UserDto.toDomain() = User(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        cart = cart.map { it.toDomain() },
        ticketWallet = ticketWallet.map { it.toDomain() },
    )

    private fun CartLineDto.toDomain() = CartLine(
        ticketId = ticketId,
        eventId = eventId,
        eventTitle = eventTitle,
        ticketLabel = ticketLabel,
        priceCents = priceCents,
        currency = currency,
    )

    private fun OwnedTicketDto.toDomain() = OwnedTicket(
        id = id,
        label = label,
        priceCents = priceCents,
        currency = currency,
        barcode = barcode,
    )
}
