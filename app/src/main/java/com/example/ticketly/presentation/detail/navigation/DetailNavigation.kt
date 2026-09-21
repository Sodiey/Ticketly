package com.example.ticketly.presentation.detail.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.ticketly.presentation.detail.DetailScreenRoute
import com.example.ticketly.presentation.detail.DetailViewModel
import kotlinx.serialization.Serializable


@Serializable data class DetailRoute(val id: String)
fun NavController.navigateToDetail(
    eventId: String,
    navOptions: NavOptionsBuilder.() -> Unit = {}
) {
    navigate(route = DetailRoute(eventId)) {
        navOptions()
    }
}

fun NavGraphBuilder.detailSection(onNavigateToCart: () -> Unit, onEventClick: (String) -> Unit) {
    composable<DetailRoute> { entry ->
        val id = entry.toRoute<DetailRoute>().id
        DetailScreenRoute(
            viewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
                key = id,
            ) { factory ->
                factory.create(id)
            },
            onNavigateToCart = onNavigateToCart,
            onEventClick = onEventClick,
        )
    }
}