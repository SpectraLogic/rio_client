/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty

data class LogsetResponse(
    val id: String,
    val status: String,
    val creationDate: String
) : RioResponse()

data class LogsetListResponse(
    @JsonProperty("logs") val objects: List<LogsetResponse>,
    val page: PageInfo
) : RioResponse()
