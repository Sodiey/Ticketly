package com.example.ticketly.presentation.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ticketly.model.CartLine
import com.example.ticketly.model.User
import com.example.ticketly.presentation.auth.AuthViewModel
import com.example.ticketly.ui.components.CheckoutSheetContent
import com.example.ticketly.ui.components.CheckoutUiState
import com.example.ticketly.ui.components.LoginSheetContent
import com.example.ticketly.ui.components.LoginUiState
import com.example.ticketly.utils.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartRoute(
    viewModel: CartViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onCheckoutSuccess: () -> Unit = {},
) {
    val currentUser = authViewModel.currentUser.collectAsStateWithLifecycle()
    val loginState = authViewModel.loginState.collectAsStateWithLifecycle()
    val cart = viewModel.cart.collectAsStateWithLifecycle()
    val checkoutState = viewModel.checkoutState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkoutSuccess.collect { onCheckoutSuccess() }
    }

    CartScreen(
        user = currentUser.value,
        loginState = loginState.value,
        cart = cart.value,
        checkoutState = checkoutState.value,
        onLogin = authViewModel::login,
        onRemoveTicket = viewModel::removeTicket,
        onCheckout = viewModel::checkout,
    )
}

/** Which single bottom sheet (if any) CartScreen is currently showing. */
private sealed interface CartSheet {
    object Login : CartSheet
    object Checkout : CartSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    user: User?,
    loginState: LoginUiState = LoginUiState.Idle,
    cart: List<CartLine> = emptyList(),
    checkoutState: CheckoutUiState = CheckoutUiState.Idle,
    onLogin: (String, String) -> Unit = { _, _ -> },
    onRemoveTicket: (String) -> Unit = {},
    onCheckout: (creditCardNumber: String) -> Unit = {},
) {
    var activeSheet by remember { mutableStateOf<CartSheet?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(user) {
        if (user != null && activeSheet == CartSheet.Login) activeSheet = null
    }
    // A successful checkout clears the cart server-side (and so locally too) - that's the signal to dismiss.
    LaunchedEffect(cart) {
        if (cart.isEmpty() && activeSheet == CartSheet.Checkout) activeSheet = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (user == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Log in to view your cart")
                Button(onClick = { activeSheet = CartSheet.Login }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Login")
                }
            }
        } else if (cart.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your cart is empty")
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // No key: ticketId isn't unique here - buying the same ticket twice
                    // creates two CartLines with the same ticketId (one entry per unit,
                    // per the backend's own cart model), so a key on it would collide.
                    items(cart) { line ->
                        CartLineRow(line = line, onRemove = { onRemoveTicket(line.ticketId) })
                        HorizontalDivider()
                    }
                }
                Button(
                    onClick = { activeSheet = CartSheet.Checkout },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text("Checkout")
                }
            }
        }
    }

    val sheetToShow = activeSheet
    if (sheetToShow != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
        ) {
            when (sheetToShow) {
                CartSheet.Login -> LoginSheetContent(loginState = loginState, onLogin = onLogin)
                CartSheet.Checkout -> CheckoutSheetContent(checkoutState = checkoutState, onCheckout = onCheckout)
            }
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = line.eventTitle, style = MaterialTheme.typography.bodyLarge)
            Text(text = line.ticketLabel, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formatPrice(line.priceCents, line.currency),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onRemove) {
            Text("Remove")
        }
    }
}
