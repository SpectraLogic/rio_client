/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import java.net.URI

data class ArchiveRequest(
    val name: String? = null,
    val files: List<FileToArchive>
): RioRequest

data class FileToArchive(
    val name: String,
    val uri: URI,
    val size: Long?,
    val metadata: Map<String, String>? = null,
    val indexMedia: Boolean = false,
    val `upload-new-files-only`: Boolean = true
)
