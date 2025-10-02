package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.network.getHttpClient

abstract class ViewModelBase: ViewModel() {
    protected val client by lazy { getHttpClient() }
}
