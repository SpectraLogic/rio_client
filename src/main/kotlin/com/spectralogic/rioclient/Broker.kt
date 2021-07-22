/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableMap
import java.util.*

data class Broker(
    val name: String
)

data class BrokerCreateRequest(
    val name: String,
    val agentName: String,
    val agentConfig: AgentConfig,
    val agentType: String? = "bp_agent"
): RioRequest

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

fun BpAgentConfig.toImmutableMap(): ImmutableMap<String, String> {
    val builder = ImmutableMap.builder<String, String>()
    builder.put("bucket", this.bucket)
    builder.put("blackPearlName", this.blackPearlName)
    builder.put("username", this.username)
    builder.put("createBucket", this.createBucket.toString())
    builder.put("https", this.https.toString())
    this.dataPolicyUUID?.let { builder.put("dataPolicyUUID", it.toString()) }
    return builder.build()
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
    val agentConfig: ImmutableMap<String, String>
): RioRequest

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
    val metadata: ImmutableMap<String, String>,
    val internalMetadata: ImmutableMap<String, String>? = null
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




