package com.example.ticketly.presentation.cart

import app.cash.turbine.test
import com.example.ticketly.MainDispatcherRule
import com.example.ticketly.model.CartLine
import com.example.ticketly.model.User
import com.example.ticketly.repository.fakes.FakeAuthRepository
import com.example.ticketly.repository.fakes.FakeCartRepository
import com.example.ticketly.ui.components.CheckoutUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCartRepository: FakeCartRepository

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeCartRepository = FakeCartRepository()
    }

    private fun createViewModel() = CartViewModel(fakeAuthRepository, fakeCartRepository)

    @Test
    fun `cart is empty when signed out`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.cart.value.isEmpty())
    }

    @Test
    fun `cart mirrors currentUser's cart, including a push after construction`() = runTest {
        val viewModel = createViewModel()

        viewModel.cart.test {
            assertEquals(emptyList<CartLine>(), awaitItem())

            fakeAuthRepository.setCurrentUser(user(cart = listOf(cartLine("tkt_1"))))
            assertEquals(listOf(cartLine("tkt_1")), awaitItem())

            // A second push proves this is a live observation of the shared flow, not a
            // one-time read taken at construction - this is exactly the distinction the
            // cart/wallet single-source-of-truth bug turned on.
            fakeAuthRepository.setCurrentUser(user(cart = listOf(cartLine("tkt_1"), cartLine("tkt_2"))))
            assertEquals(listOf(cartLine("tkt_1"), cartLine("tkt_2")), awaitItem())
        }
    }

    @Test
    fun `removeTicket delegates to CartRepository with the given ticket id`() = runTest {
        val viewModel = createViewModel()

        viewModel.removeTicket("tkt_1")
        advanceUntilIdle()

        assertEquals(listOf("tkt_1"), fakeCartRepository.removedTicketIds)
    }

    @Test
    fun `checkout success resets checkoutState to Idle`() = runTest {
        val viewModel = createViewModel()

        viewModel.checkout("4242424242424242")
        advanceUntilIdle()

        assertEquals(CheckoutUiState.Idle, viewModel.checkoutState.value)
    }

    @Test
    fun `checkout failure surfaces the repository's error message`() = runTest {
        fakeCartRepository.checkoutResult = Result.failure(RuntimeException("Card declined"))
        val viewModel = createViewModel()

        viewModel.checkout("4242424242424242")
        advanceUntilIdle()

        val errorState = viewModel.checkoutState.value as CheckoutUiState.Error
        assertEquals("Card declined", errorState.message)
    }

    @Test
    fun `checkout success emits checkoutSuccess once`() = runTest {
        val viewModel = createViewModel()

        viewModel.checkoutSuccess.test {
            viewModel.checkout("4242424242424242")
            advanceUntilIdle()
            awaitItem()
        }
    }

    @Test
    fun `checkout failure never emits checkoutSuccess`() = runTest {
        fakeCartRepository.checkoutResult = Result.failure(RuntimeException("Card declined"))
        val viewModel = createViewModel()

        viewModel.checkoutSuccess.test {
            viewModel.checkout("4242424242424242")
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    private fun user(cart: List<CartLine> = emptyList()) = User(
        id = "user_1",
        firstName = "Test",
        lastName = "User",
        username = "testuser",
        cart = cart,
    )

    private fun cartLine(ticketId: String) = CartLine(
        ticketId = ticketId,
        eventId = "evt_1",
        eventTitle = "Test Concert",
        ticketLabel = "General Admission",
        priceCents = 2500,
        currency = "USD",
    )
}
