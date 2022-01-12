package com.spectralogic.rioclient

internal class TokenContainer(
    private val longLivedToken: String? = null,
    private val tokenGenerator: () -> String
) {
    private var internalToken: String = ""

    val token: String
        get() {
            if (longLivedToken != null) return longLivedToken
            if (internalToken.isEmpty()) {
                internalToken = tokenGenerator()
            }
            return internalToken
        }
}
