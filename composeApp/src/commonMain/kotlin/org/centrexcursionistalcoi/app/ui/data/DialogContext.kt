package org.centrexcursionistalcoi.app.ui.data

import androidx.compose.foundation.layout.ColumnScope

interface DialogContext : ColumnScope {
    fun dismiss()
}

class DialogContextImpl(scope: ColumnScope, private val onDismiss: () -> Unit) : DialogContext, ColumnScope by scope {
    override fun dismiss() {
        onDismiss()
    }
}
