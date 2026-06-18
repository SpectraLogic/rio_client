/**
 * ***************************************************************************
 *    Copyright 2014-2026 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class BundleNameRequest(
    val bundleName: String,
) : RioRequest

@Serializable
data class BundleNameResponse(
    val bundleName: String,
    val bundleUuid: String,
) : RioResponse()

@Serializable
data class ListBundleNameResponse(
    val page: PageInfo,
    val bundleNames: List<BundleNameResponse>,
) : RioResponse()

@Serializable
data class BundleObjectRequest(
    val bundleObjectKeys: List<String>,
) : RioRequest

@Serializable
data class BundleObjectResponse(
    val objectName: String,
    val objectKey: String,
) : RioResponse()

@Serializable
data class BundleDetailsResponse(
    val bundleName: String,
    val page: PageInfo,
    val bundleMembers: List<BundleObjectResponse>,
) : RioResponse()