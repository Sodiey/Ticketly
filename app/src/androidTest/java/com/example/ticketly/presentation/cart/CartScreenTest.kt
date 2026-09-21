package com.example.ticketly.presentation.cart

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ticketly.model.CartLine
import com.example.ticketly.model.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders `CartScreen` directly with fixed `user`/`cart` params - no ViewModel, no Hilt, no navigation. */
@RunWith(AndroidJUnit4::class)
class CartScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun whenSignedOut_showsLoginPrompt() {
        composeTestRule.setContent {
            CartScreen(user = null)
        }

        composeTestRule.onNodeWithText("Log in to view your cart").assertIsDisplayed()
    }

    @Test
    fun whenSignedInWithEmptyCart_showsEmptyMessage() {
        composeTestRule.setContent {
            CartScreen(user = sampleUser(), cart = emptyList())
        }

        composeTestRule.onNodeWithText("Your cart is empty").assertIsDisplayed()
    }

    @Test
    fun whenSignedInWithCartLines_showsTicketAndCheckoutButton() {
        val cartLine = sampleCartLine()

        composeTestRule.setContent {
            CartScreen(user = sampleUser(), cart = listOf(cartLine))
        }

        composeTestRule.onNodeWithText(cartLine.eventTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(cartLine.ticketLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText("Checkout").assertIsDisplayed()
    }

    private fun sampleUser() = User(
        id = "user_1",
        firstName = "Test",
        lastName = "User",
        username = "testuser",
    )

    private fun sampleCartLine() = CartLine(
        ticketId = "tkt_1",
        eventId = "evt_1",
        eventTitle = "Test Concert",
        ticketLabel = "General Admission",
        priceCents = 2500,
        currency = "USD",
    )
}
