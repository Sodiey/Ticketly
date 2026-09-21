package com.example.ticketly.repository.fakes

import com.example.ticketly.model.CartLine
import com.example.ticketly.repository.CartRepository

/** Hand-written fake for [CartRepository]; see TESTING.md for why no mocking library is used. */
class FakeCartRepository : CartRepository {
    var addTicketResult: Result<List<CartLine>> = Result.success(emptyList())
    var removeTicketResult: Result<List<CartLine>> = Result.success(emptyList())
    var checkoutResult: Result<Unit> = Result.success(Unit)

    val addedTicketIds = mutableListOf<String>()
    val removedTicketIds = mutableListOf<String>()

    override suspend fun addTicket(ticketId: String): Result<List<CartLine>> {
        addedTicketIds.add(ticketId)
        return addTicketResult
    }

    override suspend fun removeTicket(ticketId: String): Result<List<CartLine>> {
        removedTicketIds.add(ticketId)
        return removeTicketResult
    }

    override suspend fun checkout(creditCardNumber: String): Result<Unit> {
        return checkoutResult
    }
}
