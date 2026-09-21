package com.example.ticketly.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketly.model.Event
import com.example.ticketly.repository.CartRepository
import com.example.ticketly.repository.EventsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted val detailId: String,
    private val eventsRepository: EventsRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _addToCartState = MutableStateFlow<AddToCartUiState>(AddToCartUiState.Idle)
    val addToCartState: StateFlow<AddToCartUiState> = _addToCartState.asStateFlow()

    // One-shot: DetailScreen navigates to Cart when this fires, instead of the
    // ViewModel holding a navigation callback.
    private val _navigateToCart = MutableSharedFlow<Unit>()
    val navigateToCart: SharedFlow<Unit> = _navigateToCart.asSharedFlow()

    init {
        viewModelScope.launch {
            eventsRepository.getEvent(detailId)
                .onSuccess { event ->
                    // Similar events failing shouldn't block showing the event itself,
                    // so a catalog fetch failure just means an empty list here.
                    val similarEvents = eventsRepository.getCachedEvents()
                        .getOrDefault(emptyList())
                        .similarTo(event)
                    _uiState.update { DetailUiState.Success(event, similarEvents) }
                }
                .onFailure { _uiState.update { DetailUiState.Failure } }
        }
    }

    fun addTicket(ticketId: String) {
        viewModelScope.launch {
            _addToCartState.value = AddToCartUiState.Loading
            cartRepository.addTicket(ticketId)
                .onSuccess {
                    _addToCartState.value = AddToCartUiState.Idle
                    _navigateToCart.emit(Unit)
                }
                .onFailure {
                    _addToCartState.value = AddToCartUiState.Error(it.message ?: "Couldn't add ticket")
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            detailId: String,
        ): DetailViewModel
    }
}

interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val event: Event, val similarEvents: List<Event> = emptyList()) : DetailUiState
    object Failure : DetailUiState
}

private const val SIMILAR_EVENTS_LIMIT = 5

/**
 * "Similar" = same genre, taken as the title's first word (e.g. "Rock Night 40:
 * Spring Series" -> "Rock"). The catalog has no dedicated genre/tag field, but this
 * mock dataset's titles consistently follow that convention.
 */
private fun List<Event>.similarTo(event: Event): List<Event> {
    val genre = event.title.substringBefore(' ').lowercase()
    return filter { it.id != event.id && it.title.substringBefore(' ').lowercase() == genre }
        .take(SIMILAR_EVENTS_LIMIT)
}

sealed interface AddToCartUiState {
    object Idle : AddToCartUiState
    object Loading : AddToCartUiState
    data class Error(val message: String) : AddToCartUiState
}
