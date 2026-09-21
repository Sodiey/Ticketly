package com.example.ticketly

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.ticketly.presentation.cart.navigation.cartSection
import com.example.ticketly.presentation.cart.navigation.navigateToCart
import com.example.ticketly.presentation.detail.navigation.detailSection
import com.example.ticketly.presentation.detail.navigation.navigateToDetail
import com.example.ticketly.presentation.features.events.navigation.EventsBaseRoute
import com.example.ticketly.presentation.features.events.navigation.eventSection
import com.example.ticketly.presentation.features.events.navigation.navigateToEvents

@Composable
fun MainNavGraph(
    navHostController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navHostController,
        startDestination = EventsBaseRoute,
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        // Horizontal push/pop instead of the default fade-through crossfade.
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
    ) {
        eventSection(
            onEventClick = { eventId -> navHostController.navigateToDetail(eventId) }
        )
        detailSection(
            onNavigateToCart = { navHostController.navigateToCart() },
            onEventClick = { eventId -> navHostController.navigateToDetail(eventId) }
        )
        cartSection(
            onCheckoutSuccess = { navHostController.navigateToEvents() }
        )
    }
}