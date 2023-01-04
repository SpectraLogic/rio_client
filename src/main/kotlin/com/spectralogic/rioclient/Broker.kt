/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BrokerCreateRequest(
    val name: String,
    val agentName: String,
    val agentConfig: Map<String, String>,
    @EncodeDefault val agentType: String = "bp_agent"
) : RioRequest

@Serializable
data class BrokerResponse(
    val name: String,
    val creationDate: String,
    val objectCount: Long
) : RioResponse()

@Serializable
data class BrokerData(
    val name: String,
    val creationDate: String,
    val objectCount: Long
)

@Serializable
sealed class AgentConfig {
    abstract fun toConfigMap(): Map<String, String>
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BpAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String,
    @EncodeDefault val createBucket: Boolean = false,
    @Serializable(with = UUIDSerializer::class)
    val dataPolicyUUID: UUID? = null,
    @EncodeDefault val https: Boolean = false
) : AgentConfig() {
    override fun toConfigMap(): Map<String, String> {
        return buildMap(5) {
            put("bucket", bucket)
            put("blackPearlName", blackPearlName)
            put("username", username)
            put("createBucket", createBucket.toString())
            put("https", https.toString())
            if (dataPolicyUUID != null) put("dataPolicyUUID", dataPolicyUUID.toString())
        }
    }
}

@Serializable
data class VailAgentConfig(
    val vailDeviceName: String,
    val bucket: String
) : AgentConfig() {
    override fun toConfigMap(): Map<String, String> {
        return buildMap {
            put("vailDeviceName", vailDeviceName)
            put("bucket", bucket)
        }
    }
}

@Serializable
data class DivaAgentConfig(
    val divaDeviceName: String,
    val category: String,
    val qos: Int?,
    val priority: Int?
) : AgentConfig() {
    override fun toConfigMap(): Map<String, String> {
        return buildMap {
            put("divaDeviceName", divaDeviceName)
            put("category", category)
            if (qos != null) put("qos", qos.toString())
            if (priority != null) put("priority", priority.toString())
        }
    }
}

@Serializable
data class FlashnetAgentConfig(
    val flashnetDeviceName: String,
    val applicationName: String,
    val storageGroupName: String
) : AgentConfig() {
    override fun toConfigMap(): Map<String, String> {
        return buildMap {
            put("flashnetDeviceName", flashnetDeviceName)
            put("applicationName", applicationName)
            put("storageGroupName", storageGroupName)
        }
    }
}

@Serializable
data class SglLtfsAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String
) : AgentConfig() {
    override fun toConfigMap(): Map<String, String> {
        return buildMap {
            put("bucket", bucket)
            put("blackPearlName", blackPearlName)
            put("username", username)
        }
    }
}

@Serializable
data class AgentCreateRequest(
    val name: String,
    val type: String,
    val agentConfig: Map<String, String>
) : RioRequest

@Serializable
data class AgentUpdateRequest(
    val agentConfig: Map<String, String>
) : RioRequest

@Serializable
data class AgentResponse(
    val name: String,
    val type: String,
    val creationDate: String,
    val lastIndexDate: String? = null,
    val writable: Boolean,
    val agentConfig: Map<String, String>,
    val indexState: String? = null
) : RioResponse()

@Serializable
data class AgentData(
    val name: String,
    val type: String,
    val creationDate: String,
    val lastIndexDate: String? = null,
    val writable: Boolean,
    val agentConfig: Map<String, String>,
    val indexState: String? = null
)

@Serializable
data class ObjectResponse(
    val name: String,
    val size: Long,
    val creationDate: String,
    val broker: String,
    val checksum: Checksum,
    val metadata: Map<String, String>,
    val internalMetadata: Map<String, String>? = null
) : RioResponse()

@Serializable
data class ObjectData(
    val name: String,
    val size: Long,
    val creationDate: String,
    val broker: String,
    val checksum: Checksum,
    val metadata: Map<String, String>,
    val internalMetadata: Map<String, String>? = null
)

@Serializable
data class ObjectBatchUpdateRequest(
    val objects: List<ObjectUpdateRequest>
) : RioRequest

@Serializable
data class ObjectUpdateRequest(
    val name: String,
    val metadata: Map<String, String>
) : RioRequest

@Serializable
data class Checksum(val hash: String, val type: String)

@Serializable
data class ObjectListResponse(
    val objects: List<ObjectData>,
    val page: PageInfo
) : RioResponse()

@Serializable
data class ObjectCountResponse(
    val objectCount: Long
) : RioResponse()

@Serializable
data class ObjectBatchHeadRequest(
    val objects: List<String>
) : RioRequest

@Serializable
data class ObjectBatchHeadResponse(
    val objects: List<ObjectHeadData>
) : RioResponse()

@Serializable
data class ObjectHeadData(
    val name: String,
    val found: Boolean
)

@Serializable
data class BrokerListResponse(
    val brokers: List<BrokerData>,
    val page: PageInfo
) : RioResponse()

@Serializable
data class AgentListResponse(
    val agents: List<AgentData>,
    val page: PageInfo
) : RioResponse()
