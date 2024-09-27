/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonInclude
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

open class LoginTokenResponse(
    val token: String,
    val username: String? = null,
    val userId: Long? = null,
    val role: String? = null
) : RioResponse()

data class TokenListResponse(
    val data: List<TokenKeyData>,
    val page: PageInfo
) : RioListResponse<TokenKeyData>(data, page)

data class UserLoginCredentials(
    val username: String,
    val password: String
) : RioRequest
