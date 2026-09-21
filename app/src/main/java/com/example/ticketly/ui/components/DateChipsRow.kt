package com.example.ticketly.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ticketly.ui.theme.TicketlyTheme
import com.example.ticketly.utils.upcomingDates
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * A horizontal row of day chips, e.g. "Thu 17 Sep". Purely presentational: it
 * renders whatever [dates] it's given and reports taps via [onDateSelected] -
 * it doesn't generate dates or own selection state itself.
 */
@Composable
fun DateChipsRow(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabledDates: Set<LocalDate>? = null,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(dates, key = { it.toEpochDay() }) { date ->
            DateChip(
                date = date,
                selected = date == selectedDate,
                // null means "no restriction" - callers that don't care about
                // disabling anything don't have to pass a set of every date.
                enabled = enabledDates == null || date in enabledDates,
                onClick = { onDateSelected(date) },
            )
        }
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    Column(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The day name sits outside the outline - only the date+month are boxed.
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
        Column(
            modifier = Modifier
                .clip(shape)
                .padding(top = 4.dp)
                .let {
                    if (selected) it.border(1.dp, MaterialTheme.colorScheme.outline, shape) else it
                }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else contentColor,
            )
            Text(
                text = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DateChipsRowPreview() {
    TicketlyTheme {
        val dates = remember { upcomingDates(months = 6) }
        DateChipsRow(
            dates = dates,
            selectedDate = dates.first(),
            onDateSelected = {},
        )
    }
}
