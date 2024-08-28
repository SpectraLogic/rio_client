/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

data class LogsetResponse(
    val id: String,
    val status: String,
    val creationDate: String
) : RioResponse()

data class LogsetData(
    val id: String,
    val status: String,
    val creationDate: String
)

data class LogsetListResponse(
    val logs: List<LogsetData>,
    val page: PageInfo
) : RioListResponse<LogsetData>(logs, page)

data class LogLevelResponse(
    val currentLevel: String
) : RioResponse()

data class LogLevelSetResponse(
    val currentLevel: String,
    val requestedLevel: String,
    val previousLevel: String,
    val status: String,
    val message: String
) : RioResponse()
