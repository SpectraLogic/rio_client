/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableMap
import com.spectralogic.rioclient.PageInfo
import java.util.*

data class Broker(
    val name: String
)

data class CreateBroker(
    val name: String,
    val agentName: String,
    val agentConfig: AgentConfig,
    val agentType: String? = "bp_agent"
): RioRequest

sealed class AgentConfig

data class BpAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String,
    val createBucket: Boolean = false,
    val dataPolicyUUID: UUID? = null,
    val https: Boolean = false
) : AgentConfig()

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

data class CreateGenericAgentRequest(
    val name: String,
    val type: String,
    val agentConfig: ImmutableMap<String, String>
): RioRequest

data class CreateGenericAgentResponse(
    val name: String,
    val type: String,
    val writable: Boolean,
    val agentConfig: ImmutableMap<String, String>
)

data class ObjectDetails(
    val name: String,
    val size: Long,
    val creationDate: String,
    val broker: String,
    val metadata: ImmutableMap<String, String>,
    val internalMetadata: ImmutableMap<String, String>? = null
)


data class ListObjectMetadata(
    override val objects: List<ObjectDetails>,
    override val page: PageInfo
) : PageData<ObjectDetails>

data class ListBrokersResponse(
    @JsonProperty("brokers") override val objects: List<BrokersResponse>,
    override val page: PageInfo
) : PageData<BrokersResponse>

data class BrokersResponse(
    val name: String,
    val creationDate: String,
    val objectCount: Int
)

data class ListAgentsResponse(
    val agents: List<AgentInfo>,
    val page: PageInfo
)

data class ListAgents(
    @JsonProperty("agents") override val objects: List<AgentInfo>,
    override val page: PageInfo
) : PageData<AgentInfo>

data class AgentInfo(
    val name: String,
    val type: String,
    val creationDate: String,
    val lastIndexDate: String,
    val writable: Boolean,
    val agentConfig: Map<String, String>,
    val indexState: String?
)


