/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import java.net.URI
import java.util.Collections.emptyMap
import java.util.UUID

enum class JobStatusEnum(val isFinal: Boolean = false) {
    ACTIVE,
    COMPLETED(true),
    CANCELED(true),
    ERROR(true),
    UNKNOWN;

    companion object {
        fun parse(status: String): JobStatusEnum {
            return try {
                JobStatusEnum.valueOf(status)
            } catch (_: IllegalArgumentException ) {
                UNKNOWN
            }
        }
    }
}

enum class FileStatusEnum(val isFinal: Boolean = false) {
    Completed(true),
    Error(true),
    Initializing,
    Indexing,
    Copying,
    Transferring,
    Rewrapping,
    UNKNOWN;

    companion object {
        fun parse(status: String): FileStatusEnum {
            return try {
                FileStatusEnum.valueOf(status)
            } catch (_: IllegalArgumentException ) {
                UNKNOWN
            }
        }
    }
}

data class JobStatus(
    val message: String,
    val status: String,
    val reason: String? = null
)

data class JobCallback(
    val url: String,
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
    callbacks: List<JobCallback>? = null
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
    callbacks = callbacks
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
    val callbacks: List<JobCallback>? = null
) : RioResponse()

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
    val callbacks: List<JobCallback>? = null
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
    val agentName: String?,
    val fileId: String
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
