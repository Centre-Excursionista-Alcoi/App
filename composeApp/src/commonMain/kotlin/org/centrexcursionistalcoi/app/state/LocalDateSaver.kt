package org.centrexcursionistalcoi.app.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import kotlinx.datetime.LocalDate

object LocalDateSaver : Saver<MutableState<LocalDate?>, String> {
    override fun restore(value: String): MutableState<LocalDate?> {
        return mutableStateOf(
            try {
                LocalDate.parse(value)
            } catch (_: IllegalArgumentException) {
                null
            }
        )
    }

    override fun SaverScope.save(value: MutableState<LocalDate?>): String = value.value.toString()
}
