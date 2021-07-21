/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import nl.altindag.ssl.util.TrustManagerUtils
import retrofit2.Response
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.Closeable
import java.net.URL
import java.util.UUID

interface RioRequest
// interface RioResponse

class RioClient(rioUrl: URL, val username: String = "spectra", val password: String = "spectra") : Closeable {

    private data class EmptyRequest(val blank: String) : RioRequest
    private data class MyMetadata (val metadata: Map<String, String>): RioRequest

    private val myEmptyRequest = EmptyRequest("")
    private val api by lazy { "${rioUrl}/api" }

    private val tokenContainer: TokenResponse by lazy {
        runBlocking {
            getShortToken()
        }
    }
    private val client by lazy {
        HttpClient(CIO) {
            engine {
                https {
                    this.trustManager = TrustManagerUtils.createUnsafeTrustManager()
                }
            }
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(KotlinModule())
                    registerModule(JavaTimeModule())
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                    configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                }
            }
        }
    }

    /**
     * Token (do not use myPost, will cause infinite loop)
     */
    private suspend fun getShortToken(): TokenResponse {
        return client.post("$api/tokens") {
            contentType(ContentType.Application.Json)
            body = UserLoginCredentials(username, password)
        }
    }

    private suspend fun createNewToken(shortToken: String): CreateTokenResponse {
        return client.post("$api/keys") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $shortToken")
        }
    }

    // DWL TODO:     suspend fun createApiTokenWithExpiration(@Body request: CreateLongLivedToken): CreateTokenResponse

    suspend fun getApiToken(id: UUID): ApiKeyResponse =
        client.myGet("$api/keys/$id")

    private suspend fun deleteApiToken(id: UUID): Response<Void> =
        client.myDelete("$api/keys/$id")

    private suspend fun headApiToken(id: UUID): Boolean =
        client.myHead("$api/keys/$id")

    /**
     * Cluster
     */
    suspend fun deleteCluster(): Response<Void> =
        client.myPost("$api/cluster")

    suspend fun listClusterMembers(): ClusterMembersList =
        client.myGet("$api/cluster/members/")

    /**
     * Device
     */

    // Spectra
    suspend fun createSpectraDevice(createSpectraDevice: CreateSpectraDevice): SpectraDeviceResponse =
        client.myPost("$api/devices/spectra", createSpectraDevice)

    suspend fun deleteSpectraDevice(name: String): Response<Void> =
        client.myDelete("$api/devices/spectra/$name")

    suspend fun getSpectraDevice(name: String): SpectraDeviceResponse =
        client.myGet("$api/devices/spectra/$name")

    suspend fun headSpectraDevice(@Path("deviceName") name: String): Boolean =
        client.myHead("$api/devices/spectra/$name")

    suspend fun listSpectraDevices(): ListSpectraDevicesResponse =
        client.myGet("$api/devices/spectra")

    // Flashnet
    suspend fun createFlashnetDevice(createFlashnetDevice: CreateFlashnetDevice): FlashnetDeviceResponse =
        client.myPost("$api/devices/flashnet", createFlashnetDevice)

    // DWL TODO: flashnet delete, get, head

    suspend fun listFlashnetDevice(): ListFlashnetDevicesResponse =
        client.myGet("$api/devices/spectra")

    // TBPFR
    suspend fun listTbpfrDevice(): ListTbpfrDevicesResponse =
        client.myGet("$api/devices/spectra")

    // DWL TODO: tbpfr create, delete, get, head


    /**
     * Endpoint
     */
    suspend fun deleteEndpointDevice(name: String): Response<Void> =
        client.myDelete("$api/devices/endpoint/$name")

    suspend fun getEndpointDevice(name: String): Response<EndpointGenericDeviceResponse> =
        client.myGet("$api/devices/endpoint/$name")

    suspend fun headEndpointDevice(name: String): Boolean =
        client.myHead("$api/devices/endpoint/$name")

    suspend fun listEndpointDevices(): ListEndpointDevicesResponse =
        client.myGet("$api/devices/endpoint")

    suspend fun createFtpEndpointDevice(createFtpEndpointDevice: CreateFtpEndpointDevice): Response<EndpointFtpDeviceResponse> =
        client.myPost("$api/devices/endpoint", createFtpEndpointDevice)

    suspend fun getFtpEndpointDevice(name: String): Response<EndpointFtpDeviceResponse> =
        client.myGet("$api/devices/endpoint/$name")

    suspend fun createS3EndpointDevice(createS3EndpointDevice: CreateS3EndpointDevice): Response<EndpointS3DeviceResponse> =
        client.myPost("$api/devices/endpoint", createS3EndpointDevice)

    suspend fun getS3EndpointDevice(name: String): Response<EndpointS3DeviceResponse> =
        client.myGet("$api/devices/endpoint/$name")

    suspend fun createUriEndpointDevice(createUriEndpointDevice: CreateUriEndpointDevice): Response<EndpointUriDeviceResponse> =
        client.myPost("$api/devices/endpoint", createUriEndpointDevice)

    suspend fun getUriEndpointDevice(name: String): Response<EndpointUriDeviceResponse> =
        client.myGet("$api/devices/endpoint/$name")

    /**
     * Broker
     */
    suspend fun createBroker(createBroker: CreateBroker): Broker =
        client.myPost("$api/brokers", createBroker)

    suspend fun deleteBroker(brokerName: String, force: Boolean?): Response<Void> =
        client.myDelete("$api/brokers/$brokerName", "force", force)

    suspend fun getBroker(brokerName: String): Broker =
        client.myGet("$api/brokers/$brokerName")

    suspend fun headBroker(brokerName: String): Boolean =
        client.myHead("$api/brokers/$brokerName")

    suspend fun listBrokers(): ListBrokersResponse {
        return client.myGet("$api/brokers")
    }

    suspend fun listAgents(brokerName: String): ListAgentsResponse =
        client.myGet("$api/brokers/$brokerName/agents")

    suspend fun addGenericAgent(brokerName: String, createAgentRequest: CreateGenericAgentRequest): CreateGenericAgentResponse =
        client.myPost("$api/brokers/$brokerName/agents", createAgentRequest)

    suspend fun deleteAgent(brokerName: String, agentName: String, force: Boolean?): Response<Void> =
        client.myDelete("$api/brokers/$brokerName/agents/$agentName", "force", force)

    suspend fun getAgent(brokerName: String, agentName: String): AgentInfo =
        client.myGet("$api/brokers/$brokerName/agents/$agentName")

    suspend fun headAgent(brokerName: String, agentName: String): Boolean =
        client.myHead("$api/brokers/$brokerName/agents/$agentName")

    suspend fun indexAgent(
        brokerName: String,
        agentName: String,
        index: Boolean? = null,
        reIndex: Boolean? = null,
        overWriteIndex: Boolean? = null
    ): Response<Void> {
        val paramMap: Map<String, Any?> = mapOf(
            Pair("index", index),
            Pair("re-index", reIndex),
            Pair("overwrite-index", overWriteIndex),
        )
        return client.myPut("$api/brokers/$brokerName/agents/$agentName", paramMap = paramMap)
    }

    /**
     * Object
     */
    suspend fun listObjects(
        brokerName: String,
        perPage: Long = 100,
        page: Long = 0,
        includeInternalMetadata: Boolean? = null,
        internalMetadataKey: String? = null,
        internalMetadataValue: String? = null
    ): ListObjectMetadata {
        val paramMap: Map<String, Any?> = mapOf(
            Pair("per_page", perPage),
            Pair("page", page),
            Pair("includeInternalMetadata", includeInternalMetadata),
            Pair("internalMetadata", internalMetadataKey?.let { "$it,$internalMetadataValue" } )
        )
        return client.myGet("$api/brokers/$brokerName/objects", paramMap)
    }

    suspend fun getObject(
        brokerName: String,
        objectName: String,
        includeInternalMetadata: Boolean? = null
    ): ObjectDetails =
        client.myGet("$api/brokers/$brokerName/objects/${objectName.urlEncode()}", "includeInternalMetadata", includeInternalMetadata)

    suspend fun objectExists(brokerName: String, objectName: String): Boolean =
        client.myHead("$api/brokers/$brokerName/objects/${objectName.urlEncode()}")

    suspend fun deleteObject(brokerName: String, objName: String): Response<Void> =
        client.myDelete("$api/brokers/$brokerName/objects/${objName.urlEncode()}")

    suspend fun updateObject(brokerName: String, objName: String, metadata: Map<String, String>) {
        return client.myPut("$api/brokers/$brokerName/objects/${objName.urlEncode()}", MyMetadata(metadata))
    }
    // DWL TODO:  ?internalData=true

    /**
     * Job
     */
    suspend fun archiveFile(brokerName: String, archiveBody: ArchiveBody, uploadNewOnly: Boolean? = null): RioJob =
        client.myPost("$api/brokers/$brokerName/archive", archiveBody, "upload-new-files-only", uploadNewOnly)

    suspend fun retryArchiveJob(brokerName: String, retry: UUID): RioJob =
        client.myPost("$api/brokers/$brokerName/archive?retry=$retry")

    suspend fun restoreFile(brokerName: String, restoreBody: RestoreBody): RioJob =
        client.myPost("$api/brokers/$brokerName/restore", restoreBody)

    suspend fun jobStatus(jobId: UUID): DetailedRioJob =
        client.myGet("$api/jobs/$jobId")

    suspend fun retryRestoreJob(brokerName: String, retry: UUID): RioJob {
        return client.myPost("$api/brokers/$brokerName/restore?retry=$retry")
    }

    suspend fun getJobStatus(jobId: UUID, withFileStatus: Boolean? = null): DetailedRioJob {
        return client.myGet("$api/jobs/$jobId", "withFileStatus", withFileStatus)
    }

    suspend fun listJobs(
        job_type: String? = null,
        jobStatus: String? = "active",
        broker: String? = null,
        creation_date_start: String? = null,
        creation_date_end: String? = null,
        jobName: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        page: Int? = null,
        per_page: Int? = null
    ): JobList {
        val paramMap = mapOf<String,Any?>(
            Pair("job_type", job_type),
            Pair("status", jobStatus),
            Pair("broker", broker),
            Pair("creation_date_start", creation_date_start),
            Pair("creation_date_end", creation_date_end),
            Pair("job_name", jobName),
            Pair("sort_by", sortBy),
            Pair("sort_order", sortOrder),
            Pair("page", page),
            Pair("per_page", per_page)
        )
        return client.myGet("$api/jobs", paramMap)
    }

    suspend fun deleteAllJobs(): Response<Unit> =
        client.myDelete("$api/jobs")

    suspend fun cancelJob(jobId: UUID): Response<Void> =
        client.myPut("$api/jobs/$jobId")

    suspend fun headJob(jobId: String): Boolean =
        client.myHead("$api/jobs/$jobId")

    suspend fun updateJob(jobId: String, cancel: Boolean): DetailedRioJob =
        client.myPut("$api/jobs/$jobId?cancel==$cancel")

    suspend fun fileStatus(jobId: UUID): FileStatusLogResponse =
        client.myGet("$api/jobs/$jobId/filestatus")

    suspend fun fileStatus(jobId: UUID, objectName: String): FileStatusLogResponse =
        client.myGet("$api/jobs/$jobId/filestatus/${objectName.urlEncode()}")


    /**
     * Log
     */
    suspend fun newLog(): LogsetResponse =
        client.myPost("$api/logs")

    // TODO: suspend fun downloadLogset(@Path("logsetId") logsetId: String)

    suspend fun listLogs(
        @Query("per_page") perPage: Int? = null,
        @Query("page") page: Int? = null
    ): LogsetListResponse {
        val paramMap = mapOf<String,Any?>(
            Pair("per_page", perPage),
            Pair("page", page)
        )
        return client.myGet("$api/logs", paramMap)
    }

    suspend fun getLogset(@Path("logsetId") logsetId: String): LogsetResponse =
        client.myGet("$api/logs/$logsetId")

    suspend fun deleteLogset(@Path("logsetId") logsetId: UUID): Response<Void> =
        client.myDelete("$api/logs/$logsetId")

    suspend fun headLogset(@Path("logsetId") logsetId: UUID): Boolean =
        client.myHead("$api/logs/$logsetId")

    /**
     * System
     */
    suspend fun systemInfo(): SystemResponse =
        client.myGet("$api/system")

    /*suspend fun metadataValues(brokerName: String, metadataKey: String, page: Long = 0, perPage: Long = 100, internal: Boolean): ListMetadataValuesDistinct {
        return client.get("$api/brokers/$brokerName/metadata/$metadataKey?page=$page&per_page=$perPage&internalData=$internal") {
            header("Authorization", "Bearer ${tokenContainer.token}")
        }
    }*/

    override fun close() {
        /*runBlocking {
            deleteApiToken()
        }*/
        client.close()
    }

    private fun Map<String, Any?>?.queryString(): String =
        this?.let {
            "?" + it.map { (k, v) ->
                v?.let { "$k=${v.toString().urlEncode()}" } ?: ""
            }.joinToString("&")
        } ?: ""

    private fun paramMap(key: String, value: Any? = null): Map<String,Any?>? =
        value?.let { mapOf<String,Any?>(Pair(key, value)) }

    private suspend inline fun <reified T> HttpClient.myDelete(url: String, key: String, value: Any? = null): T {
        return myDelete(url, paramMap(key, value))
    }

    private suspend inline fun <reified T> HttpClient.myDelete(url: String, paramMap: Map<String,Any?>? = null): T {
        return delete("$url${paramMap.queryString()}") {
            header("Authorization", "Bearer ${tokenContainer.token}")
        }
    }

    private suspend inline fun <reified T> HttpClient.myGet(url: String, key: String, value: Any? = null): T {
        return myGet(url, paramMap(key, value))
    }

    private suspend inline fun <reified T> HttpClient.myGet(url: String, paramMap: Map<String,Any?>? = null): T {
        return get("$url${paramMap.queryString()}") {
            header("Authorization", "Bearer ${tokenContainer.token}")
        }
    }

    private suspend inline fun HttpClient.myHead(url: String): Boolean {
        return try {
            val response: HttpResponse = head(url) {
                header("Authorization", "Bearer ${tokenContainer.token}")
            }
            true
        } catch (t: ClientRequestException) {
            t.response.status.value == 200
        }
    }

    private suspend inline fun <reified T> HttpClient.myPost(url: String, request: RioRequest = myEmptyRequest, key: String, value: Any? = null): T {
        return myPost(url, request, paramMap(key, value))
    }

    private suspend inline fun <reified T> HttpClient.myPost(url: String, request: RioRequest = myEmptyRequest, paramMap: Map<String,Any?>? = null): T {
        return post("$url${paramMap.queryString()}") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${tokenContainer.token}")
            body = request
        }
    }

    private suspend inline fun <reified T> HttpClient.myPut(url: String, request: RioRequest = myEmptyRequest, paramMap: Map<String,Any?>? = null ): T {
        return post(url) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${tokenContainer.token}")
            body = request
        }
    }

}
