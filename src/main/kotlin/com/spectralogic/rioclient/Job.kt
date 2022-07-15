/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import java.net.URI
import java.net.URL
import java.util.Collections.emptyMap
import java.util.UUID

data class JobStatus(
    val message: String,
    val status: String,
    val reason: String? = null
)

data class JobCallback(
    val url: URL,
    val eventClass: String,
    val eventType: String
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
    foreignJobs: Map<UUID, ForeignJobDetails> = emptyMap(),
    priority: String? = null,
    callback: JobCallbackData? = null
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
    priority,
    callback = callback
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
    val foreignJobs: Map<UUID, ForeignJobDetails> = emptyMap(),
    val priority: String? = null,
    val sessionId: String? = null,
    val callback: JobCallbackData? = null
) : RioResponse()

data class JobCallbackData(
    val url: String,
    val eventClass: String,
    val eventType: String
)

data class JobData(
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
    val foreignJobs: Map<UUID, ForeignJobDetails> = emptyMap(),
    val priority: String? = null,
    val callback: JobCallbackData? = null
)

enum class JobType {
    ARCHIVE, RESTORE
}

data class ForeignJobDetails(
    val id: String,
    val type: String
)

data class JobListResponse(
    val jobs: List<JobData>,
    val page: PageInfo
) : RioListResponse<JobData>(jobs, page)

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
) : RioResponse()

data class FileStatusResponse(
    val name: String,
    val uri: URI,
    val sizeInBytes: Long,
    val status: String,
    val statusMessage: String,
    val lastUpdated: String,
    val foreignJob: UUID?
) : RioResponse()
