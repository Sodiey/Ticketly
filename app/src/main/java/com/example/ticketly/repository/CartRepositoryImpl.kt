package com.example.ticketly.repository

import com.example.ticketly.model.CartLine
import com.example.ticketly.model.OwnedTicket
import com.example.ticketly.network.TicketlyApiService
import com.example.ticketly.network.apiCall
import com.example.ticketly.network.dto.AddCartItemRequestDto
import com.example.ticketly.network.dto.CartLineDto
import com.example.ticketly.network.dto.CheckoutRequestDto
import com.example.ticketly.network.dto.OwnedTicketDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val api: TicketlyApiService,
    private val authRepository: AuthRepository,
) : CartRepository {

    override suspend fun addTicket(ticketId: String): Result<List<CartLine>> =
        apiCall { api.addCartItem(AddCartItemRequestDto(ticketId)).cart.lines.map { it.toDomain() } }
            .onSuccess { authRepository.updateCart(it) }

    override suspend fun removeTicket(ticketId: String): Result<List<CartLine>> =
        apiCall { api.removeCartItem(ticketId).cart.lines.map { it.toDomain() } }
            .onSuccess { authRepository.updateCart(it) }

    override suspend fun checkout(creditCardNumber: String): Result<Unit> =
        apiCall { api.checkout(CheckoutRequestDto(creditCardNumber)).purchasedTickets.map { it.toDomain() } }
            .onSuccess {
                authRepository.addToWallet(it)
                authRepository.updateCart(emptyList())
            }
            .map { }

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
