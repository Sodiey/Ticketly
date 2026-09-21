package com.example.ticketly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A shorter, fixed-height app bar than Material3's own TopAppBar - this project's
 * Material3 version (1.3.2) doesn't expose a height override on TopAppBar, so this
 * reimplements the same visual language (colored band, nav icon, title, actions) at a
 * more compact fixed height instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketlyAppBar(
    titleRes: String,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.containerColor)
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon()
        Text(
            text = titleRes,
            style = MaterialTheme.typography.titleLarge,
            color = colors.titleContentColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        actions()
    }
}
