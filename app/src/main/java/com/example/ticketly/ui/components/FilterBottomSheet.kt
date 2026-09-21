package com.example.ticketly.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ticketly.ui.theme.TicketlyTheme
import com.example.ticketly.utils.formatPrice

private const val PRICE_STEP_CENTS = 1_000

/** "$25" - a compact axis label, distinct from [formatPrice]'s full "25.00 USD" used for line items. */
private fun compactPrice(cents: Int) = "$${cents / 100}"

/**
 * Content-only, hosted by EventsScreen's shared ModalBottomSheet. Purely presentational -
 * [bounds] and [appliedRange] come from the ViewModel, and dragging the slider only
 * reports back through [onApply]/[onReset]; this composable does no filtering itself.
 */
@Composable
fun FilterSheetContent(
    bounds: ClosedFloatingPointRange<Float>,
    appliedRange: ClosedFloatingPointRange<Float>,
    onApply: (ClosedFloatingPointRange<Float>) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Seeded from the currently applied range so reopening the sheet shows what's
    // actually active, not a reset slider - re-seeds if a Reset changes it underneath.
    var priceRange by remember(appliedRange) { mutableStateOf(appliedRange) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Filter", style = MaterialTheme.typography.titleLarge)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Price range", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${formatPrice(priceRange.start.toInt(), "USD")} – " +
                            formatPrice(priceRange.endInclusive.toInt(), "USD"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                RangeSlider(
                    value = priceRange,
                    onValueChange = { priceRange = it },
                    valueRange = bounds,
                    steps = ((bounds.endInclusive - bounds.start) / PRICE_STEP_CENTS).toInt() - 1,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = compactPrice(bounds.start.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${compactPrice(bounds.endInclusive.toInt())}+",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    priceRange = bounds
                    onReset()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Reset")
            }
            Button(
                onClick = { onApply(priceRange) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Apply")
            }
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun FilterSheetContentPreview() {
    val bounds = 0f..20_000f
    TicketlyTheme {
        FilterSheetContent(
            bounds = bounds,
            appliedRange = bounds,
            onApply = {},
            onReset = {},
        )
    }
}
