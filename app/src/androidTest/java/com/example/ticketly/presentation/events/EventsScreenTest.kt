package com.example.ticketly.presentation.events

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ticketly.model.Event
import com.example.ticketly.model.Venue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders `EventsScreen` directly with a fixed `EventsUiState` - no ViewModel, no Hilt, no navigation. */
@RunWith(AndroidJUnit4::class)
class EventsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun whenLoading_showsLoadingIndicator() {
        composeTestRule.setContent {
            EventsScreen(uiState = EventsUiState.Loading)
        }

        composeTestRule.onNodeWithTag("circular_loading_indicator").assertIsDisplayed()
    }

    @Test
    fun whenSuccess_showsEventTitle() {
        val event = sampleEvent()

        composeTestRule.setContent {
            EventsScreen(uiState = EventsUiState.Success(EventsUiData(events = listOf(event))))
        }

        composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
    }

    @Test
    fun whenSuccessWithNoEvents_showsEmptyMessage() {
        composeTestRule.setContent {
            EventsScreen(uiState = EventsUiState.Success(EventsUiData(events = emptyList())))
        }

        composeTestRule.onNodeWithText("No events found").assertIsDisplayed()
    }

    @Test
    fun whenFailure_showsErrorMessage() {
        composeTestRule.setContent {
            EventsScreen(uiState = EventsUiState.Failure)
        }

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun tappingFilterIcon_opensPriceFilterSheet_butTappingAllVenuesDoesNot() {
        composeTestRule.setContent {
            EventsScreen(uiState = EventsUiState.Success(EventsUiData(events = listOf(sampleEvent()))))
        }

        composeTestRule.onNodeWithText("All venues").performClick()
        composeTestRule.onNodeWithText("Price range").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Filter").performClick()
        composeTestRule.onNodeWithText("Price range").assertIsDisplayed()
    }

    private fun sampleEvent() = Event(
        id = "evt_1",
        title = "Test Concert",
        description = null,
        startsAt = "2026-06-01T20:00:00Z",
        imageUrl = "",
        venue = Venue(id = "venue_1", name = "Test Venue", imageUrl = ""),
        tickets = emptyList(),
    )
}
