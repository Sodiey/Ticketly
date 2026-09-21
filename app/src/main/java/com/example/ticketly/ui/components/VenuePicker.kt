package com.example.ticketly.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ticketly.ui.theme.TicketlyIcons

/**
 * The venue label and the filter icon open two different things - venue picking vs.
 * the price filter sheet - so each gets its own click target instead of sharing one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Filters(
    onVenueClick: () -> Unit = {},
    onFilterClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onVenueClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "All venues", style = MaterialTheme.typography.titleMedium)
            Icon(TicketlyIcons.ChevronDown, contentDescription = null)
        }
        Box(
            modifier = Modifier
                .clickable(onClick = onFilterClick)
        ) {
            Icon(TicketlyIcons.Filter, contentDescription = "Filter")
        }
    }
}
