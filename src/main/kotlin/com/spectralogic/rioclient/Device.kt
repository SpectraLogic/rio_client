/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateSpectraDevice(
    val name: String,
    val mgmtInterface: String,
    val username: String,
    val password: String,
    val dataPath: String? = null
): RioRequest

data class SpectraDeviceResponse(val name: String, val username: String, val mgmtInterface: String, val dataPath: String? = null)
data class ListSpectraDevicesResponse(val devices: List<SpectraDeviceResponse>, val page: PageInfo)


data class CreateFlashnetDevice(
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
): RioRequest

data class FlashnetDeviceResponse(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    @JsonProperty("database_host")
    val databaseHost: String,
    @JsonProperty("database_port")
    val databasePort: Int? = null,
    @JsonProperty("database_username")
    val databaseUsername: String? = null
)

data class ListFlashnetDevicesResponse(val devices: List<FlashnetDeviceResponse>, val page: PageInfo)

data class TbpfrDeviceResponse(
    val name: String,
    val endpoint: String,
    val tempStorage: String
)

data class ListTbpfrDevicesResponse(val devices: List<TbpfrDeviceResponse>, val page: PageInfo)

sealed class CreateEndpointDevice(open val name: String, val type: String): RioRequest

data class CreateFtpEndpointDevice(
    override val name: String,
    val endpoint: String,
    val username: String,
    val password: String
) : CreateEndpointDevice(name, "ftp")

data class CreateS3EndpointDevice(
    override val name: String,
    val https: String,
    val bucket: String,
    @JsonProperty("access_id")
    val accessId: String,
    @JsonProperty("secret_key")
    val secretKey: String,
    val region: String
) : CreateEndpointDevice(name, "s3")

data class CreateUriEndpointDevice(
    override val name: String,
    val endpoint: String
) : CreateEndpointDevice(name, "uri")


