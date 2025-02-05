/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class ByteRange(
    val startingIndex: Long,
    val endingIndex: Long,
)

@Serializable
data class RestoreRequest(
    val name: String? = null,
    val files: List<FileToRestore>,
    val callbacks: List<JobCallback>? = null,
) : RioRequest

@Serializable
data class FileToRestore(
    val name: String,
    @Serializable(with = URISerializer::class)
    val uri: URI,
    val timeCodeRange: String? = null,
    val byteRange: ByteRange? = null,
)
