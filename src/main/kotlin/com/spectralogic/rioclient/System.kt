/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

data class SystemResponse(
    val version: String,
    val apiVersion: String,
    val gitCommitHash: String,
    val buildDate: String,
    val server: ServerResponse,
    val runtimeStats: ProcessStatsResponse
)

data class ServerResponse(
    val jvm: JvmResponse,
    val operatingSystem: OsResponse
)

data class JvmResponse(
    val version: String,
    val vendor: String,
    val vmVersion: String,
    val vmName: String
)

data class OsResponse(
    val name: String,
    val arch: String,
    val version: String,
    val cores: Int
)

data class ProcessStatsResponse(
    val uptime: Long,
    val totalMemory: Long,
    val usedMemory: Long,
    val freeMemory: Long
)
