/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

data class BrokerCreateRequest(
    val name: String,
    val agentName: String,
    val agentConfig: AgentConfig,
    val agentType: String? = "bp_agent"
) : RioRequest

data class BrokerResponse(
    val name: String,
    val creationDate: String,
    val objectCount: Long
) : RioResponse()

data class BrokerData(
    val name: String,
    val creationDate: String,
    val objectCount: Long
)

sealed class AgentConfig

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BpAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String,
    val createBucket: Boolean = false,
    val dataPolicyUUID: UUID? = null,
    val https: Boolean = false,
    val protect: Boolean = false
) : AgentConfig()

fun BpAgentConfig.toConfigMap(): Map<String, String> {
    return buildMap {
        put("bucket", bucket)
        put("blackPearlName", blackPearlName)
        put("username", username)
        put("createBucket", createBucket.toString())
        put("https", https.toString())
        put("protect", protect.toString())
        if (dataPolicyUUID != null) put("dataPolicyUUID", dataPolicyUUID.toString())
    }
}

data class VailAgentConfig(
    val vailDeviceName: String,
    val bucket: String
) : AgentConfig()

fun VailAgentConfig.toConfigMap(): Map<String, String> {
    return buildMap {
        put("vailDeviceName", vailDeviceName)
        put("bucket", bucket)
    }
}

data class DivaAgentConfig(
    val divaDeviceName: String,
    val category: String,
    val qos: Int?,
    val priority: Int?
) : AgentConfig()

fun DivaAgentConfig.toConfigMap(): Map<String, String> {
    return buildMap {
        put("divaDeviceName", divaDeviceName)
        put("category", category)
        if (qos != null) put("qos", qos.toString())
        if (priority != null) put("priority", priority.toString())
    }
}

data class FlashnetAgentConfig(
    val flashnetDeviceName: String,
    val applicationName: String,
    val storageGroupName: String
)

fun FlashnetAgentConfig.toConfigMap(): Map<String, String> {
    return buildMap {
        put("flashnetDeviceName", flashnetDeviceName)
        put("applicationName", applicationName)
        put("storageGroupName", storageGroupName)
    }
}

data class SglLtfsAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String
)

fun SglLtfsAgentConfig.toConfigMap(): Map<String, String> {
    return buildMap {
        put("bucket", bucket)
        put("blackPearlName", blackPearlName)
        put("username", username)
    }
}

data class AgentCreateRequest(
    val name: String,
    val type: String,
    val agentConfig: Map<String, String>
) : RioRequest

data class AgentUpdateRequest(
    val agentConfig: Map<String, String>
) : RioRequest

data class AgentResponse(
    val name: String,
    val type: String,
    val creationDate: String,
    val lastIndexDate: String?,
    val writable: Boolean,
    val agentConfig: Map<String, String>,
    val indexState: String?
) : RioResponse()

data class AgentData(
    val name: String,
    val type: String,
    val creationDate: String,
    val lastIndexDate: String?,
    val writable: Boolean,
    val agentConfig: Map<String, String>,
    val indexState: String?
)

data class ObjectResponse(
    val name: String,
    val size: Long,
    val creationDate: String,
    val broker: String,
    val checksum: Checksum,
    val metadata: Map<String, String>,
    val internalMetadata: Map<String, String>? = null
) : RioResponse()

data class ObjectData(
    val name: String,
    val size: Long,
    val creationDate: String,
    val broker: String,
    val checksum: Checksum,
    val metadata: Map<String, String>,
    val internalMetadata: Map<String, String>? = null
)

data class ObjectBatchUpdateRequest(
    val objects: List<ObjectUpdateRequest>
) : RioRequest

data class ObjectUpdateRequest(
    val name: String,
    val metadata: Map<String, String>
) : RioRequest

data class Checksum(val hash: String, val type: String)

data class ObjectListResponse(
    val objects: List<ObjectData>,
    val page: PageInfo
) : RioListResponse<ObjectData>(objects, page)

data class ObjectCountResponse(
    val objectCount: Long
) : RioResponse()

data class ObjectBatchHeadRequest(
    val objects: List<String>
) : RioRequest

data class ObjectBatchHeadResponse(
    val objects: List<ObjectHeadData>
) : RioResponse()
data class ObjectHeadData(
    val name: String,
    val found: Boolean
)

data class BrokerListResponse(
    val brokers: List<BrokerData>,
    val page: PageInfo
) : RioListResponse<BrokerData>(brokers, page)

data class AgentListResponse(
    val agents: List<AgentData>,
    val page: PageInfo
) : RioListResponse<AgentData>(agents, page)
