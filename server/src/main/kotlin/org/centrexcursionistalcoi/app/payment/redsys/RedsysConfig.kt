package org.centrexcursionistalcoi.app.payment.redsys

/**
 * How to reach the club's Redsys terminal, from the environment:
 * - `REDSYS_MERCHANT_CODE`: the merchant number (FUC).
 * - `REDSYS_TERMINAL`: the terminal number.
 * - `REDSYS_SECRET_KEY`: the terminal's signing key, from the Redsys admin portal.
 * - `REDSYS_ENVIRONMENT`: `test` (default) or `production`.
 * - `REDSYS_BIZUM`: `true` to offer Bizum, once the bank has enabled it on the terminal (off by default).
 */
data class RedsysConfig(
    val merchantCode: String,
    val terminal: String,
    val secretKey: String,
    val production: Boolean = false,
    val bizum: Boolean = false,
) {
    init {
        require(merchantCode.isNotBlank()) { "REDSYS_MERCHANT_CODE is empty" }
        require(terminal.isNotBlank()) { "REDSYS_TERMINAL is empty" }
        require(secretKey.length >= 16) { "REDSYS_SECRET_KEY is too short" }
        require(!production || secretKey != SANDBOX_KEY) { "REDSYS_SECRET_KEY is Redsys's public sandbox key, it can't be used in production" }
    }

    /** The page the user is sent to, to pay. */
    val paymentUrl: String get() = if (production) "https://sis.redsys.es/sis/realizarPago" else "https://sis-t.redsys.es:25443/sis/realizarPago"

    /** The REST endpoint for operations that don't need the user (refunds). */
    val restUrl: String get() = if (production) "https://sis.redsys.es/sis/rest/trataPeticionREST" else "https://sis-t.redsys.es:25443/sis/rest/trataPeticionREST"

    companion object {
        /** The key Redsys publishes for everyone's tests. */
        const val SANDBOX_KEY = "sq7HjrUOBfKmC576ILgskD5srU870gJ7"

        /**
         * Reads the configuration with [getenv].
         * @return `null` if Redsys isn't configured at all.
         * @throws IllegalArgumentException if it's configured partially or wrongly.
         */
        fun fromEnvironment(getenv: (String) -> String? = System::getenv): RedsysConfig? {
            fun value(name: String) = getenv(name)?.trim()?.ifEmpty { null }
            val merchantCode = value("REDSYS_MERCHANT_CODE")
            val terminal = value("REDSYS_TERMINAL")
            val secretKey = value("REDSYS_SECRET_KEY")
            if (merchantCode == null && terminal == null && secretKey == null) return null
            require(merchantCode != null && terminal != null && secretKey != null) {
                "REDSYS_MERCHANT_CODE, REDSYS_TERMINAL and REDSYS_SECRET_KEY must all be set"
            }
            val environment = value("REDSYS_ENVIRONMENT")?.lowercase() ?: "test"
            require(environment == "test" || environment == "production") { "REDSYS_ENVIRONMENT must be test or production" }
            return RedsysConfig(
                merchantCode = merchantCode,
                terminal = terminal,
                secretKey = secretKey,
                production = environment == "production",
                bizum = value("REDSYS_BIZUM")?.lowercase() == "true",
            )
        }
    }
}
