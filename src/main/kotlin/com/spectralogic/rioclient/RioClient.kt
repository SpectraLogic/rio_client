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
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import nl.altindag.ssl.util.TrustManagerUtils
import java.io.Closeable
import java.net.URL
import java.nio.file.Path
import java.util.UUID

interface RioRequest

class RioClient(rioUrl: URL, val username: String = "spectra", val password: String = "spectra") : Closeable {

    private data class EmptyRequest(val blank: String) : RioRequest
    private data class MyMetadata(val metadata: Map<String, String>) : RioRequest

    private val myEmptyRequest = EmptyRequest("")
    private val api by lazy { "$rioUrl/api" }

    private val tokenCreateContainer: ShortTokenResponse by lazy {
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
     * Token & Keys (do not use myPost, will cause infinite loop)
     */
    private suspend fun getShortToken(): ShortTokenResponse {
        return client.post("$api/tokens") {
            contentType(ContentType.Application.Json)
            body = UserLoginCredentials(username, password)
        }
    }

    suspend fun createApiToken(tokenCreateRequest: TokenCreateRequest): TokenResponse =
        client.myPost("$api/keys", tokenCreateRequest)

    suspend fun deleteApiToken(id: UUID): Boolean =
        client.myDelete("$api/keys/$id")

    suspend fun getApiToken(id: UUID): TokenKeyResponse =
        client.myGet("$api/keys/$id")

    suspend fun headApiToken(id: UUID): Boolean =
        client.myHead("$api/keys/$id")

    suspend fun listTokenKeys(page: Long? = null, perPage: Long? = null): TokenListResponse =
        client.myGet("$api/keys", pageParamMap(page, perPage))

    /**
     * Cluster
     */
    suspend fun deleteCluster(): Boolean =
        client.myPost("$api/cluster")

    suspend fun listClusterMembers(): ClusterMembersList =
        client.myGet("$api/cluster/members/")

    /**
     * Device
     */

    suspend fun ensureSpectraDeviceExists(spectraDeviceCreateRequest: SpectraDeviceCreateRequest) {
        if (!headSpectraDevice(spectraDeviceCreateRequest.name)) {
            createSpectraDevice(spectraDeviceCreateRequest)
        }
    }

    suspend fun deleteDevice(type: String, name: String) =
        client.myDelete("$api/devices/$type/$name")

    suspend fun headDevice(type: String, name: String) =
        client.myHead("$api/devices/$type/$name")

    // Spectra
    suspend fun createSpectraDevice(spectraDeviceCreateRequest: SpectraDeviceCreateRequest): SpectraDeviceResponse =
        client.myPost("$api/devices/spectra", spectraDeviceCreateRequest)

    suspend fun deleteSpectraDevice(name: String): Boolean =
        client.myDelete("$api/devices/spectra/$name")

    suspend fun getSpectraDevice(name: String): SpectraDeviceResponse =
        client.myGet("$api/devices/spectra/$name")

    suspend fun headSpectraDevice(name: String): Boolean =
        client.myHead("$api/devices/spectra/$name")

    suspend fun listSpectraDevices(page: Long? = null, perPage: Long? = null): SpectraDeviceListResponse =
        client.myGet("$api/devices/spectra", pageParamMap(page, perPage))

    // Diva
    suspend fun createDivaDevice(divaDeviceCreateRequest: DivaDeviceCreateRequest): DivaDeviceResponse =
        client.myPost("$api/devices/diva", divaDeviceCreateRequest)

    suspend fun deleteDivaDevice(name: String): Boolean =
        client.myDelete("$api/devices/diva/$name")

    suspend fun getDivaDevice(name: String): DivaDeviceResponse =
        client.myGet("$api/devices/diva/$name")

    suspend fun headDivaDevice(name: String): Boolean =
        client.myHead("$api/devices/diva/$name")

    suspend fun listDivaDevices(page: Long? = null, perPage: Long? = null): DivaDeviceListResponse =
        client.myGet("$api/devices/diva", pageParamMap(page, perPage))

    // Flashnet
    suspend fun createFlashnetDevice(flashnetDeviceCreateRequest: FlashnetDeviceCreateRequest): FlashnetDeviceResponse =
        client.myPost("$api/devices/flashnet", flashnetDeviceCreateRequest)

    suspend fun deleteFlashnetDevice(name: String): Boolean =
        client.myDelete("$api/devices/flashnet/$name")

    suspend fun getFlashnetDevice(name: String): FlashnetDeviceResponse =
        client.myGet("$api/devices/flashnet/$name")

    suspend fun headFlashnetDevice(name: String): Boolean =
        client.myHead("$api/devices/flashnet/$name")

    suspend fun listFlashnetDevices(page: Long? = null, perPage: Long? = null): FlashnetDeviceListResponse =
        client.myGet("$api/devices/flashnet", pageParamMap(page, perPage))

    // TBPFR
    suspend fun createTbpfrDevice(tbpfrDeviceCreateRequest: TbpfrDeviceCreateRequest): TbpfrDeviceResponse =
        client.myPost("$api/devices/tbpfr", tbpfrDeviceCreateRequest)

    suspend fun deleteTbpfrDevice(name: String): Boolean =
        client.myDelete("$api/devices/tbpfr/$name")

    suspend fun getTbpfrDevice(name: String): TbpfrDeviceResponse =
        client.myGet("$api/devices/tbpfr/$name")

    suspend fun headTbpfrDevice(name: String): Boolean =
        client.myHead("$api/devices/tbpfr/$name")

    suspend fun listTbpfrDevices(page: Long? = null, perPage: Long? = null): TbpfrDeviceListResponse =
        client.myGet("$api/devices/tbpfr", pageParamMap(page, perPage))

    // Vs3
    suspend fun createVs3Device(vs3DeviceCreateRequest: Vs3DeviceCreateRequest): Vs3DeviceResponse =
        client.myPost("$api/devices/vs3", vs3DeviceCreateRequest)

    suspend fun deleteVs3Device(name: String): Boolean =
        client.myDelete("$api/devices/vs3/$name")

    suspend fun getVs3Device(name: String): Vs3DeviceResponse =
        client.myGet("$api/devices/vs3/$name")

    suspend fun headVs3Device(name: String): Boolean =
        client.myHead("$api/devices/vs3/$name")

    suspend fun listVs3Devices(page: Long? = null, perPage: Long? = null): Vs3DeviceListResponse =
        client.myGet("$api/devices/vs3", pageParamMap(page, perPage))

    /**
     * Endpoint
     */
    suspend fun deleteEndpointDevice(name: String): Boolean =
        client.myDelete("$api/devices/endpoint/$name")

    suspend fun getEndpointDevice(name: String): EndpointGenericDeviceResponse =
        client.myGet("$api/devices/endpoint/$name")

    suspend fun headEndpointDevice(name: String): Boolean =
        client.myHead("$api/devices/endpoint/$name")

    suspend fun listEndpointDevices(page: Long? = null, perPage: Long? = null): EndpointDeviceListResponse =
        client.myGet("$api/devices/endpoint", pageParamMap(page, perPage))

    suspend fun createFtpEndpointDevice(ftpEndpointDeviceCreateRequest: FtpEndpointDeviceCreateRequest): EndpointFtpDeviceResponse =
        client.myPost("$api/devices/endpoint", ftpEndpointDeviceCreateRequest)

    suspend fun getFtpEndpointDevice(name: String): EndpointFtpDeviceResponse =
        client.myGet("$api/devices/endpoint/$name")

    suspend fun createS3EndpointDevice(s3EndpointDeviceCreateRequest: S3EndpointDeviceCreateRequest): EndpointS3DeviceResponse =
        client.myPost("$api/devices/endpoint", s3EndpointDeviceCreateRequest)

    suspend fun getS3EndpointDevice(name: String): EndpointS3DeviceResponse =
        client.myGet("$api/devices/endpoint/$name")

    suspend fun createUriEndpointDevice(uriEndpointDeviceCreateRequest: UriEndpointDeviceCreateRequest): EndpointUriDeviceResponse =
        client.myPost("$api/devices/endpoint", uriEndpointDeviceCreateRequest)

    suspend fun getUriEndpointDevice(name: String): EndpointUriDeviceResponse =
        client.myGet("$api/devices/endpoint/$name")

    /**
     * Broker
     */
    suspend fun createBroker(brokerCreateRequest: BrokerCreateRequest): BrokerResponse =
        client.myPost("$api/brokers", brokerCreateRequest)

    suspend fun deleteBroker(brokerName: String, force: Boolean): Boolean =
        client.myDelete("$api/brokers/$brokerName?force=$force")

    suspend fun getBroker(brokerName: String): BrokerResponse =
        client.myGet("$api/brokers/$brokerName")

    suspend fun headBroker(brokerName: String): Boolean =
        client.myHead("$api/brokers/$brokerName")

    suspend fun listBrokers(page: Long? = null, perPage: Long? = null): BrokerListResponse {
        return client.myGet("$api/brokers", pageParamMap(page, perPage))
    }

    suspend fun listAgents(brokerName: String, page: Long? = null, perPage: Long? = null): AgentListResponse =
        client.myGet("$api/brokers/$brokerName/agents", pageParamMap(page, perPage))

    suspend fun createAgent(brokerName: String, agentCreateRequest: AgentCreateRequest): AgentResponse =
        client.myPost("$api/brokers/$brokerName/agents", agentCreateRequest)

    suspend fun deleteAgent(brokerName: String, agentName: String, force: Boolean?): Boolean =
        client.myDelete("$api/brokers/$brokerName/agents/$agentName", "force", force)

    suspend fun getAgent(brokerName: String, agentName: String, includeIndexState: Boolean? = null): AgentResponse =
        client.myGet("$api/brokers/$brokerName/agents/$agentName", "includeIndexState", includeIndexState)

    suspend fun headAgent(brokerName: String, agentName: String): Boolean =
        client.myHead("$api/brokers/$brokerName/agents/$agentName")

    suspend fun indexAgent(
        brokerName: String,
        agentName: String,
        index: Boolean? = null,
        reIndex: Boolean? = null,
        overWriteIndex: Boolean? = null
    ): Boolean {
        val paramMap: Map<String, Any?> = mapOf(
            Pair("index", index),
            Pair("re-index", reIndex),
            Pair("overwrite-index", overWriteIndex),
        )
        return client.myPutBoolean("$api/brokers/$brokerName/agents/$agentName", paramMap = paramMap)
    }

    /**
     * Object
     */
    suspend fun listObjects(
        brokerName: String,
        page: Long? = null,
        perPage: Long? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        dateStart: String? = null,
        dateEnd: String? = null,
        prefix: String? = null,
        filename: String? = null,
        includeInternalMetadata: Boolean? = null,
        internalMetadataKey: String? = null,
        internalMetadataValue: String? = null
    ): ObjectListResponse {
        val paramMap = pageParamMap(page, perPage).plus(
            arrayOf(
                Pair("sort_by", sortBy),
                Pair("sort_order", sortOrder),
                Pair("creation_date_start", dateStart),
                Pair("creation_date_end", dateEnd),
                Pair("prefix", prefix),
                Pair("filename", filename),
                Pair("includeInternalMetadata", includeInternalMetadata),
                Pair("internalMetadata", internalMetadataKey?.let { "$it,$internalMetadataValue" })
            )
        )
        return client.myGet("$api/brokers/$brokerName/objects", paramMap)
    }

    suspend fun getObject(
        brokerName: String,
        objectName: String,
        includeInternalMetadata: Boolean? = null
    ): ObjectResponse =
        client.myGet("$api/brokers/$brokerName/objects/${objectName.urlEncode()}", "includeInternalMetadata", includeInternalMetadata)

    suspend fun objectExists(brokerName: String, objectName: String): Boolean =
        client.myHead("$api/brokers/$brokerName/objects/${objectName.urlEncode()}")

    suspend fun deleteObject(brokerName: String, objName: String): Boolean =
        client.myDelete("$api/brokers/$brokerName/objects/${objName.urlEncode()}")

    suspend fun updateObject(brokerName: String, objName: String, metadata: Map<String, String>, internalData: Boolean? = null, merge: Boolean = false): ObjectResponse {
        val paraMap: Map<String, Any?> = mapOf(
            "internalData" to internalData,
            "merge" to merge
        )
        return client.myPut(
            "$api/brokers/$brokerName/objects/${objName.urlEncode()}?merge=$merge",
            MyMetadata(metadata),
            paraMap
        )
    }

    /**
     * Job
     */
    suspend fun createArchiveJob(brokerName: String, archiveRequest: ArchiveRequest, uploadNewOnly: Boolean? = null, jobPriority: String? = null): JobResponse {
        val paramMap = mapOf(
            Pair("upload-new-files-only", uploadNewOnly),
            Pair("priority", jobPriority)
        )
        return client.myPost("$api/brokers/$brokerName/archive", archiveRequest, paramMap)
    }

    suspend fun retryArchiveJob(brokerName: String, retry: UUID): JobResponse =
        client.myPost("$api/brokers/$brokerName/archive?retry=$retry")

    suspend fun createRestoreJob(brokerName: String, restoreRequest: RestoreRequest, jobPriority: String? = null): JobResponse =
        client.myPost("$api/brokers/$brokerName/restore", restoreRequest, "priority", jobPriority)

    suspend fun retryRestoreJob(brokerName: String, retry: UUID): JobResponse {
        return client.myPost("$api/brokers/$brokerName/restore?retry=$retry")
    }

    suspend fun jobStatus(jobId: UUID, withFileStatus: Boolean? = null): DetailedJobResponse =
        client.myGet("$api/jobs/$jobId", "withFileStatus", withFileStatus)

    suspend fun listJobs(
        job_type: String? = null,
        jobStatus: String? = "active",
        broker: String? = null,
        creation_date_start: String? = null,
        creation_date_end: String? = null,
        jobName: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        page: Long? = null,
        perPage: Long? = null
    ): JobListResponse {
        val paramMap = pageParamMap(page, perPage)
            .plus(
                arrayOf(
                    Pair("job_type", job_type),
                    Pair("status", jobStatus),
                    Pair("broker", broker),
                    Pair("creation_date_start", creation_date_start),
                    Pair("creation_date_end", creation_date_end),
                    Pair("job_name", jobName),
                    Pair("sort_by", sortBy),
                    Pair("sort_order", sortOrder)
                )
            )
        return client.myGet("$api/jobs", paramMap)
    }

    suspend fun deleteJob(jobId: UUID): Boolean =
        client.myDelete("$api/jobs/$jobId")

    suspend fun deleteAllJobs(): Boolean =
        client.myDelete("$api/jobs")

    suspend fun cancelJob(jobId: UUID): Boolean =
        client.myPutBoolean("$api/jobs/$jobId")

    suspend fun headJob(jobId: String): Boolean =
        client.myHead("$api/jobs/$jobId")

    suspend fun updateJob(jobId: String, cancel: Boolean?, jobPriority: String? = null): JobResponse {
        val paramMap = mapOf(
            Pair("cancel", cancel),
            Pair("priority", jobPriority)
        )
        return client.myPut("$api/jobs/$jobId", paramMap = paramMap)
    }

    suspend fun fileStatus(jobId: UUID): FileStatusLogResponse =
        client.myGet("$api/jobs/$jobId/filestatus")

    suspend fun fileStatus(jobId: UUID, objectName: String): FileStatusLogResponse =
        client.myGet("$api/jobs/$jobId/filestatus/${objectName.urlEncode()}")

    /**
     * Log
     */
    suspend fun createLogset(): LogsetResponse =
        client.myPost("$api/logs")

    suspend fun deleteLogset(logsetId: UUID): Boolean =
        client.myDelete("$api/logs/$logsetId")

    suspend fun downloadLogset(logsetId: UUID): Path =
        client.myGet("$api/logs/$logsetId/download")

    suspend fun getLogset(logsetId: String): LogsetResponse =
        client.myGet("$api/logs/$logsetId")

    suspend fun headLogset(logsetId: UUID): Boolean =
        client.myHead("$api/logs/$logsetId")

    suspend fun listLogsets(page: Long? = null, perPage: Long? = null): LogsetListResponse =
        client.myGet("$api/logs", pageParamMap(page, perPage))

    /**
     * Messages
     */
    suspend fun getMessage(messageId: UUID): MessageResponse =
        client.myGet("$api/messages/$messageId")

    suspend fun listMessages(page: Long?, perPage: Long?): MessageListResponse =
        client.myGet("$api/messages", pageParamMap(page, perPage))

    suspend fun updateMessage(messageId: UUID, read: Boolean) =
        client.myPatch("$api/messages/$messageId", MessageUpdateRequest(read))

    suspend fun updateAllMessage(read: Boolean) =
        client.myPatch("$api/messages", MessageUpdateRequest(read))

    /**
     * System
     */
    suspend fun systemInfo(): SystemResponse =
        client.myGet("$api/system")

    override fun close() {
        client.close()
    }

    private fun Map<String, Any?>?.queryString(): String =
        this?.let {
            "?" + it.map { (k, v) ->
                v?.let { "$k=${v.toString().urlEncode()}" } ?: ""
            }.joinToString("&")
        } ?: ""

    private fun paramMap(key: String, value: Any? = null): Map<String, Any?>? =
        value?.let { mapOf<String, Any?>(Pair(key, value)) }

    private fun pageParamMap(page: Long? = null, perPage: Long? = null): Map<String, Any?> =
        mapOf(Pair("page", page), Pair("per_page", perPage))

    private suspend inline fun HttpClient.myDelete(url: String, key: String, value: Any? = null): Boolean {
        return myDelete(url, paramMap(key, value))
    }

    private suspend inline fun HttpClient.myDelete(url: String, paramMap: Map<String, Any?>? = null): Boolean {
        return try {
            val response: HttpResponse = delete("$url${paramMap.queryString()}") {
                header("Authorization", "Bearer ${tokenCreateContainer.token}")
            }
            true
        } catch (t: ClientRequestException) {
            t.response.status.value == HttpStatusCode.OK.value
        }
    }

    private suspend inline fun <reified T> HttpClient.myGet(url: String, key: String, value: Any? = null): T {
        return myGet(url, paramMap(key, value))
    }

    private suspend inline fun <reified T> HttpClient.myGet(url: String, paramMap: Map<String, Any?>? = null): T {
        return get("$url${paramMap.queryString()}") {
            header("Authorization", "Bearer ${tokenCreateContainer.token}")
        }
    }

    private suspend inline fun HttpClient.myHead(url: String): Boolean {
        return try {
            val response: HttpResponse = head(url) {
                header("Authorization", "Bearer ${tokenCreateContainer.token}")
            }
            true
        } catch (t: ClientRequestException) {
            t.response.status.value == HttpStatusCode.OK.value
        }
    }

    private suspend inline fun HttpClient.myPatch(url: String, request: RioRequest = myEmptyRequest): Boolean {
        return try {
            val response: HttpResponse = patch(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${tokenCreateContainer.token}")
                body = request
            }
            true
        } catch (t: ClientRequestException) {
            t.response.status.value == HttpStatusCode.OK.value || t.response.status.value == HttpStatusCode.NoContent.value
        }
    }

    private suspend inline fun <reified T> HttpClient.myPost(url: String, request: RioRequest = myEmptyRequest, key: String, value: Any? = null): T {
        return myPost(url, request, paramMap(key, value))
    }

    private suspend inline fun <reified T> HttpClient.myPost(url: String, request: RioRequest = myEmptyRequest, paramMap: Map<String, Any?>? = null): T {
        return post("$url${paramMap.queryString()}") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${tokenCreateContainer.token}")
            body = request
        }
    }

