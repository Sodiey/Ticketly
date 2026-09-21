package com.example.ticketly.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ticketly.model.Event
import com.example.ticketly.utils.formatPrice

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                Text(text = event.venue.name, style = MaterialTheme.typography.bodyMedium)
                val lowestPriceTicket = event.tickets.minByOrNull { it.priceCents }
                if (lowestPriceTicket != null) {
                    Text(
                        text = "From ${formatPrice(lowestPriceTicket.priceCents, lowestPriceTicket.currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
