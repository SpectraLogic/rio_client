/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SystemResponse(
    val version: String,
    val apiVersion: String,
    val gitCommitHash: String,
    val buildDate: String,
    val server: ServerData,
    val runtimeStats: ProcessStatsData,
    val buildNumber: String,
    val buildType: String
) : RioResponse()

@Serializable
data class ServerData(
    val jvm: JvmData,
    val operatingSystem: OsData
)

@Serializable
data class JvmData(
    val version: String,
    val vendor: String,
    val vmVersion: String,
    val vmName: String
)

@Serializable
data class OsData(
    val name: String,
    val arch: String,
    val version: String,
    val cores: Int
)

@Serializable
data class ProcessStatsData(
    val uptime: Long,
    val totalMemory: Long,
    val usedMemory: Long,
    val freeMemory: Long
)

@Serializable
data class ClientDataRequest(
    val clientDataId: String,
    val clientName: String,
    val tag: String,
    val mapData: Map<String, String>
) : RioRequest

@Serializable
data class ClientDataResponse(
    @Serializable(with = UUIDSerializer::class)
    val dataId: UUID,
    val creationDate: String,
    val clientDataId: String,
    val clientName: String,
    val tag: String,
    val mapData: Map<String, String>
) : RioResponse()

@Serializable
data class ClientDataListResponse(
    @SerialName("result")
    val result: List<ClientData>,
    @SerialName("page")
    val page: PageInfo
) : RioListResponse<ClientData>(result, page)

@Serializable
data class ClientData(
    @Serializable(with = UUIDSerializer::class)
    val dataId: UUID,
    val creationDate: String,
    val clientDataId: String,
    val clientName: String,
    val tag: String,
    val mapData: Map<String, String>
)