/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableList
import java.net.URI
import java.util.*

data class FileStatusLogResponse(
    val page: PageInfo,
    val fileStatus: ImmutableList<FileStatusResponse>
)

data class FileStatusResponse
@JsonCreator constructor(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("uri")
    val uri: URI,
    @JsonProperty("sizeInBytes")
    val sizeInBytes: Long,
    @JsonProperty("status")
    val status: String,
    @JsonProperty("statusMessage")
    val statusMessage: String,
    @JsonProperty("lastUpdated")
    val lastUpdated: String,
    @JsonProperty("foreignJob")
    val foreignJob: UUID?
)
