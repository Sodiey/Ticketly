package com.example.ticketly.presentation.features.events.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.ticketly.presentation.events.EventsRoute
import kotlinx.serialization.Serializable

@Serializable
data object EventsRoute

@Serializable data object EventsBaseRoute

/**
 * Always lands on a clean Events screen, discarding whatever was pushed on top (e.g. Detail) -
 * unlike a normal tab switch, Home never restores prior state.
 */
fun NavController.navigateToEvents() {
    navigate(EventsBaseRoute) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
    }
}

fun NavGraphBuilder.eventSection(
    onEventClick: (String) -> Unit
) {
    navigation<EventsBaseRoute>(startDestination = EventsRoute) {
        composable<EventsRoute> {
            EventsRoute(onEventClick = onEventClick)
        }
    }
}