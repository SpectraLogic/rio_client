/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class CreateClusterRequest(
    val name: String,
    val pgHost: String,
    val pgPort: Int,
    val pgAdmin: String,
    val pgPassword: String,
    val dbName: String,
    val dboLogin: String,
    val dboPassword: String,
    val queryLogin: String,
    val queryPassword: String
) : RioRequest

@Serializable
data class ClusterResponse(
    val clusterName: String
) : RioResponse()

@Serializable
data class ClusterMembersListResponse(
    val members: List<ClusterMemberData>
) : RioResponse(), RioListResponse<ClusterMemberData> {
    override fun page(): PageInfo {
        val count = members.size.toLong()
        return PageInfo(0, count, 1L, count)
    }
    override fun results() = members
}

@Serializable
data class ClusterMemberResponse(
    val memberId: String,
    val ipAddress: String,
    val clusterPort: Int,
    val httpPort: Int,
    val role: String
) : RioResponse()

@Serializable
data class ClusterMemberData(
    val memberId: String,
    val ipAddress: String,
    val clusterPort: Int,
    val httpPort: Int,
    val role: String
)
