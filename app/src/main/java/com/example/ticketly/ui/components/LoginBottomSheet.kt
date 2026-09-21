package com.example.ticketly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ticketly.model.User
import com.example.ticketly.utils.formatPrice

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

/** Full sheet for standalone use (e.g. CartScreen, which only ever shows this one sheet). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginBottomSheet(
    sheetState: SheetState,
    loginState: LoginUiState,
    onDismissRequest: () -> Unit,
    onLogin: (username: String, password: String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        LoginSheetContent(loginState = loginState, onLogin = onLogin)
    }
}

/** Content-only, for hosts that switch between several sheets in one ModalBottomSheet. */
@Composable
fun LoginSheetContent(
    loginState: LoginUiState,
    onLogin: (username: String, password: String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Login", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        if (loginState is LoginUiState.Error) {
            Text(loginState.message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { onLogin(username, password) },
            enabled = loginState !is LoginUiState.Loading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loginState is LoginUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Login")
            }
        }
    }
}

/** Content-only: signed-in user's details plus a logout button. */
@Composable
fun AccountSheetContent(
    user: User,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${user.firstName} ${user.lastName}", style = MaterialTheme.typography.titleLarge)
        Text(user.username, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (user.ticketWallet.isNotEmpty()) {
            HorizontalDivider()
            Text("My Tickets", style = MaterialTheme.typography.titleMedium)
            user.ticketWallet.forEach { ticket ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ticket.label)
                    Text(formatPrice(ticket.priceCents, ticket.currency))
                }
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Logout")
        }
    }
}