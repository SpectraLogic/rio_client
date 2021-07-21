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
)

data class LogsetListResponse(
    val logs: List<LogsetResponse>,
    val page: PageInfo
)
