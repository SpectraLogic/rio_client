/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// @JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class SpectraDeviceCreateRequest(
    val name: String,
    val mgmtInterface: String,
    val username: String,
    val password: String,
    val dataPath: String? = null
) : RioRequest

// @JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class SpectraDeviceUpdateRequest(
    val mgmtInterface: String,
    val username: String,
    val password: String,
    val dataPath: String? = null
) : RioRequest

@Serializable
data class SpectraDeviceResponse(
    val name: String,
    val username: String,
    val mgmtInterface: String,
    val dataPath: String? = null
) : RioResponse()

@Serializable
data class SpectraDeviceData(
    val name: String,
    val username: String,
    val mgmtInterface: String,
    val dataPath: String? = null
)

@Serializable
data class SpectraDeviceListResponse(
    val devices: List<SpectraDeviceData>,
    val page: PageInfo
) : RioListResponse<SpectraDeviceData>(devices, page)

@Serializable
data class DivaDeviceCreateRequest(
    val name: String,
    val endpoint: String,
    val username: String,
    val password: String
) : RioRequest

@Serializable
data class DivaDeviceUpdateRequest(
    val endpoint: String,
    val username: String,
    val password: String
) : RioRequest

@Serializable
data class DivaDeviceResponse(
    val name: String,
    val endpoint: String,
    val username: String
) : RioResponse()

@Serializable
data class DivaDeviceData(
    val name: String,
    val endpoint: String,
    val username: String
)

@Serializable
data class DivaDeviceListResponse(
    val devices: List<DivaDeviceData>,
    val page: PageInfo
) : RioListResponse<DivaDeviceData>(devices, page)

// @JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class FlashnetDeviceCreateRequest(
    val name: String,
    val host: String,
    val port: Int?,
    val username: String,
    @SerialName("database_host")
    val databaseHost: String,
    @SerialName("database_port")
    val databasePort: Int? = null,
    @SerialName("database_username")
    val databaseUsername: String? = null,
    @SerialName("database_password")
    val databasePassword: String? = null,
    @SerialName("database_name")
    val databaseName: String? = null
) : RioRequest

// @JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class FlashnetDeviceUpdateRequest(
    val host: String,
    val port: Int?,
    val username: String,
    @SerialName("database_host")
    val databaseHost: String,
    @SerialName("database_port")
    val databasePort: Int? = null,
    @SerialName("database_username")
    val databaseUsername: String? = null,
    @SerialName("database_password")
    val databasePassword: String? = null,
    @SerialName("database_name")
    val databaseName: String? = null
) : RioRequest

@Serializable
data class FlashnetDeviceResponse(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val database: FlashnetDeviceDatabaseData
) : RioResponse()

@Serializable
data class FlashnetDeviceData(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val database: FlashnetDeviceDatabaseData
)

@Serializable
data class FlashnetDeviceDatabaseData(
    val host: String,
    val port: String? = null,
    val username: String? = null,
    val name: String? = null
)

@Serializable
data class FlashnetDeviceListResponse(
    val devices: List<FlashnetDeviceData>,
    val page: PageInfo
) : RioListResponse<FlashnetDeviceData>(devices, page)

@Serializable
data class TbpfrDeviceCreateRequest(
    val name: String,
    val endpoint: String,
    val tempStorage: String,
    val allowLazyIndex: Boolean = false
) : RioRequest

@Serializable
data class TbpfrDeviceUpdateRequest(
    val endpoint: String,
    val tempStorage: String,
    val allowLazyIndex: Boolean = false
) : RioRequest

@Serializable
data class TbpfrDeviceResponse(
    val name: String,
    val endpoint: String,
    val tempStorage: String
) : RioResponse()

@Serializable
data class TbpfrDeviceData(
    val name: String,
    val endpoint: String,
    val tempStorage: String
)

@Serializable
data class TbpfrDeviceListResponse(
    val devices: List<TbpfrDeviceData>,
    val page: PageInfo
) : RioListResponse<TbpfrDeviceData>(devices, page)

// @JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class VailDeviceCreateRequest(
    val name: String,
    val accessKey: String,
    val secretKey: String,
    val endpoint: String,
    val port: String? = null,
    val https: String
) : RioRequest

// @JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
data class VailDeviceUpdateRequest(
    val accessKey: String,
    val secretKey: String,
    val endpoint: String,
    val port: String? = null,
    val https: String
) : RioRequest

@Serializable
data class VailDeviceResponse(
    val name: String,
    val endpoint: String,
    val port: Int? = null,
    val https: Boolean,
    val accessKey: String
) : RioResponse()

@Serializable
data class VailDeviceData(
    val name: String,
    val endpoint: String,
    val port: Int? = null,
    val https: Boolean,
    val accessKey: String
)

@Serializable
data class VailDeviceListResponse(
    val devices: List<VailDeviceData>,
    val page: PageInfo
) : RioListResponse<VailDeviceData>(devices, page)

@Serializable
data class DeviceObjectListResponse(
    val objects: List<String>,
    val isTruncated: Boolean
) : RioResponse()
