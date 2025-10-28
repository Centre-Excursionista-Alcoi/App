package org.centrexcursionistalcoi.app.auth

actual object AuthFlowLogic {
    actual fun start() {
        val (state, codeChallenge) = generateAndStorePCKE()

        AuthFlowWindow.start(state, codeChallenge)
    }
}
