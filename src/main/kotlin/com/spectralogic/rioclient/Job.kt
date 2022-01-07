/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import java.net.URI
import java.util.UUID

data class JobStatus(
    val message: String,
    val status: String,
    val reason: String? = null
)

class DetailedJobResponse(
    name: String?,
    id: UUID,
    creationDate: String,
    lastUpdated: String,
    status: JobStatus,
    jobType: JobType,
    numberOfFiles: Long,
    filesTransferred: Long,
    totalSizeInBytes: Long,
    progress: Float,
    val files: List<FileStatus>,
    foreignJobs: Map<UUID, ForeignJobDetails> = mapOf(),
    priority: String? = null
) : JobResponse(
    name,
    id,
    creationDate,
    lastUpdated,
    status,
    jobType,
    numberOfFiles,
    filesTransferred,
    totalSizeInBytes,
    progress,
    foreignJobs,
    priority
)

open class JobResponse(
    val name: String?,
    val id: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val status: JobStatus,
    val jobType: JobType,
    val numberOfFiles: Long,
    val filesTransferred: Long,
    val totalSizeInBytes: Long,
    val progress: Float,
    val foreignJobs: Map<UUID, ForeignJobDetails> = mapOf(),
    val priority: String? = null
)

enum class JobType {
    ARCHIVE, RESTORE
}

data class ForeignJobDetails(
    val id: String,
    val type: String
)

data class JobListResponse(
    val jobs: List<JobResponse>,
    val page: PageInfo
)

data class FileStatus(
    val name: String,
    val status: String,
    val statusMessage: String,
    val foreignJob: UUID? = null,
    val uri: URI,
    val sizeInBytes: Long,
    val lastUpdated: String,
    val broker: String?,
    val agentName: String?
)

data class FileStatusLogResponse(
    val page: PageInfo,
    val fileStatus: List<FileStatusResponse>
)

data class FileStatusResponse(
    val name: String,
    val uri: URI,
    val sizeInBytes: Long,
    val status: String,
    val statusMessage: String,
    val lastUpdated: String,
    val foreignJob: UUID?
)
