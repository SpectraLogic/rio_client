/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty
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
)

sealed class AgentConfig

data class BpAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String,
    val createBucket: Boolean = false,
    val dataPolicyUUID: UUID? = null,
    val https: Boolean = false
) : AgentConfig()

fun BpAgentConfig.toMap(): Map<String, String> {
    val map = mapOf(
        Pair("bucket", this.bucket),
        Pair("blackPearlName", this.blackPearlName),
        Pair("username", this.username),
        Pair("createBucket", this.createBucket.toString()),
        Pair("https", this.https.toString()),
        Pair("bucket", this.bucket)
    )
    return this.dataPolicyUUID?.let { map.plus(Pair("dataPolicyUUID", it.toString())) }
        ?: map
}

data class Vs3AgentConfig(
    val vs3DeviceName: String,
    val bucket: String
) : AgentConfig()

data class DivaAgentConfig(
    val divaDeviceName: String,
    val category: String,
    val qos: Int?,
    val priority: Int?
)

data class FlashnetAgentConfig(
    val flashnetDeviceName: String,
    val applicationName: String,
    val storageGroupName: String
)

data class SglLtfsAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String
)

data class AgentCreateRequest(
    val name: String,
    val type: String,
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
)

data class ListAgents(
    @JsonProperty("agents") override val objects: List<AgentResponse>,
    override val page: PageInfo
) : PageData<AgentResponse>

data class ObjectResponse(
    val name: String,
    val size: Long,
    val creationDate: String,
    val broker: String,
    val metadata: Map<String, String>,
    val internalMetadata: Map<String, String>? = null
)

data class ObjectListResponse(
    override val objects: List<ObjectResponse>,
    override val page: PageInfo
) : PageData<ObjectResponse>

data class BrokersListResponse(
    @JsonProperty("brokers") override val objects: List<BrokerResponse>,
    override val page: PageInfo
) : PageData<BrokerResponse>

data class AgentsListResponse(
    val agents: List<AgentResponse>,
    val page: PageInfo
)
