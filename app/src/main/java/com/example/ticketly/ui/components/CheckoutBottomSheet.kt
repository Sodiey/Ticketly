package com.example.ticketly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

sealed interface CheckoutUiState {
    object Idle : CheckoutUiState
    object Loading : CheckoutUiState
    data class Error(val message: String) : CheckoutUiState
}

/** Content-only, hosted by CartScreen's own sheet - mirrors LoginSheetContent. */
@Composable
fun CheckoutSheetContent(
    checkoutState: CheckoutUiState,
    onCheckout: (creditCardNumber: String) -> Unit,
) {
    var cardNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Checkout", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = cardNumber,
            onValueChange = { cardNumber = it },
            label = { Text("Card number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        if (checkoutState is CheckoutUiState.Error) {
            Text(checkoutState.message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { onCheckout(cardNumber) },
            enabled = checkoutState !is CheckoutUiState.Loading && cardNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (checkoutState is CheckoutUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Pay")
            }
        }
    }
}
