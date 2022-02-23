/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TokenCreateRequest(
    val expirationDate: String? = null
) : RioRequest

data class TokenResponse(
    val token: String,
    val expirationDate: String?,
    val creationDate: String,
    val userName: String,
    val id: UUID
) : RioResponse()

open class TokenKeyResponse(
    val expirationDate: String? = null,
    val creationDate: String,
    val userName: String,
    val id: UUID
) : RioResponse()

open class TokenKeyData(
    val expirationDate: String? = null,
    val creationDate: String,
    val userName: String,
    val id: UUID
)

open class ShortTokenResponse(
    val token: String
) : RioResponse()

data class TokenListResponse(
    @JsonProperty("data") val objects: List<TokenKeyData>,
    val page: PageInfo
) : RioResponse()

data class UserLoginCredentials(
    val username: String,
    val password: String
) : RioRequest
