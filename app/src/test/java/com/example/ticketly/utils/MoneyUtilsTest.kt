package com.example.ticketly.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyUtilsTest {

    @Test
    fun `formatPrice formats a whole-currency amount`() {
        assertEquals("25.00 USD", formatPrice(priceCents = 2500, currency = "USD"))
    }

    @Test
    fun `formatPrice zero-pads sub-100-cents amounts`() {
        assertEquals("25.07 USD", formatPrice(priceCents = 2507, currency = "USD"))
    }

    @Test
    fun `formatPrice includes the given currency code`() {
        assertEquals("10.00 EUR", formatPrice(priceCents = 1000, currency = "EUR"))
    }
}
