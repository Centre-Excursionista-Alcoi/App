package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char

@Composable
expect fun PlatformDatePicker(
    value: LocalDate?,
    onValueChanged: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    format: DateTimeFormat<LocalDate> = LocalDate.Format {
        year()
        char('/')
        monthNumber()
        char('/')
        dayOfMonth()
    },
    min: LocalDate? = null,
    max: LocalDate? = null,
    initialDisplayedDate: LocalDate? = null
)
