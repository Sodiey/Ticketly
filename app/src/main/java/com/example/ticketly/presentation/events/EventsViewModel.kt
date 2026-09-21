package com.example.ticketly.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketly.model.Event
import com.example.ticketly.repository.EventsRepository
import com.example.ticketly.utils.toLocalDate
import com.example.ticketly.utils.upcomingDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MILLIS = 300L

// Fixed, not derived from the loaded events - the slider always spans $0-$200.
private val PRICE_RANGE_BOUNDS = 0f..20_000f

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _priceRange = MutableStateFlow(PRICE_RANGE_BOUNDS)
    val priceRange: StateFlow<ClosedFloatingPointRange<Float>> = _priceRange.asStateFlow()
    val priceRangeBounds: ClosedFloatingPointRange<Float> = PRICE_RANGE_BOUNDS

    // Network-backed: refetches (from the catalog or searchEvents) when the query changes.
    private val searchResults: Flow<EventsUiState> = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        .distinctUntilChanged()
        // flatMapLatest cancels the previous in-flight search if the query changes again.
        .flatMapLatest { query -> loadEvents(query) }

    // Local-only: the backend has no date or price filter, so narrowing by either
    // happens client-side over whatever the current search already returned - no
    // extra network call.
    val uiState: StateFlow<EventsUiState> = combine(
        searchResults,
        _selectedDate,
        _priceRange,
    ) { result, date, priceRange ->
        if (result is EventsUiState.Success) {
            val eventsOnDate = result.data.events
//            val eventsOnDate = result.data.events.filter { it.startsAt.toLocalDate() == date }
            val filteredEvents = eventsOnDate.filter { event ->
                // Matches on the event's cheapest ticket - "starting from $X" - not every tier.
                val cheapestPriceCents = event.tickets.minOfOrNull { it.priceCents } ?: return@filter false
                cheapestPriceCents.toFloat() in priceRange
            }
            EventsUiState.Success(EventsUiData(filteredEvents))
        } else {
            result
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState.Loading,
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onPriceRangeApplied(range: ClosedFloatingPointRange<Float>) {
        _priceRange.value = range
    }

    fun onPriceRangeReset() {
        _priceRange.value = PRICE_RANGE_BOUNDS
    }

    private fun loadEvents(query: String) = flow {
        emit(EventsUiState.Loading)
        val result = if (query.isBlank()) {
            eventsRepository.getEvents()
        } else {
            eventsRepository.searchEvents(query.trim())
        }
        emit(
            result.fold(
                onSuccess = { events -> EventsUiState.Success(EventsUiData(events)) },
                onFailure = { EventsUiState.Failure },
            )
        )
    }
}

interface EventsUiState {
    object Loading : EventsUiState
    data class Success(val data: EventsUiData) : EventsUiState
    object Failure : EventsUiState
}

data class EventsUiData(
    val events: List<Event> = emptyList(),
) {
    // Computed once when the ViewModel emits new data, not on every recomposition -
    // sorting by the ISO-8601 startsAt string sorts chronologically too.
    val eventsByDate: List<EventsForDate> = events
        .sortedBy { it.startsAt }
        .groupBy { it.startsAt.toLocalDate() }
        .map { (date, eventsOnDate) -> EventsForDate(date, eventsOnDate) }

    // The date-chip range anchors on the earliest fetched event instead of today, so it
    // actually lines up with what's being shown (falls back to today if there are none).
    val visibleDates: List<LocalDate> = upcomingDates(
        months = 6,
        from = eventsByDate.firstOrNull()?.date ?: LocalDate.now(),
    )
}

data class EventsForDate(
    val date: LocalDate,
    val events: List<Event>,
)
