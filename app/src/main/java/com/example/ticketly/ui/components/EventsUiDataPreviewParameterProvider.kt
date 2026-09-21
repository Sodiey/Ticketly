package com.example.ticketly.ui.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.ticketly.model.Event
import com.example.ticketly.model.Ticket
import com.example.ticketly.model.Venue
import com.example.ticketly.presentation.events.EventsUiData

class EventsUiDataPreviewParameterProvider : PreviewParameterProvider<EventsUiData> {
    override val values: Sequence<EventsUiData> = sequenceOf(PreviewParameterData.eventsUiData)
}

private object PreviewParameterData {
    val eventsUiData = EventsUiData(
        events = listOf(
            Event(
                id = "evt_01",
                title = "Jazz Night: Summer Series",
                description = "A jazz event at Workshop Co-op.",
                startsAt = "2026-06-08T18:00:00.000Z",
                imageUrl = "",
                venue = Venue(id = "ven_18", name = "Workshop Co-op", imageUrl = ""),
                tickets = listOf(
                    Ticket(id = "tkt_01_01", label = "GA", priceCents = 2537, currency = "USD", quantityAvailable = 12),
                ),
            ),
            Event(
                id = "evt_02",
                title = "Techno Night: Fall Series",
                description = "A techno event at Central Park Amphitheater.",
                startsAt = "2026-07-15T19:00:00.000Z",
                imageUrl = "",
                venue = Venue(id = "ven_02", name = "Central Park Amphitheater", imageUrl = ""),
                tickets = listOf(
                    Ticket(id = "tkt_02_01", label = "GA", priceCents = 2774, currency = "USD", quantityAvailable = 13),
                ),
            ),
        ),
    )
}
