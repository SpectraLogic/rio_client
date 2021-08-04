/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonInclude
import java.net.URI

data class ByteRange(
    val startingIndex: Long,
    val endingIndex: Long
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RestoreRequest(
    val name: String? = null,
    val files: List<FileToRestore>
) : RioRequest

data class FileToRestore(
    val name: String,
    val uri: URI,
    val timeCodeRange: String? = null,
    val byteRange: ByteRange? = null
)
