package com.example.ticketly.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ticketly.model.User

private enum class AuthSheet { Login, Account }

/**
 * Self-contained profile icon + its own single login/account sheet, for hosts
 * (like MainActivity's app bar) that don't otherwise manage a bottom sheet.
 * EventsScreen has its own sheet host and doesn't use this - it merges Login/Account
 * into the same active-sheet state as its Filter sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthMenu(
    currentUser: User?,
    loginState: LoginUiState,
    onLogin: (username: String, password: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSheet by remember { mutableStateOf<AuthSheet?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(currentUser) {
        if (currentUser != null && activeSheet == AuthSheet.Login) activeSheet = null
        if (currentUser == null && activeSheet == AuthSheet.Account) activeSheet = null
    }

    AuthIcon(
        user = currentUser,
        onClick = { activeSheet = if (currentUser == null) AuthSheet.Login else AuthSheet.Account },
        modifier = modifier.padding(end = 16.dp),
    )

    val sheetToShow = activeSheet
    if (sheetToShow != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
        ) {
            when (sheetToShow) {
                AuthSheet.Login -> LoginSheetContent(loginState = loginState, onLogin = onLogin)
                AuthSheet.Account -> currentUser?.let { AccountSheetContent(user = it, onLogout = onLogout) }
            }
        }
    }
}
