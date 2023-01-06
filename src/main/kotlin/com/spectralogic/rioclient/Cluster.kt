/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class ClusterResponse(
    val clusterName: String
) : RioResponse()

@Serializable
data class ClusterMembersListResponse(
    val clusterMembers: List<ClusterMemberData>
) : RioResponse(), RioListResponse<ClusterMemberData> {
    override fun page(): PageInfo {
        val count = clusterMembers.size.toLong()
        return PageInfo(count, count, 1L, count)
    }
    override fun results() = clusterMembers
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
