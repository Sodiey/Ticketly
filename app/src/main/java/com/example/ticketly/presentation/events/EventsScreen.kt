package com.example.ticketly.presentation.events

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ticketly.model.User
import com.example.ticketly.presentation.auth.AuthViewModel
import com.example.ticketly.ui.components.AccountSheetContent
import com.example.ticketly.ui.components.AuthIcon
import com.example.ticketly.ui.components.CircularProgressLoader
import com.example.ticketly.ui.components.DateChipsRow
import com.example.ticketly.ui.components.EventCard
import com.example.ticketly.ui.components.EventsSearchBar
import com.example.ticketly.ui.components.EventsUiDataPreviewParameterProvider
import com.example.ticketly.ui.components.FilterSheetContent
import com.example.ticketly.ui.components.LoginSheetContent
import com.example.ticketly.ui.components.LoginUiState
import com.example.ticketly.ui.components.Filters
import com.example.ticketly.ui.theme.TicketlyTheme
import com.example.ticketly.utils.upcomingDates
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsRoute(
    viewModel: EventsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onEventClick: (String) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDate = viewModel.selectedDate.collectAsStateWithLifecycle()
    val priceRange = viewModel.priceRange.collectAsStateWithLifecycle()
    val currentUser = authViewModel.currentUser.collectAsStateWithLifecycle()
    val loginState = authViewModel.loginState.collectAsStateWithLifecycle()

    EventsScreen(
        uiState = uiState.value,
        searchQuery = searchQuery.value,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        selectedDate = selectedDate.value,
        onDateSelected = viewModel::onDateSelected,
        priceRangeBounds = viewModel.priceRangeBounds,
        appliedPriceRange = priceRange.value,
        onPriceRangeApplied = viewModel::onPriceRangeApplied,
        onPriceRangeReset = viewModel::onPriceRangeReset,
        currentUser = currentUser.value,
        loginState = loginState.value,
        onLogin = authViewModel::login,
        onLogout = authViewModel::logout,
        onEventClick = onEventClick,
    )
}

/** Which single bottom sheet (if any) EventsScreen is currently showing. */
private sealed interface EventsSheet {
    object Filter : EventsSheet
    object Login : EventsSheet
    object Account : EventsSheet
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    uiState: EventsUiState,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedDate: LocalDate = LocalDate.now(),
    onDateSelected: (LocalDate) -> Unit = {},
    priceRangeBounds: ClosedFloatingPointRange<Float> = 0f..20_000f,
    appliedPriceRange: ClosedFloatingPointRange<Float> = priceRangeBounds,
    onPriceRangeApplied: (ClosedFloatingPointRange<Float>) -> Unit = {},
    onPriceRangeReset: () -> Unit = {},
    currentUser: User? = null,
    loginState: LoginUiState = LoginUiState.Idle,
    onLogin: (String, String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
) {
    // Only one sheet can be open at a time, so a single active-sheet slot replaces
    // per-sheet booleans and states as more sheets get added.
    var activeSheet by remember { mutableStateOf<EventsSheet?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(currentUser) {
        if (currentUser != null && activeSheet == EventsSheet.Login) activeSheet = null
        if (currentUser == null && activeSheet == EventsSheet.Account) activeSheet = null
    }

    // The pickable date range anchors on the earliest fetched event - computed in the
    // ViewModel as part of EventsUiData, not here. Falls back to today while loading/on failure.
    val dates = (uiState as? EventsUiState.Success)?.data?.visibleDates ?: upcomingDates(months = 6)

    // Date chips are navigational, not a filter (see CLAUDE.md/SPEC.md's "Data notes") -
    // tapping one scrolls the already-grouped list to that day's header instead.
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val dateHeaderIndices = remember(uiState) {
        val indices = mutableMapOf<LocalDate, Int>()
        var index = 0
        (uiState as? EventsUiState.Success)?.data?.eventsByDate?.forEach { group ->
            indices[group.date] = index
            index += 1 + group.events.size
        }
        indices
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthIcon(
                user = currentUser,
                onClick = {
                    activeSheet = if (currentUser == null) EventsSheet.Login else EventsSheet.Account
                },
            )
            EventsSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
            )
        }

        // UI only for now - venue click isn't wired to anything yet.
        Filters(
            onFilterClick = { activeSheet = EventsSheet.Filter },
        )

        DateChipsRow(
            dates = dates,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                onDateSelected(date)
                dateHeaderIndices[date]?.let { index ->
                    coroutineScope.launch { listState.animateScrollToItem(index) }
                }
            },
            enabledDates = dateHeaderIndices.keys,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is EventsUiState.Loading -> {
                    CircularProgressLoader()
                }
                is EventsUiState.Success -> {
                    if (uiState.data.eventsByDate.isEmpty()) {
                        Text(text = "No events found")
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            uiState.data.eventsByDate.forEach { (date, eventsOnDate) ->
                                item(key = date) {
                                    EventsDateHeader(date)
                                }
                                items(eventsOnDate, key = { it.id }) { event ->
                                    EventCard(event = event, onClick = { onEventClick(event.id) })
                                }
                            }
                        }
                    }
                }
                is EventsUiState.Failure -> {
                    Text("Something went wrong", modifier = Modifier.padding(16.dp))
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
                EventsSheet.Filter -> FilterSheetContent(
                    bounds = priceRangeBounds,
                    appliedRange = appliedPriceRange,
                    onApply = { range ->
                        onPriceRangeApplied(range)
                        activeSheet = null
                    },
                    onReset = onPriceRangeReset,
                )
                EventsSheet.Login -> LoginSheetContent(loginState = loginState, onLogin = onLogin)
                EventsSheet.Account -> currentUser?.let {
                    AccountSheetContent(user = it, onLogout = onLogout)
                }
            }
        }
    }
}


@Composable
private fun EventsDateHeader(date: LocalDate) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    Text(
        text = "$dayName, ${date.dayOfMonth} $monthName",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EventsScreenPreview(
    @PreviewParameter(EventsUiDataPreviewParameterProvider::class)
    eventsUiData: EventsUiData,
) {
    TicketlyTheme {
        var query by remember { mutableStateOf("") }
        EventsScreen(
            uiState = EventsUiState.Success(eventsUiData),
            searchQuery = query,
            onSearchQueryChange = { query = it },
        )
    }
}
