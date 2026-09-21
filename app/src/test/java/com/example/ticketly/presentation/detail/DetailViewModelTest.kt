package com.example.ticketly.presentation.detail

import app.cash.turbine.test
import com.example.ticketly.MainDispatcherRule
import com.example.ticketly.model.Event
import com.example.ticketly.model.Venue
import com.example.ticketly.repository.fakes.FakeCartRepository
import com.example.ticketly.repository.fakes.FakeEventsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeEventsRepository: FakeEventsRepository
    private lateinit var fakeCartRepository: FakeCartRepository

    @Before
    fun setUp() {
        fakeEventsRepository = FakeEventsRepository()
        fakeCartRepository = FakeCartRepository()
    }

    private fun createViewModel(detailId: String) =
        DetailViewModel(detailId, fakeEventsRepository, fakeCartRepository)

    @Test
    fun `success path populates the event and its similar events`() = runTest {
        val main = event(id = "1", title = "Rock Night")
        val similar = event(id = "2", title = "Rock Fest")
        fakeEventsRepository.events = listOf(main, similar)

        val viewModel = createViewModel("1")
        advanceUntilIdle()

        val success = viewModel.uiState.value as DetailUiState.Success
        assertEquals(main, success.event)
        assertEquals(listOf(similar), success.similarEvents)
    }

    @Test
    fun `getEvent failure surfaces as Failure`() = runTest {
        fakeEventsRepository.getEventShouldFail = true

        val viewModel = createViewModel("1")
        advanceUntilIdle()

        assertEquals(DetailUiState.Failure, viewModel.uiState.value)
    }

    @Test
    fun `getCachedEvents failure leaves similarEvents empty without failing the screen`() = runTest {
        val main = event(id = "1", title = "Rock Night")
        fakeEventsRepository.events = listOf(main)
        fakeEventsRepository.getCachedEventsShouldFail = true

        val viewModel = createViewModel("1")
        advanceUntilIdle()

        val success = viewModel.uiState.value as DetailUiState.Success
        assertEquals(main, success.event)
        assertTrue(success.similarEvents.isEmpty())
    }

    @Test
    fun `similar events match by first-word genre, exclude self, and cap at 5`() = runTest {
        val main = event(id = "main", title = "Rock Night")
        val sameGenre = (1..6).map { event(id = "rock-$it", title = "Rock Show $it") }
        val differentGenre = event(id = "pop-1", title = "Pop Party")
        fakeEventsRepository.events = listOf(main) + sameGenre + differentGenre

        val viewModel = createViewModel("main")
        advanceUntilIdle()

        val success = viewModel.uiState.value as DetailUiState.Success
        assertEquals(5, success.similarEvents.size)
        assertTrue(success.similarEvents.none { it.id == main.id })
        assertTrue(success.similarEvents.all { it.title.startsWith("Rock") })
    }

    @Test
    fun `addTicket success clears loading state and navigates to cart once`() = runTest {
        fakeEventsRepository.events = listOf(event(id = "1", title = "Rock Night"))
        val viewModel = createViewModel("1")
        advanceUntilIdle()

        viewModel.navigateToCart.test {
            viewModel.addTicket("ticket-1")
            advanceUntilIdle()
            awaitItem()
        }

        assertEquals(AddToCartUiState.Idle, viewModel.addToCartState.value)
        assertEquals(listOf("ticket-1"), fakeCartRepository.addedTicketIds)
    }

    @Test
    fun `addTicket failure surfaces the repository's error message`() = runTest {
        fakeEventsRepository.events = listOf(event(id = "1", title = "Rock Night"))
        fakeCartRepository.addTicketResult = Result.failure(RuntimeException("Sold out"))
        val viewModel = createViewModel("1")
        advanceUntilIdle()

        viewModel.addTicket("ticket-1")
        advanceUntilIdle()

        val errorState = viewModel.addToCartState.value as AddToCartUiState.Error
        assertEquals("Sold out", errorState.message)
    }

    private fun event(
        id: String,
        title: String,
        startsAt: String = "2026-01-01T20:00:00Z",
    ) = Event(
        id = id,
        title = title,
        description = null,
        startsAt = startsAt,
        imageUrl = "",
        venue = Venue(id = "v1", name = "Venue", imageUrl = ""),
        tickets = emptyList(),
    )
}
