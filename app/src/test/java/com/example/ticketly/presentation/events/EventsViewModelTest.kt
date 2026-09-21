package com.example.ticketly.presentation.events

import app.cash.turbine.test
import com.example.ticketly.MainDispatcherRule
import com.example.ticketly.model.Event
import com.example.ticketly.model.Ticket
import com.example.ticketly.model.Venue
import com.example.ticketly.repository.fakes.FakeEventsRepository
import com.example.ticketly.utils.toLocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeEventsRepository: FakeEventsRepository

    @Before
    fun setUp() {
        fakeEventsRepository = FakeEventsRepository()
    }

    private fun createViewModel() = EventsViewModel(fakeEventsRepository)

    @Test
    fun `blank query fetches the catalog instead of searching`() = runTest {
        fakeEventsRepository.events = listOf(event(id = "1", title = "Rock Night"))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(EventsUiState.Loading, awaitItem())
            advanceUntilIdle()
            val success = awaitItem() as EventsUiState.Success
            assertEquals(1, success.data.events.size)
        }

        assertEquals(1, fakeEventsRepository.getEventsCallCount)
        assertTrue(fakeEventsRepository.searchEventsQueries.isEmpty())
    }

    @Test
    fun `non-blank query searches with the trimmed query`() = runTest {
        fakeEventsRepository.events = listOf(event(id = "1", title = "Rock Night"))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial Loading
            advanceUntilIdle()
            awaitItem() // Success from the initial blank-query fetch

            viewModel.onSearchQueryChanged("  rock  ")
            advanceUntilIdle()

            awaitItem() // Loading while the search is in flight
            val success = awaitItem() as EventsUiState.Success
            assertEquals(1, success.data.events.size)
        }

        assertEquals(listOf("rock"), fakeEventsRepository.searchEventsQueries)
    }

    @Test
    fun `rapid query changes within the debounce window only search once`() = runTest {
        fakeEventsRepository.events = listOf(event(id = "1", title = "Rock Night"))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial Loading
            advanceUntilIdle()
            awaitItem() // Success from the initial blank-query fetch

            viewModel.onSearchQueryChanged("r")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("ro")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("rock")
            advanceUntilIdle()

            awaitItem() // Loading
            awaitItem() // Success
        }

        assertEquals(listOf("rock"), fakeEventsRepository.searchEventsQueries)
    }

    @Test
    fun `changing the query cancels the previous in-flight search`() = runTest {
        val rockEvent = event(id = "1", title = "Rock Night")
        val popEvent = event(id = "2", title = "Pop Party")
        fakeEventsRepository.events = listOf(rockEvent, popEvent)
        fakeEventsRepository.searchEventsDelayMillis = { query -> if (query == "rock") 1_000L else 0L }
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial Loading
            advanceUntilIdle()
            awaitItem() // Success from the initial blank-query fetch

            viewModel.onSearchQueryChanged("rock")
            advanceTimeBy(301) // past the debounce; "rock" is now in flight, held by its artificial delay

            viewModel.onSearchQueryChanged("pop")
            advanceUntilIdle() // past "pop"'s debounce; cancels "rock" and resolves "pop"

            awaitItem() // Loading, once the "rock" search starts
            val success = awaitItem() as EventsUiState.Success
            assertEquals(listOf(popEvent), success.data.events)
        }

        // Both were called, but only "pop"'s result ever reached uiState.
        assertEquals(listOf("rock", "pop"), fakeEventsRepository.searchEventsQueries)
    }

    @Test
    fun `repository failure surfaces as Failure`() = runTest {
        fakeEventsRepository.getEventsShouldFail = true
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial Loading
            advanceUntilIdle()
            assertEquals(EventsUiState.Failure, awaitItem())
        }
    }

    @Test
    fun `price range filters events by their cheapest ticket, and reset restores them`() = runTest {
        val cheapEvent = event(id = "1", title = "Cheap Show", tickets = listOf(ticket(1_000)))
        val expensiveEvent = event(id = "2", title = "Expensive Show", tickets = listOf(ticket(15_000)))
        fakeEventsRepository.events = listOf(cheapEvent, expensiveEvent)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial Loading
            advanceUntilIdle()
            awaitItem() // Success, unfiltered

            viewModel.onPriceRangeApplied(0f..2_000f)
            advanceUntilIdle()
            val filtered = awaitItem() as EventsUiState.Success
            assertEquals(listOf(cheapEvent), filtered.data.events)

            viewModel.onPriceRangeReset()
            advanceUntilIdle()
            val reset = awaitItem() as EventsUiState.Success
            assertEquals(listOf(cheapEvent, expensiveEvent), reset.data.events)
        }
    }

    @Test
    fun `eventsByDate groups and sorts out-of-order events by day`() {
        // Same instant as `early`, not just the same date, so the grouping is independent of
        // the test machine's default zone (crossing local midnight would otherwise split them).
        val early = event(id = "1", title = "Early Show", startsAt = "2026-01-01T20:00:00Z")
        val late = event(id = "2", title = "Late Show", startsAt = "2026-03-01T20:00:00Z")
        val sameDayAsEarly = event(id = "3", title = "Early Show 2", startsAt = "2026-01-01T20:00:00Z")

        val data = EventsUiData(events = listOf(late, early, sameDayAsEarly))

        assertEquals(2, data.eventsByDate.size)
        assertEquals(2, data.eventsByDate.first().events.size)
        assertTrue(data.eventsByDate.first().date.isBefore(data.eventsByDate.last().date))
    }

    @Test
    fun `visibleDates anchors on the earliest event's date`() {
        val laterEvent = event(id = "1", title = "Later", startsAt = "2027-05-01T20:00:00Z")

        val data = EventsUiData(events = listOf(laterEvent))

        assertEquals(laterEvent.startsAt.toLocalDate(), data.visibleDates.first())
    }

    @Test
    fun `visibleDates falls back to today when there are no events`() {
        val data = EventsUiData(events = emptyList())

        assertEquals(LocalDate.now(), data.visibleDates.first())
    }

    private fun event(
        id: String,
        title: String,
        startsAt: String = "2026-01-01T20:00:00Z",
        tickets: List<Ticket> = emptyList(),
    ) = Event(
        id = id,
        title = title,
        description = null,
        startsAt = startsAt,
        imageUrl = "",
        venue = Venue(id = "v1", name = "Venue", imageUrl = ""),
        tickets = tickets,
    )

    private fun ticket(priceCents: Int) =
        Ticket(id = "t1", label = "GA", priceCents = priceCents, currency = "USD", quantityAvailable = 1)
}
