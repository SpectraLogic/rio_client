/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class ArchiveRequest(
    val name: String? = null,
    val files: List<FileToArchive>,
    val metadata: Map<String, String>? = null,
    val callbacks: List<JobCallback>? = null,
) : RioRequest

@Serializable
data class FileToArchive(
    val name: String,
    @Serializable(with = URISerializer::class)
    val uri: URI,
    val size: Long?,
    val metadata: Map<String, String>? = null,
    val indexMedia: Boolean = false,
    val deleteAfterArchive: Boolean = false,
)

@Serializable
data class ArchiveFolderRequest(
    val jobName: String? = null,
    val prefix: String? = null,
    val files: List<FileToArchive>? = null,
    val folders: List<FolderToArchive>? = null,
    val callbacks: List<JobCallback>? = null,
) : RioRequest

@Serializable
data class FolderToArchive(
    @Serializable(with = URISerializer::class)
    val uri: URI,
    val metadata: Map<String, String>? = null,
    val deleteAfterArchive: Boolean = false,
)
