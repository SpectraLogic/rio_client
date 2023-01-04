/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.Serializable
import java.net.URI
import java.util.Collections.emptyMap
import java.util.UUID

@Serializable
data class JobStatus(
    val message: String,
    val status: String,
    val reason: String? = null
)

@Serializable
data class JobCallback(
    val url: String,
    val eventClass: String,
    val eventType: String
)

@Serializable
open class DetailedJobResponse(
    val name: String?,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val status: JobStatus,
    val jobType: JobType,
    val numberOfFiles: Long,
    val filesTransferred: Long,
    val totalSizeInBytes: Long,
    val progress: Float,
    val files: List<FileStatus>,
    val foreignJobs: Map<@Serializable(with = UUIDSerializer::class)UUID, ForeignJobDetails> = emptyMap(),
    val priority: String? = null,
    val callbacks: List<JobCallback>? = null
) : RioResponse()

@Serializable
open class JobResponse(
    val name: String?,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val status: JobStatus,
    val jobType: JobType,
    val numberOfFiles: Long,
    val filesTransferred: Long,
    val totalSizeInBytes: Long,
    val progress: Float,
    val foreignJobs: Map<@Serializable(with = UUIDSerializer::class)UUID, ForeignJobDetails> = emptyMap(),
    val priority: String? = null,
    val sessionId: String? = null,
    val callbacks: List<JobCallback>? = null
) : RioResponse()

@Serializable
data class JobData(
    val name: String?,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val status: JobStatus,
    val jobType: JobType,
    val numberOfFiles: Long,
    val filesTransferred: Long,
    val totalSizeInBytes: Long,
    val progress: Float,
    val foreignJobs: Map<@Serializable(with = UUIDSerializer::class)UUID, ForeignJobDetails> = emptyMap(),
    val priority: String? = null,
    val callbacks: List<JobCallback>? = null
)

@Serializable
enum class JobType {
    ARCHIVE, RESTORE
}

@Serializable
data class ForeignJobDetails(
    val id: String,
    val type: String
)

@Serializable
data class JobListResponse(
    val jobs: List<JobData>,
    val page: PageInfo
) : RioResponse()

@Serializable
data class FileStatus(
    val name: String,
    val status: String,
    val statusMessage: String,
    @Serializable(with = UUIDSerializer::class)
    val foreignJob: UUID? = null,
    @Serializable(with = URISerializer::class)
    val uri: URI,
    val sizeInBytes: Long,
    val lastUpdated: String,
    val broker: String?,
    val agent: String?,
    val fileId: String
)

@Serializable
data class FileStatusLogResponse(
    val page: PageInfo,
    val fileStatus: List<FileStatusResponse>
) : RioResponse()

@Serializable
data class FileStatusResponse(
    val name: String,
    @Serializable(with = URISerializer::class)
    val uri: URI,
    val sizeInBytes: Long,
    val status: String,
    val statusMessage: String,
    val lastUpdated: String,
    @Serializable(with = UUIDSerializer::class)
    val foreignJob: UUID?
) : RioResponse()
