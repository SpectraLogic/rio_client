package com.spectralogic.rioclient

import java.time.ZonedDateTime

internal class TokenContainer(
    private val reAuthSeconds: Long,
    private val longLivedToken: String? = null,
    private val tokenGenerator: () -> String
) {
    private var internalToken: String = ""

    // TODO: ESCP-3450
    val token: String
        get() {
            if (longLivedToken != null) return longLivedToken
            if (internalToken.isEmpty() || ZonedDateTime.now().isAfter(creation.plusSeconds(reAuthSeconds))) {
                internalToken = tokenGenerator()
                creation = ZonedDateTime.now()
            }
            return internalToken
        }
    var creation: ZonedDateTime = ZonedDateTime.now()
}
