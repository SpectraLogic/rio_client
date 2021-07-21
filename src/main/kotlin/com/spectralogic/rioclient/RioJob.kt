/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.google.common.collect.ImmutableList
import java.net.URI
import java.util.UUID

data class RioJobStatus(val message: String, val status: String, val reason: String? = null)

enum class RioJobType {
    ARCHIVE, RESTORE
}

class DetailedRioJob(
    name: String?,
    id: UUID,
    creationDate: String,
    lastUpdated: String,
    status: RioJobStatus,
    jobType: RioJobType,
    numberOfFiles: Long,
    filesTransferred: Long,
    totalSizeInBytes: Long,
    progress: Float,
    val files: List<FileStatus>,
    foreignJobs: Map<UUID, ForeignJobDetails> = mapOf()
) : RioJob(
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
    foreignJobs
)

open class RioJob(
    val name: String?,
    val id: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val status: RioJobStatus,
    val jobType: RioJobType,
    val numberOfFiles: Long,
    val filesTransferred: Long,
    val totalSizeInBytes: Long,
    val progress: Float,
    val foreignJobs: Map<UUID, ForeignJobDetails> = mapOf()
)

data class ForeignJobDetails(
    val id: String,
    val type: ForeignJobType
)

data class JobList(
    val jobs: ImmutableList<RioJob>
)


data class FileStatus(
    val name: String,
    val status: String,
    val statusMessage: String,
    val foreignJob: UUID? = null,
    val uri: URI,
    val sizeInBytes: Long,
    val lastUpdated: String
)

enum class ForeignJobType {
    BLACK_PEARL,
    SGL,
    FLASHNET,
    VS3,
    DIVA,
    UNKNOWN;

    companion object {
        fun value(value: String): ForeignJobType {
            return when (value) {
                "bp_agent" -> BLACK_PEARL
                "sgl_ltfs_agent" -> SGL
                "flashnet_agent" -> FLASHNET
                "VS3" -> VS3
                "diva_agent" -> DIVA
                else -> throw RuntimeException("Unknown foreign job type '$value'")
            }
        }
    }
}
