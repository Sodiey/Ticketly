package com.example.ticketly.presentation.detail

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ticketly.model.Event
import com.example.ticketly.model.Ticket
import com.example.ticketly.presentation.auth.AuthViewModel
import com.example.ticketly.ui.components.CircularProgressLoader
import com.example.ticketly.ui.components.DetailsUiDataPreviewParameterProvider
import com.example.ticketly.ui.components.EventCard
import com.example.ticketly.ui.components.LoginBottomSheet
import com.example.ticketly.ui.theme.TicketlyTheme
import com.example.ticketly.utils.formatPrice
import com.example.ticketly.utils.toLocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenRoute(
    viewModel: DetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToCart: () -> Unit,
    onEventClick: (String) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val addToCartState = viewModel.addToCartState.collectAsStateWithLifecycle()
    val currentUser = authViewModel.currentUser.collectAsStateWithLifecycle()
    val loginState = authViewModel.loginState.collectAsStateWithLifecycle()
    var pendingTicketId by remember { mutableStateOf<String?>(null) }
    var showLoginSheet by remember { mutableStateOf(false) }

    val loginSheetState = rememberModalBottomSheetState()
    // Signed-out tap opens the sheet and remembers which ticket was requested; once
    // currentUser reflects a successful login, the sheet dismisses and the buy retries -
    // both reacting to the same StateFlow, not a callback threaded through AuthViewModel.
    LaunchedEffect(currentUser.value) {
        if (currentUser.value != null) {
            showLoginSheet = false
            pendingTicketId?.let { viewModel.addTicket(it) }
            pendingTicketId = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToCart.collect { onNavigateToCart() }
    }

    DetailScreen(
        uiState = uiState.value,
        addToCartState = addToCartState.value,
        onBuyClick = { ticketId ->
            if (currentUser.value == null) {
                pendingTicketId = ticketId
                showLoginSheet = true
            } else {
                viewModel.addTicket(ticketId)
            }
        },
        onEventClick = onEventClick,
    )
    if (showLoginSheet) {
        LoginBottomSheet(
            sheetState = loginSheetState,
            loginState = loginState.value,
            onDismissRequest = {
                pendingTicketId = null
                showLoginSheet = false
            },
            onLogin = authViewModel::login,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    addToCartState: AddToCartUiState = AddToCartUiState.Idle,
    onBuyClick: (ticketId: String) -> Unit = {},
    onEventClick: (String) -> Unit = {},
) {
    when (uiState) {
        is DetailUiState.Loading -> {
            CircularProgressLoader()
        }
        is DetailUiState.Success -> {
            EventDetailContent(
                event = uiState.event,
                similarEvents = uiState.similarEvents,
                addToCartState = addToCartState,
                onBuyClick = onBuyClick,
                onEventClick = onEventClick,
            )
        }
        is DetailUiState.Failure -> {
            Text("Something went wrong")
        }
    }
}

@Composable
private fun EventDetailContent(
    event: Event,
    similarEvents: List<Event>,
    addToCartState: AddToCartUiState,
    onBuyClick: (ticketId: String) -> Unit,
    onEventClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AsyncImage(
            model = event.imageUrl,
            contentDescription = event.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = formatEventDate(event.startsAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!event.description.isNullOrBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = event.venue.imageUrl,
                    contentDescription = event.venue.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(text = event.venue.name, style = MaterialTheme.typography.bodyLarge)
            }

            Text(
                text = "Tickets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )

            if (addToCartState is AddToCartUiState.Error) {
                Text(
                    text = addToCartState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            event.tickets.forEach { ticket ->
                TicketRow(
                    ticket = ticket,
                    isLoading = addToCartState is AddToCartUiState.Loading,
                    onBuyClick = { onBuyClick(ticket.id) },
                )
            }

            if (similarEvents.isNotEmpty()) {
                Text(
                    text = "Similar Events",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    similarEvents.forEach { similarEvent ->
                        EventCard(event = similarEvent, onClick = { onEventClick(similarEvent.id) })
                    }
                }
            }
        }
    }
}

private fun formatEventDate(startsAt: String): String {
    val date = startsAt.toLocalDate()
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$dayName, ${date.dayOfMonth} $monthName"
}

private const val LOW_STOCK_THRESHOLD = 5
private const val MODERATE_STOCK_THRESHOLD = 20

@Composable
private fun TicketRow(
    ticket: Ticket,
    isLoading: Boolean,
    onBuyClick: () -> Unit,
) {
    val soldOut = ticket.quantityAvailable <= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = ticket.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = formatPrice(ticket.priceCents, ticket.currency),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Button(
                modifier = Modifier.padding(bottom = 4.dp),
                onClick = onBuyClick,
                enabled = !isLoading && !soldOut
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(if (soldOut) "Sold out" else "Buy")
                }
            }
            TicketAvailabilityBadge(ticket.quantityAvailable)
        }

    }
}

/** Always visible; both the emoji and the color scale with how urgent the count is. */
@Composable
private fun TicketAvailabilityBadge(quantityAvailable: Int) {
    val soldOut = quantityAvailable <= 0
    val (emoji, urgent) = when {
        soldOut -> "❌" to true
        quantityAvailable <= LOW_STOCK_THRESHOLD -> "🔥" to true
        quantityAvailable <= MODERATE_STOCK_THRESHOLD -> "⚡" to false
        else -> "🎟️" to false
    }
    val text = if (soldOut) "Sold out" else "Only $quantityAvailable available"

    Text(
        text = "$emoji $text",
        style = MaterialTheme.typography.labelMedium,
        color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DetailScreenPreview(
    @PreviewParameter(DetailsUiDataPreviewParameterProvider::class)
    detailsUiData: Event,
) {
    TicketlyTheme {
        DetailScreen(
            uiState = DetailUiState.Success(detailsUiData)
        )
    }
}
