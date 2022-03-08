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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SpectraDeviceUpdateRequest(
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
) : RioResponse()

data class SpectraDeviceData(
    val name: String,
    val username: String,
    val mgmtInterface: String,
    val dataPath: String? = null
)

data class SpectraDeviceListResponse(
    val devices: List<SpectraDeviceData>,
    val page: PageInfo
) : RioListResponse<SpectraDeviceData>(devices, page)

data class DivaDeviceCreateRequest(
    val name: String,
    val endpoint: String,
    val username: String,
    val password: String
) : RioRequest

data class DivaDeviceUpdateRequest(
    val endpoint: String,
    val username: String,
    val password: String
) : RioRequest

data class DivaDeviceResponse(
    val name: String,
    val endpoint: String,
    val username: String
) : RioResponse()

data class DivaDeviceData(
    val name: String,
    val endpoint: String,
    val username: String
)

data class DivaDeviceListResponse(
    val devices: List<DivaDeviceData>,
    val page: PageInfo
) : RioListResponse<DivaDeviceData>(devices, page)

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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlashnetDeviceUpdateRequest(
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
    val database: FlashnetDeviceDatabaseData
) : RioResponse()

data class FlashnetDeviceData(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val database: FlashnetDeviceDatabaseData
)

data class FlashnetDeviceDatabaseData(
    val host: String,
    val port: String? = null,
    val username: String? = null,
    val name: String? = null
)

data class FlashnetDeviceListResponse(
    val devices: List<FlashnetDeviceData>,
    val page: PageInfo
) : RioListResponse<FlashnetDeviceData>(devices, page)

data class TbpfrDeviceCreateRequest(
    val name: String,
    val endpoint: String,
    val tempStorage: String,
    val allowLazyIndex: Boolean = false
) : RioRequest

data class TbpfrDeviceUpdateRequest(
    val endpoint: String,
    val tempStorage: String,
    val allowLazyIndex: Boolean = false
) : RioRequest

data class TbpfrDeviceResponse(
    val name: String,
    val endpoint: String,
    val tempStorage: String
) : RioResponse()

data class TbpfrDeviceData(
    val name: String,
    val endpoint: String,
    val tempStorage: String
)

data class TbpfrDeviceListResponse(
    val devices: List<TbpfrDeviceData>,
    val page: PageInfo
) : RioListResponse<TbpfrDeviceData>(devices, page)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VailDeviceCreateRequest(
    val name: String,
    val accessKey: String,
    val secretKey: String,
    val endpoint: String,
    val port: String? = null,
    val https: String
) : RioRequest

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VailDeviceUpdateRequest(
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
    val accessKey: String
) : RioResponse()

data class VailDeviceData(
    val name: String,
    val endpoint: String,
    val port: Int? = null,
    val https: Boolean,
    val accessKey: String
)

data class VailDeviceListResponse(
    val devices: List<VailDeviceData>,
    val page: PageInfo
) : RioListResponse<VailDeviceData>(devices, page)
