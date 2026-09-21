package com.example.ticketly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ticketly.presentation.auth.AuthViewModel
import com.example.ticketly.presentation.cart.navigation.Cart
import com.example.ticketly.presentation.detail.navigation.DetailRoute
import com.example.ticketly.ui.theme.TicketlyTheme
import com.example.ticketly.ui.components.AuthMenu
import com.example.ticketly.ui.components.TicketlyAppBar
import com.example.ticketly.ui.components.TicketlyBottomBar
import com.example.ticketly.ui.theme.TicketlyIcons
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicketlyTheme {
                val navController = rememberNavController()
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination
                val isDetailScreen = currentDestination?.hierarchy?.any { it.hasRoute<DetailRoute>() } == true
                val isCartScreen = currentDestination?.hierarchy?.any { it.hasRoute<Cart>() } == true

                val authViewModel: AuthViewModel = hiltViewModel()
                val currentUser = authViewModel.currentUser.collectAsStateWithLifecycle()
                val loginState = authViewModel.loginState.collectAsStateWithLifecycle()

                Scaffold(
                    topBar = {
                        // Events renders its own plain (non-Material-bar) header instead.
                        when {
                            isDetailScreen -> TicketlyAppBar(
                                titleRes = "Event details",
                                navigationIcon = { BackButton(navController) },
                                actions = {
                                    AuthMenu(
                                        currentUser = currentUser.value,
                                        loginState = loginState.value,
                                        onLogin = authViewModel::login,
                                        onLogout = authViewModel::logout,
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                            isCartScreen -> TicketlyAppBar(
                                titleRes = "Your cart",
                                navigationIcon = { BackButton(navController) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    },
                    bottomBar = {
                        if (!isDetailScreen) TicketlyBottomBar(navController)
                    },
                ) {
                    MainNavGraph(
                        navHostController = navController,
                        paddingValues = it
                    )
                }
            }
        }
    }
    @Composable
    private fun BackButton(navController: NavHostController) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(TicketlyIcons.KeyboardArrowLeft, contentDescription = "Back")
        }
    }
}