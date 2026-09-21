package com.example.ticketly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ticketly.model.User
import com.example.ticketly.ui.theme.TicketlyIcons

/**
 * Shows the login icon when signed out, or a circle with the user's initial when
 * signed in. [user] is read from local session state, never fetched here.
 */
@Composable
fun AuthIcon(
    user: User?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (user != null) {
        Box(
            modifier = modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = user.firstName.first().uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    } else {
        Icon(
            imageVector = TicketlyIcons.Login,
            contentDescription = "Login",
            modifier = modifier.clickable(onClick = onClick),
        )
    }
}
