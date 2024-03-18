/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

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

data class ClusterResponse(
    val clusterName: String
) : RioResponse()

data class ClusterMembersListResponse(
    val members: List<ClusterMemberData>
) : RioResponse()

data class ClusterMemberResponse(
    val memberId: String,
    val ipAddress: String,
    val clusterPort: Int,
    val httpPort: Int,
    val role: String
) : RioResponse()

data class ClusterMemberData(
    val memberId: String,
    val ipAddress: String,
    val clusterPort: Int,
    val httpPort: Int,
    val role: String
)
