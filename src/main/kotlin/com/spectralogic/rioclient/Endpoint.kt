/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty

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

data class EndpointDeviceListResponse(
    val devices: List<EndpointGenericDeviceData>,
    val page: PageInfo
) : RioListResponse<EndpointGenericDeviceData>(devices, page)

sealed class EndpointDeviceResponse(
    open val name: String,
    open val type: String
) : RioResponse()

sealed class EndpointDeviceData(
    open val name: String,
    open val type: String
)

data class EndpointGenericDeviceResponse(
    override val name: String,
    override val type: String
) : EndpointDeviceResponse(name, type)

open class EndpointGenericDeviceData(
    override val name: String,
    override val type: String
) : EndpointDeviceData(name, type)

data class EndpointFtpDeviceResponse(
    override val name: String,
    override val type: String,
    val endpoint: String,
    val username: String
) : EndpointDeviceResponse(name, type)

data class EndpointS3DeviceResponse(
    override val name: String,
    override val type: String,
    val https: String,
    @JsonProperty("access_id")
    val accessId: String,
    @JsonProperty("secret_key")
    val secretKey: String,
    val region: String
) : EndpointDeviceResponse(name, type)

data class EndpointUriDeviceResponse(
    override val name: String,
    override val type: String,
    val endpoint: String,
) : EndpointDeviceResponse(name, type)