    private suspend inline fun <reified T> HttpClient.myPut(url: String, request: RioRequest = myEmptyRequest, key: String, value: Any? = null): T {
        return myPut(url, request, paramMap(key, value))
    }

    private suspend inline fun <reified T> HttpClient.myPut(url: String, request: RioRequest = myEmptyRequest, paramMap: Map<String, Any?>? = null): T {
        return put("$url${paramMap.queryString()}") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${tokenCreateContainer.token}")
            body = request
        }
    }

    private suspend inline fun HttpClient.myPutBoolean(url: String, request: RioRequest = myEmptyRequest, paramMap: Map<String, Any?>? = null): Boolean {
        return try {
            val response: HttpResponse = put("$url${paramMap.queryString()}") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${tokenCreateContainer.token}")
                body = request
            }
            true
        } catch (t: ClientRequestException) {
            t.response.status.value == HttpStatusCode.OK.value
        }
    }

    suspend fun metadataValues(brokerName: String, metadataKey: String, page: Long = 0, perPage: Long = 100, internal: Boolean): ListMetadataValuesDistinct {
        return client.get("$api/brokers/$brokerName/metadata/$metadataKey?page=$page&per_page=$perPage&internalData=$internal") {
            header("Authorization", "Bearer ${tokenCreateContainer.token}")
        }
    }
}
