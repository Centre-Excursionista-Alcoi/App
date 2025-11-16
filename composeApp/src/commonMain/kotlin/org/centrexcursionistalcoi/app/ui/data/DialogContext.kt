package org.centrexcursionistalcoi.app.ui.data

import androidx.compose.foundation.layout.ColumnScope

interface IDialogContext {
    fun dismiss()
}

interface DialogContext: IDialogContext, ColumnScope

class DialogContextImpl(scope: ColumnScope, private val onDismiss: () -> Unit) : DialogContext, ColumnScope by scope {
    override fun dismiss() {
        onDismiss()
    }
}
