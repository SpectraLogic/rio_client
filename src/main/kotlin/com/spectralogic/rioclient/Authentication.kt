/**
 * ***************************************************************************
 *    Copyright 2014-2024 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest(
    val username: String,
    val password: String,
    val local: Boolean?
) : RioRequest

@Serializable
data class UserUpdateRequest(
    val password: String?,
    val local: Boolean?
) : RioRequest

@Serializable
data class UserListResponse(
    val result: List<UserResponse>,
    val page: PageInfo
) : RioResponse(), RioListResponse<UserResponse> {
    override fun page() = page
    override fun results() = result
}

@Serializable
data class UserResponse(
    val username: String,
    val active: Boolean,
    val local: Boolean,
    val createDate: String,
    val updateDate: String
) : RioResponse()



        /*
            @Serializable
data class RioClientApplicationListResponse(
    val result: List<RioClientApplicationResponse>,
    val page: PageInfo
) : RioResponse(), RioListResponse<RioClientApplicationResponse> {
    override fun page() = page
    override fun results() = result
}
)
         */