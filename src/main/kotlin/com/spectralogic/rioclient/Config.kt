/**
 * ***************************************************************************
 *    Copyright 2014-2024 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class ActiveDirectoryRequest(
    val domain: String,
    val ldapServer: String,
    val port: Int,
    val tls: Boolean,
    val allowAny: Boolean,
    val defaultRole: String,
) : RioRequest

@Serializable
data class ActiveDirectoryResponse(
    val domain: String,
    val ldapServer: String,
    val port: Int,
    val tls: Boolean,
    val allowAny: Boolean,
    val defaultRole: String,
) : RioResponse()

@Serializable
data class CacheConfigRequest(
    val uri: String,
    val maximumBytes: Long,
) : RioRequest

@Serializable
data class CacheConfigResponse(
    val uri: String,
    val maximumBytes: Long,
) : RioResponse()

@Serializable
data class CacheStatusResponse(
    val size: Long,
    val required: Long,
    val reserved: Long,
    val available: Long,
    val hits: Long,
    val misses: Long,
    val free: Long,
    val remaining: Long,
    val isFull: Boolean,
    val used: Long,
    val efficiency: Double,
) : RioResponse()
