package com.example.ticketly.presentation.cart.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.ticketly.presentation.cart.CartRoute
import kotlinx.serialization.Serializable

@Serializable
data object Cart

@Serializable
data object CartBaseRoute

/**
 * "Finish checkout, show the cart" - a plain forward push, so Detail stays on the back
 * stack underneath and Cart's own back button returns to it. Safe to leave Detail there:
 * the Home button does its own unconditional reset regardless of what's on the stack, so
 * it doesn't rely on this navigation clearing anything to stay correct.
 */
fun NavController.navigateToCart() {
    navigate(CartBaseRoute) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.cartSection(onCheckoutSuccess: () -> Unit) {
    navigation<CartBaseRoute>(startDestination = Cart) {
        composable<Cart> {
            CartRoute(onCheckoutSuccess = onCheckoutSuccess)
        }
    }
}
