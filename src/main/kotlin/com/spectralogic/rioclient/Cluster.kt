/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

data class ClusterResponse(
    val clusterName: String
) : RioResponse()

data class ClusterMembersListResponse(
    val clusterMembers: List<ClusterMemberResponse>
) : RioResponse()

data class ClusterMemberResponse(
    val memberId: String,
    val ipAddress: String,
    val clusterPort: Int,
    val httpPort: Int,
    val role: String
) : RioResponse()
