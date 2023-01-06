/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TokenCreateRequest(
    val expirationDate: String? = null
) : RioRequest

@Serializable
data class TokenResponse(
    val token: String,
    val expirationDate: String? = null,
    val creationDate: String,
    val userName: String,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID
) : RioResponse()

@Serializable
open class TokenKeyResponse(
    val expirationDate: String? = null,
    val creationDate: String,
    val userName: String,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID
) : RioResponse()

@Serializable
open class TokenKeyData(
    val expirationDate: String? = null,
    val creationDate: String,
    val userName: String,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID
)

@Serializable
open class ShortTokenResponse(
    val token: String
) : RioResponse()

@Serializable
data class TokenListResponse(
    val data: List<TokenKeyData>,
    val page: PageInfo
) : RioResponse(), RioListResponse<TokenKeyData> {
    override fun page() = page
    override fun results() = data
}

@Serializable
data class UserLoginCredentials(
    val username: String,
    val password: String
) : RioRequest
