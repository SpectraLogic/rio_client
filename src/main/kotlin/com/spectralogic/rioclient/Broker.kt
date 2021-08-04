/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableMap
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BpAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String,
    val createBucket: Boolean = false,
    val dataPolicyUUID: UUID? = null,
    val https: Boolean = false
) : AgentConfig()

fun BpAgentConfig.toConfigMap(): Map<String, String> {
    val builder = ImmutableMap.builder<String, String>()
    builder.put("bucket", bucket)
    builder.put("blackPearlName", blackPearlName)
    builder.put("username", username)
    builder.put("createBucket", createBucket.toString())
    builder.put("https", https.toString())
    dataPolicyUUID?.let { builder.put("dataPolicyUUID", it.toString()) }
    return builder.build()
}

data class Vs3AgentConfig(
    val vs3DeviceName: String,
    val bucket: String
) : AgentConfig()

fun Vs3AgentConfig.toConfigMap(): Map<String, String> {
    val builder = ImmutableMap.builder<String, String>()
    builder.put("vs3DeviceName", vs3DeviceName)
    builder.put("bucket", bucket)
    return builder.build()
}

data class DivaAgentConfig(
    val divaDeviceName: String,
    val category: String,
    val qos: Int?,
    val priority: Int?
)

fun DivaAgentConfig.toConfigMap(): Map<String, String> {
    val builder = ImmutableMap.builder<String, String>()
    builder.put("divaDeviceName", divaDeviceName)
    builder.put("category", category)
    qos?.let { builder.put("qos", it.toString()) }
    priority?.let { builder.put("priority", it.toString()) }
    return builder.build()
}

data class FlashnetAgentConfig(
    val flashnetDeviceName: String,
    val applicationName: String,
    val storageGroupName: String
)

fun FlashnetAgentConfig.toConfigMap(): Map<String, String> {
    val builder = ImmutableMap.builder<String, String>()
    builder.put("flashnetDeviceName", flashnetDeviceName)
    builder.put("applicationName", applicationName)
    builder.put("storageGroupName", storageGroupName)
    return builder.build()
}

data class SglLtfsAgentConfig(
    val bucket: String,
    val blackPearlName: String,
    val username: String
)

fun SglLtfsAgentConfig.toConfigMap(): Map<String, String> {
    val builder = ImmutableMap.builder<String, String>()
    builder.put("bucket", bucket)
    builder.put("blackPearlName", blackPearlName)
    builder.put("username", username)
    return builder.build()
}

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

data class BrokerListResponse(
    @JsonProperty("brokers") override val objects: List<BrokerResponse>,
    override val page: PageInfo
) : PageData<BrokerResponse>

data class AgentListResponse(
    @JsonProperty("agents") override val objects: List<AgentResponse>,
    override val page: PageInfo
) : PageData<AgentResponse>
