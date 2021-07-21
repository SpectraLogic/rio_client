/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import java.time.ZonedDateTime
import java.util.UUID

class CreateTokenResponse(
    val token: String,
    expirationDate: ZonedDateTime?,
    creationDate: ZonedDateTime,
    userName: String,
    id: UUID
) : ApiKeyResponse(expirationDate, creationDate, userName, id)

open class ApiKeyResponse(
    val expirationDate: ZonedDateTime? = null,
    val creationDate: ZonedDateTime,
    val userName: String,
    val id: UUID
)

data class TokenResponse(val token: String)

data class UserLoginCredentials(
    val username: String,
    val password: String
): RioRequest
