package com.spectralogic.rioclient

import java.time.ZonedDateTime

class TokenContainer(private val reAuthSeconds: Long, private val tokenGenerator: () -> String) {
    private var internalToken: String = ""
    val token: String
        get() {
            if (internalToken.isEmpty() || ZonedDateTime.now().isAfter(creation.plusSeconds(reAuthSeconds))) {
                internalToken = tokenGenerator()
                creation = ZonedDateTime.now()
            }
            return internalToken
        }
    var creation: ZonedDateTime = ZonedDateTime.now()
}
