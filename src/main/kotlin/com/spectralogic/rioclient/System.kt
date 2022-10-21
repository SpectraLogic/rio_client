/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import java.util.*

data class SystemResponse(
    val version: String,
    val apiVersion: String,
    val gitCommitHash: String,
    val buildDate: String,
    val server: ServerData,
    val runtimeStats: ProcessStatsData
) : RioResponse()

data class ServerData(
    val jvm: JvmData,
    val operatingSystem: OsData
)

data class JvmData(
    val version: String,
    val vendor: String,
    val vmVersion: String,
    val vmName: String
)

data class OsData(
    val name: String,
    val arch: String,
    val version: String,
    val cores: Int
)

data class ProcessStatsData(
    val uptime: Long,
    val totalMemory: Long,
    val usedMemory: Long,
    val freeMemory: Long
)

data class ClientDataRequest(
    val clientDataId: String,
    val clientName: String,
    val tag: String,
    val mapData: Map<String, String>
) : RioRequest

data class ClientDataResponse(
    val dataId: UUID,
    val creationDate: String,
    val clientDataId: String,
    val clientName: String,
    val tag: String,
    val mapData: Map<String, String>
) : RioResponse()

data class ClientDataListResponse(
    val result: List<ClientData>,
    val page: PageInfo
) : RioListResponse<ClientData>(result, page)

data class ClientData(
    val dataId: UUID,
    val creationDate: String,
    val clientDataId: String,
    val clientName: String,
    val tag: String,
    val mapData: Map<String, String>
)