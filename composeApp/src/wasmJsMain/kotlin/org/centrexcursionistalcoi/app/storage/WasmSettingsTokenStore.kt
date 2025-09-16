package org.centrexcursionistalcoi.app.storage

import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore

@OptIn(ExperimentalOpenIdConnect::class)
class WasmSettingsTokenStore : SettingsTokenStore(WasmSettingsStore())
