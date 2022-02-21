/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty

data class EndpointDeviceListResponse(
    @JsonProperty("devices") val objects: List<EndpointGenericDeviceResponse>,
    val page: PageInfo
) : RioResponse()

sealed class EndpointDeviceResponse(
    open val name: String,
    open val type: String
) : RioResponse()

data class EndpointGenericDeviceResponse(
    override val name: String,
    override val type: String
) : EndpointDeviceResponse(name, type)

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
