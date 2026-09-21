package com.example.ticketly.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ticketly.model.Event
import com.example.ticketly.model.Ticket
import com.example.ticketly.model.Venue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders `DetailScreen` directly with a fixed `DetailUiState` - no ViewModel, no Hilt, no navigation. */
@RunWith(AndroidJUnit4::class)
class DetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun whenLoading_showsLoadingIndicator() {
        composeTestRule.setContent {
            DetailScreen(uiState = DetailUiState.Loading)
        }

        composeTestRule.onNodeWithTag("circular_loading_indicator").assertIsDisplayed()
    }

    @Test
    fun whenSuccess_showsEventTitleAndBuyButton() {
        val event = sampleEvent()

        composeTestRule.setContent {
            DetailScreen(uiState = DetailUiState.Success(event))
        }

        composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
        composeTestRule.onNodeWithText("Buy").assertIsDisplayed()
    }

    @Test
    fun whenSuccessWithSoldOutTicket_showsSoldOut() {
        val event = sampleEvent(quantityAvailable = 0)

        composeTestRule.setContent {
            DetailScreen(uiState = DetailUiState.Success(event))
        }

        composeTestRule.onNodeWithText("Sold out").assertIsDisplayed()
    }

    @Test
    fun whenFailure_showsErrorMessage() {
        composeTestRule.setContent {
            DetailScreen(uiState = DetailUiState.Failure)
        }

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    private fun sampleEvent(quantityAvailable: Int = 10) = Event(
        id = "evt_1",
        title = "Test Concert",
        description = null,
        startsAt = "2026-06-01T20:00:00Z",
        imageUrl = "",
        venue = Venue(id = "venue_1", name = "Test Venue", imageUrl = ""),
        tickets = listOf(
            Ticket(
                id = "tkt_1",
                label = "General Admission",
                priceCents = 2500,
                currency = "USD",
                quantityAvailable = quantityAvailable,
            ),
        ),
    )
}
