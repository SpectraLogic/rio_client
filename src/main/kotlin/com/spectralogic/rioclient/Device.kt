/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SpectraDeviceCreateRequest(
    val name: String,
    val mgmtInterface: String,
    val username: String,
    val password: String,
    val dataPath: String? = null
) : RioRequest

data class SpectraDeviceResponse(
    val name: String,
    val username: String,
    val mgmtInterface: String,
    val dataPath: String? = null
)

data class SpectraDeviceListResponse(
    @JsonProperty("devices") override val objects: List<SpectraDeviceResponse>,
    override val page: PageInfo
) : PageData<SpectraDeviceResponse>

data class DivaDeviceCreateRequest(
    val name: String,
    val endpoint: String,
    val username: String,
    val password: String
) : RioRequest

data class DivaDeviceResponse(
    val name: String,
    val endpoint: String,
    val username: String
)

data class DivaDeviceListResponse(
    @JsonProperty("devices") override val objects: List<DivaDeviceResponse>,
    override val page: PageInfo
) : PageData<DivaDeviceResponse>

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlashnetDeviceCreateRequest(
    val name: String,
    val host: String,
    val port: Int?,
    val username: String,
    @JsonProperty("database_host")
    val databaseHost: String,
    @JsonProperty("database_port")
    val databasePort: Int? = null,
    @JsonProperty("database_username")
    val databaseUsername: String? = null,
    @JsonProperty("database_password")
    val databasePassword: String? = null,
    @JsonProperty("database_name")
    val databaseName: String? = null
) : RioRequest

data class FlashnetDeviceResponse(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val database: FlashnetDeviceDatabaseResponse
)

data class FlashnetDeviceDatabaseResponse(
    val host: String,
    val port: String? = null,
    val username: String? = null,
    val name: String? = null
)

data class FlashnetDeviceListResponse(
    @JsonProperty("devices") override val objects: List<FlashnetDeviceResponse>,
    override val page: PageInfo
) : PageData<FlashnetDeviceResponse>

data class TbpfrDeviceCreateRequest(
    val name: String,
    val endpoint: String,
    val tempStorage: String,
    val allowLazyIndex: Boolean = false
) : RioRequest

data class TbpfrDeviceResponse(
    val name: String,
    val endpoint: String,
    val tempStorage: String
)

data class TbpfrDeviceListResponse(
    @JsonProperty("devices") override val objects: List<TbpfrDeviceResponse>,
    override val page: PageInfo
) : PageData<TbpfrDeviceResponse>

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VailDeviceCreateRequest(
    val name: String,
    val accessKey: String,
    val secretKey: String,
    val endpoint: String,
    val port: String? = null,
    val https: String
) : RioRequest

data class VailDeviceResponse(
    val name: String,
    val endpoint: String,
    val port: Int? = null,
    val https: Boolean,
)

data class VailDeviceListResponse(
    @JsonProperty("devices") override val objects: List<VailDeviceResponse>,
    override val page: PageInfo
) : PageData<VailDeviceResponse>

sealed class EndpointDeviceCreateRequest(
    open val name: String,
    val type: String
) : RioRequest

data class FtpEndpointDeviceCreateRequest(
    override val name: String,
    val endpoint: String,
    val username: String,
    val password: String
) : EndpointDeviceCreateRequest(name, "ftp")

data class S3EndpointDeviceCreateRequest(
    override val name: String,
    val https: String,
    val bucket: String,
    @JsonProperty("access_id")
    val accessId: String,
    @JsonProperty("secret_key")
    val secretKey: String,
    val region: String
) : EndpointDeviceCreateRequest(name, "s3")

data class UriEndpointDeviceCreateRequest(
    override val name: String,
    val endpoint: String
) : EndpointDeviceCreateRequest(name, "uri")
