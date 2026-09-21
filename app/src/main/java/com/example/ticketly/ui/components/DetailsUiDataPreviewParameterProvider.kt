package com.example.ticketly.ui.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.ticketly.model.Event
import com.example.ticketly.model.Ticket
import com.example.ticketly.model.Venue

class DetailsUiDataPreviewParameterProvider : PreviewParameterProvider<Event> {
    override val values: Sequence<Event> = sequenceOf(DetailsPreviewParameterData.detailsUiData)
}

private object DetailsPreviewParameterData {
        val detailsUiData =  Event(
        id = "evt_01",
        title = "Jazz Night: Summer Series",
        description = "A jazz event at Workshop Co-op. Show 1 of the season.",
        startsAt = "2026-06-08T18:00:00.000Z",
        imageUrl = "",
        venue = Venue(id = "ven_18", name = "Workshop Co-op", imageUrl = ""),
        tickets = listOf(
            Ticket(id = "tkt_01_01", label = "GA", priceCents = 2537, currency = "USD", quantityAvailable = 12),
            Ticket(id = "tkt_01_02", label = "VIP", priceCents = 3337, currency = "USD", quantityAvailable = 23),
        ),
    )
}