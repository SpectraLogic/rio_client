/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface EndpointDeviceCreateRequest {
    val name: String
    val type: String
}

@Serializable
data class FtpEndpointDeviceCreateRequest(
    override val name: String,
    val endpoint: String,
    val username: String,
    val password: String,
    @EncodeDefault override val type: String = "ftp"
) : EndpointDeviceCreateRequest, RioRequest

@Serializable
data class S3EndpointDeviceCreateRequest(
    override val name: String,
    val https: String,
    val bucket: String,
    @SerialName("access_id")
    val accessId: String,
    @SerialName("secret_key")
    val secretKey: String,
    val region: String,
    @EncodeDefault override val type: String = "s3"
) : EndpointDeviceCreateRequest, RioRequest

@Serializable
data class UriEndpointDeviceCreateRequest(
    override val name: String,
    val endpoint: String,
    @EncodeDefault override val type: String = "uri"
) : EndpointDeviceCreateRequest, RioRequest

@Serializable
data class EndpointDeviceListResponse(
    val devices: List<EndpointGenericDeviceData>,
    val page: PageInfo
) : RioResponse(), RioListResponse<EndpointGenericDeviceData> {
    override fun page() = page
    override fun results() = devices
}

interface EndpointDeviceResponse {
    val name: String
    val type: String
}

interface EndpointDeviceData {
    val name: String
    val type: String
}

@Serializable
data class EndpointGenericDeviceResponse(
    override val name: String,
    override val type: String
) : EndpointDeviceResponse, RioResponse()

@Serializable
open class EndpointGenericDeviceData(
    override val name: String,
    override val type: String
) : EndpointDeviceData

@Serializable
data class EndpointFtpDeviceResponse(
    override val name: String,
    override val type: String,
    val endpoint: String,
    val username: String
) : EndpointDeviceResponse, RioResponse()

@Serializable
data class EndpointS3DeviceResponse(
    override val name: String,
    override val type: String,
    val https: String,
    @SerialName("access_id")
    val accessId: String,
    @SerialName("secret_key")
    val secretKey: String,
    val region: String
) : EndpointDeviceResponse, RioResponse()

@Serializable
data class EndpointUriDeviceResponse(
    override val name: String,
    override val type: String,
    val endpoint: String
) : EndpointDeviceResponse, RioResponse()
