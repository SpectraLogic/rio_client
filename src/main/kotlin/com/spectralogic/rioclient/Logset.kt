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
)

data class LogsetListResponse(
    @JsonProperty("logs") override val objects: List<LogsetResponse>,
    override val page: PageInfo
) : PageData<LogsetResponse>
