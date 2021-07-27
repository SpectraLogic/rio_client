/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime
import java.util.UUID

data class TokenCreateRequest(
    val expirationDate: ZonedDateTime? = null
) : RioRequest

data class TokenResponse(
    val token: String,
    val expirationDate: ZonedDateTime?,
    val creationDate: ZonedDateTime,
    val userName: String,
    val id: UUID
)

open class TokenKeyResponse(
    val expirationDate: ZonedDateTime? = null,
    val creationDate: ZonedDateTime,
    val userName: String,
    val id: UUID
)

open class ShortTokenResponse(
    val token: String
)

data class TokenListResponse(
    @JsonProperty("data") override val objects: List<TokenKeyResponse>,
    override val page: PageInfo
) : PageData<TokenKeyResponse>

data class UserLoginCredentials(
    val username: String,
    val password: String
) : RioRequest
