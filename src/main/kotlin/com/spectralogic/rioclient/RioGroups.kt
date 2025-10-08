package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class ListGroupsForRioUserResponse(
    val rioGroupUuids: List<String>,
    val userUuid: String,
) : RioResponse()

@Serializable
data class CreateRioGroupRequest(
    val rioDomainGroupUuids: List<String>?,
    val rioGroupDescription: String?,
    val rioGroupName: String,
    val rioUserUuids: List<String>,
) : RioRequest

@Serializable
data class ListRioGroups(
    val page: PageInfo,
    val rioGroups: List<RioGroupDetails>,
) : RioResponse()

@Serializable
/**
 * Request body to update the Rio Groups associated with a user.
 */
data class UpdateUserRioGroupRequest(
    val rioGroupUuids: List<String>,
) : RioRequest

@Serializable
data class RioGroupDetails(
    val rioDomainGroupUuids: List<String>?,
    val rioGroupDescription: String?,
    val rioGroupName: String,
    val rioGroupUuid: String,
    val rioMetaGroup: Boolean,
    val rioUserUuids: List<String>?,
) : RioResponse()
