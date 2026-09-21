package com.example.ticketly.utils

import java.util.Locale

/** priceCents is a minor-unit integer (e.g. 2537 = 25.37); currency is an ISO code like "USD". */
fun formatPrice(priceCents: Int, currency: String): String {
    return String.format(Locale.US, "%.2f %s", priceCents / 100.0, currency)
}
