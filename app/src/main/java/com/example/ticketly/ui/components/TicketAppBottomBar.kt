package com.example.ticketly.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ticketly.presentation.cart.navigation.CartBaseRoute
import com.example.ticketly.presentation.features.events.navigation.EventsBaseRoute
import com.example.ticketly.presentation.features.events.navigation.navigateToEvents
import com.example.ticketly.ui.theme.TicketlyIcons

@Composable
fun TicketlyBottomBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    NavigationBar {
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.hasRoute<EventsBaseRoute>() } == true,
            // Home always lands on a clean Events screen - it doesn't restore whatever was
            // pushed on top (e.g. Detail) the way a normal tab switch would.
            onClick = { navController.navigateToEvents() },
            icon = { Icon(TicketlyIcons.Home, contentDescription = "Home") },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.hasRoute<CartBaseRoute>() } == true,
            onClick = { navController.navigateToTab(CartBaseRoute) },
            icon = { Icon(TicketlyIcons.Cart, contentDescription = "Cart") },
            label = { Text("Cart") },
        )
    }
}

/** Standard bottom-nav pattern: avoids piling up back stack entries when switching tabs. */
private fun <T : Any> NavHostController.navigateToTab(route: T) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
