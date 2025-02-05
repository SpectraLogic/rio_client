/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class LogsetResponse(
    val id: String,
    val status: String,
    val creationDate: String,
) : RioResponse()

@Serializable
data class LogsetData(
    val id: String,
    val status: String,
    val creationDate: String,
)

@Serializable
data class LogsetListResponse(
    val logs: List<LogsetData>,
    val page: PageInfo,
) : RioResponse(),
    RioListResponse<LogsetData> {
    override fun page() = page

    override fun results() = logs
}

@Serializable
data class LogLevelResponse(
    val currentLevel: String,
) : RioResponse()

@Serializable
data class LogLevelSetResponse(
    val currentLevel: String,
    val requestedLevel: String,
    val previousLevel: String,
    val status: String,
    val message: String,
) : RioResponse()
