/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

data class UserCreateRequest(
    val username: String,
    val password: String,
    val local: Boolean?
) : RioRequest

data class UserUpdateRequest(
    val password: String?,
    val local: Boolean?
) : RioRequest

data class UserListResponse(
    val users: List<UserResponse>,
    val page: PageInfo
) : RioListResponse<UserResponse>(users, page)

data class UserResponse(
    val username: String,
    val active: Boolean,
    val local: Boolean,
    val createDate: String,
    val updateDate: String
) : RioResponse()