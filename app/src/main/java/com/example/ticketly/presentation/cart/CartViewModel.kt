package com.example.ticketly.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketly.model.CartLine
import com.example.ticketly.repository.AuthRepository
import com.example.ticketly.repository.CartRepository
import com.example.ticketly.ui.components.CheckoutUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {
    // AuthRepository.currentUser is the single source of truth for the cart - CartRepository
    // writes every add/remove/checkout result back into it, so this stays correct regardless of
    // which screen triggered the mutation (e.g. buying from the Detail screen).
    val cart: StateFlow<List<CartLine>> = authRepository.currentUser
        .map { it?.cart ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

    // One-shot: CartScreen navigates home (clearing the back stack) when this fires,
    // instead of the ViewModel holding a navigation callback - same pattern as
    // DetailViewModel.navigateToCart.
    private val _checkoutSuccess = MutableSharedFlow<Unit>()
    val checkoutSuccess: SharedFlow<Unit> = _checkoutSuccess.asSharedFlow()

    fun removeTicket(ticketId: String) {
        viewModelScope.launch {
            cartRepository.removeTicket(ticketId)
        }
    }

    fun checkout(creditCardNumber: String) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutUiState.Loading
            cartRepository.checkout(creditCardNumber)
                .onSuccess {
                    _checkoutState.value = CheckoutUiState.Idle
                    _checkoutSuccess.emit(Unit)
                }
                .onFailure { _checkoutState.value = CheckoutUiState.Error(it.message ?: "Checkout failed") }
        }
    }
}
