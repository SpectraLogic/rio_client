/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient


import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import nl.altindag.ssl.util.TrustManagerUtils
import java.io.Closeable
import java.net.URL
import java.nio.file.Path
import java.util.UUID

class RioClient(
    rioUrl: URL,
    username: String = "spectra",
    password: String = "spectra",
    private val requestTimeout: Long = 60L * 1000L, // 60 seconds
    private val verbose: Boolean = false
) : Closeable {

    companion object {
        private val logger = mu.KotlinLogging.logger {}
    }

    private data class MyMetadata(val metadata: Map<String, String>) : RioRequest
    private val api by lazy { "$rioUrl/api" }
    private val tokenClient: TokenClient = TokenClient(rioUrl, username, password)
    private val jsonContentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)

    private val client = HttpClient(CIO) {
        engine {
            https {
                this.trustManager = TrustManagerUtils.createUnsafeTrustManager()
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeout
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    // TODO ?? isLenient = true
                }
            )
        }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(tokenClient.getShortToken(), "")
                }
                refreshTokens {
                    BearerTokens(tokenClient.getShortToken(), "")
                }
            }
        }
        install(Logging) {
            if (verbose) {
                level = LogLevel.ALL
            } else {
                level = LogLevel.NONE
            }
        }
    }

    /**
     * Keys
     */
    suspend fun createApiToken(tokenCreateRequest: TokenCreateRequest): TokenResponse =
        client.myPost("$api/keys", tokenCreateRequest)

    suspend fun deleteApiToken(id: UUID): EmptyResponse =
        client.myDelete("$api/keys/$id")

    suspend fun getApiToken(id: UUID): TokenKeyResponse =
        client.myGet("$api/keys/$id")

    suspend fun headApiToken(id: UUID): Boolean =
        client.myHead("$api/keys/$id")

    suspend fun listTokenKeys(page: Long? = null, perPage: Long? = null): TokenListResponse =
        client.myGet("$api/keys", paramMap = pageParamMap(page, perPage))

    /**
     * Cluster
     */
    suspend fun createCluster(name: String): ClusterResponse =
        client.myPost("$api/cluster", paramMap = paramMap("name", name))

    suspend fun joinCluster(url: String): ClusterResponse =
        client.myPost("$api/cluster", paramMap = paramMap("cluster_url", url))

    suspend fun getCluster(): ClusterResponse =
        client.myPost("$api/cluster")

    suspend fun deleteCluster(): EmptyResponse =
        client.myPost("$api/cluster")

    suspend fun listClusterMembers(): ClusterMembersListResponse =
        client.myGet("$api/cluster/members/")

    /**
     * Device
     */
    suspend fun ensureSpectraDeviceExists(spectraDeviceCreateRequest: SpectraDeviceCreateRequest) {
        if (!headSpectraDevice(spectraDeviceCreateRequest.name)) {
            createSpectraDevice(spectraDeviceCreateRequest)
        }
    }

    suspend fun deleteDevice(type: String, name: String): EmptyResponse =
        client.myDelete("$api/devices/$type/$name")

    suspend fun headDevice(type: String, name: String) =
        client.myHead("$api/devices/$type/$name")

    // Spectra
    suspend fun createSpectraDevice(spectraDeviceCreateRequest: SpectraDeviceCreateRequest): SpectraDeviceResponse =
        client.myPost("$api/devices/spectra", spectraDeviceCreateRequest)

    suspend fun updateSpectraDevice(name: String, spectraDeviceUpdateRequest: SpectraDeviceUpdateRequest): SpectraDeviceResponse =
        client.myPut("$api/devices/spectra/$name", spectraDeviceUpdateRequest)

    suspend fun deleteSpectraDevice(name: String): EmptyResponse =
        client.myDelete("$api/devices/spectra/$name")

    suspend fun getSpectraDevice(name: String): SpectraDeviceResponse =
        client.myGet("$api/devices/spectra/$name")

    suspend fun headSpectraDevice(name: String): Boolean =
        client.myHead("$api/devices/spectra/$name")

    suspend fun listSpectraDevices(page: Long? = null, perPage: Long? = null): SpectraDeviceListResponse =
        client.myGet("$api/devices/spectra", paramMap = pageParamMap(page, perPage))

    // Diva
    suspend fun createDivaDevice(divaDeviceCreateRequest: DivaDeviceCreateRequest): DivaDeviceResponse =
        client.myPost("$api/devices/diva", divaDeviceCreateRequest)

    suspend fun updateDivaDevice(name: String, divaDeviceUpdateRequest: DivaDeviceUpdateRequest): DivaDeviceResponse =
        client.myPut("$api/devices/diva/$name", divaDeviceUpdateRequest)

    suspend fun deleteDivaDevice(name: String): EmptyResponse =
        client.myDelete("$api/devices/diva/$name")

    suspend fun getDivaDevice(name: String): DivaDeviceResponse =
        client.myGet("$api/devices/diva/$name")

    suspend fun headDivaDevice(name: String): Boolean =
        client.myHead("$api/devices/diva/$name")

    suspend fun listDivaDevices(page: Long? = null, perPage: Long? = null): DivaDeviceListResponse =
        client.myGet("$api/devices/diva", paramMap = pageParamMap(page, perPage))

    // Flashnet
    suspend fun createFlashnetDevice(flashnetDeviceCreateRequest: FlashnetDeviceCreateRequest): FlashnetDeviceResponse =
        client.myPost("$api/devices/flashnet", flashnetDeviceCreateRequest)

    suspend fun updateFlashnetDevice(name: String, flashnetDeviceUpdateRequest: FlashnetDeviceUpdateRequest): FlashnetDeviceResponse =
        client.myPut("$api/devices/flashnet/$name", flashnetDeviceUpdateRequest)

    suspend fun deleteFlashnetDevice(name: String): EmptyResponse =
        client.myDelete("$api/devices/flashnet/$name")

    suspend fun getFlashnetDevice(name: String): FlashnetDeviceResponse =
        client.myGet("$api/devices/flashnet/$name")

    suspend fun headFlashnetDevice(name: String): Boolean =
        client.myHead("$api/devices/flashnet/$name")

    suspend fun listFlashnetDevices(page: Long? = null, perPage: Long? = null): FlashnetDeviceListResponse =
        client.myGet("$api/devices/flashnet", paramMap = pageParamMap(page, perPage))

    // TBPFR
    suspend fun createTbpfrDevice(tbpfrDeviceCreateRequest: TbpfrDeviceCreateRequest): TbpfrDeviceResponse =
        client.myPost("$api/devices/tbpfr", tbpfrDeviceCreateRequest)

    suspend fun updateTbpfrDevice(name: String, tbpfrDeviceUpdateRequest: TbpfrDeviceUpdateRequest): TbpfrDeviceResponse =
        client.myPut("$api/devices/tbpfr/$name", tbpfrDeviceUpdateRequest)

    suspend fun deleteTbpfrDevice(name: String): EmptyResponse =
        client.myDelete("$api/devices/tbpfr/$name")

    suspend fun getTbpfrDevice(name: String): TbpfrDeviceResponse =
        client.myGet("$api/devices/tbpfr/$name")

    suspend fun headTbpfrDevice(name: String): Boolean =
        client.myHead("$api/devices/tbpfr/$name")

    suspend fun listTbpfrDevices(page: Long? = null, perPage: Long? = null): TbpfrDeviceListResponse =
        client.myGet("$api/devices/tbpfr", paramMap = pageParamMap(page, perPage))

    // Vail
    suspend fun createVailDevice(vailDeviceCreateRequest: VailDeviceCreateRequest): VailDeviceResponse =
        client.myPost("$api/devices/vail", vailDeviceCreateRequest)

    suspend fun updateVailDevice(name: String, vailDeviceUpdateRequest: VailDeviceUpdateRequest): VailDeviceResponse =
        client.myPut("$api/devices/vail/$name", vailDeviceUpdateRequest)

    suspend fun deleteVailDevice(name: String): EmptyResponse =
        client.myDelete("$api/devices/vail/$name")

    suspend fun getVailDevice(name: String): VailDeviceResponse =
        client.myGet("$api/devices/vail/$name")

    suspend fun headVailDevice(name: String): Boolean =
        client.myHead("$api/devices/vail/$name")

    suspend fun listVailDevices(page: Long? = null, perPage: Long? = null): VailDeviceListResponse =
        client.myGet("$api/devices/vail", paramMap = pageParamMap(page, perPage))

    /**
     * Endpoint
     */
    suspend fun deleteEndpointDevice(name: String): EmptyResponse =
        client.myDelete("$api/devices/endpoint/$name")

    suspend fun getEndpointDevice(name: String): EndpointGenericDeviceResponse =
        client.myGet("$api/devices/endpoint/$name")

    suspend fun headEndpointDevice(name: String): Boolean =
        client.myHead("$api/devices/endpoint/$name")

    suspend fun listEndpointDevices(page: Long? = null, perPage: Long? = null): EndpointDeviceListResponse =
        client.myGet("$api/devices/endpoint", paramMap = pageParamMap(page, perPage))

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

    suspend fun listEndpointDevice(
        name: String,
        type: String,
        recursive: Boolean = false,
        subPath: String? = null,
        startAfter: String? = null,
        maxResults: Int? = null
    ): DeviceObjectListResponse {
        val paramMap: Map<String, Any?> = mapOf(
            Pair("recursive", recursive),
            Pair("path", subPath),
            Pair("startAfter", startAfter),
            Pair("maxResults", maxResults)
        )
        return client.myGet("$api/devices/$type/$name/list", paramMap = paramMap)
    }

    /**
     * Broker
     */
    suspend fun ensureBrokerExists(brokerCreateRequest: BrokerCreateRequest) {
        if (!headBroker(brokerCreateRequest.name)) {
            createBroker(brokerCreateRequest)
        }
    }

    suspend fun createBroker(brokerCreateRequest: BrokerCreateRequest): BrokerResponse =
        client.myPost("$api/brokers", brokerCreateRequest)

    suspend fun deleteBroker(brokerName: String, force: Boolean): EmptyResponse =
        client.myDelete("$api/brokers/$brokerName?force=$force")

    suspend fun getBroker(brokerName: String): BrokerResponse =
        client.myGet("$api/brokers/$brokerName")

    suspend fun headBroker(brokerName: String): Boolean =
        client.myHead("$api/brokers/$brokerName")

    suspend fun listBrokers(page: Long? = null, perPage: Long? = null): BrokerListResponse {
        return client.myGet("$api/brokers", paramMap = pageParamMap(page, perPage))
    }

    suspend fun listAgents(brokerName: String, page: Long? = null, perPage: Long? = null): AgentListResponse =
        client.myGet("$api/brokers/$brokerName/agents", paramMap = pageParamMap(page, perPage))

    suspend fun createAgent(brokerName: String, agentCreateRequest: AgentCreateRequest): AgentResponse =
        client.myPost("$api/brokers/$brokerName/agents", agentCreateRequest)

    suspend fun updateAgent(brokerName: String, agentName: String, agentUpdateRequest: AgentUpdateRequest): AgentResponse =
        client.myPut("$api/brokers/$brokerName/agents/$agentName", agentUpdateRequest)

    suspend fun deleteAgent(brokerName: String, agentName: String, force: Boolean?): EmptyResponse =
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
    ): EmptyResponse {
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
        internalMetadataValue: String? = null,
        migration: Boolean? = null,
        paginationSetId: UUID? = null
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
                Pair("migration", migration),
                Pair("pagination_set_id", paginationSetId)
            )
        ).let {
            if (!internalMetadataKey.isNullOrBlank() && !internalMetadataValue.isNullOrBlank()) {
                it.plus(Pair("internalMetadata", "$internalMetadataKey,$internalMetadataValue"))
            } else {
                it
            }
        }
        return client.myGet("$api/brokers/$brokerName/objects", paramMap = paramMap)
    }

    suspend fun objectCount(
        brokerName: String,
        dateStart: String? = null,
        dateEnd: String? = null,
        prefix: String? = null,
        filename: String? = null,
        internalMetadataKey: String? = null,
        internalMetadataValue: String? = null
    ): ObjectCountResponse {
        val paramMap: Map<String, Any?> = mapOf(
                Pair("creation_date_start", dateStart),
                Pair("creation_date_end", dateEnd),
                Pair("prefix", prefix),
                Pair("filename", filename)
        ).let {
            if (!internalMetadataKey.isNullOrBlank() && !internalMetadataValue.isNullOrBlank()) {
                it.plus(Pair("internalMetadata", "$internalMetadataKey,$internalMetadataValue"))
            } else {
                it
            }
        }
        return client.myGet("$api/brokers/$brokerName/objectcount", paramMap = paramMap)
    }

    suspend fun getObject(
        brokerName: String,
        objectName: String,
        includeInternalMetadata: Boolean? = null
    ): ObjectResponse =
        client.myGet("$api/brokers/$brokerName/objects/${objectName.urlEncode()}", "includeInternalMetadata", includeInternalMetadata)

    suspend fun objectExists(brokerName: String, objectName: String): Boolean =
        client.myHead("$api/brokers/$brokerName/objects/${objectName.urlEncode()}")

    suspend fun objectBatchHead(brokerName: String, objectBatchHeadRequest: ObjectBatchHeadRequest): ObjectBatchHeadResponse =
        client.myGet("$api/brokers/$brokerName/objectbatch", request = objectBatchHeadRequest)

    suspend fun deleteObject(brokerName: String, objName: String): EmptyResponse =
        client.myDelete("$api/brokers/$brokerName/objects/${objName.urlEncode()}")

    suspend fun updateObject(brokerName: String, objName: String, metadata: Map<String, String>, internalData: Boolean? = null, merge: Boolean = false): ObjectResponse {
        val paraMap: Map<String, Any?> = mapOf(
            "internalData" to internalData,
            "merge" to merge
        )
        return client.myPut(
            "$api/brokers/$brokerName/objects/${objName.urlEncode()}",
            MyMetadata(metadata),
            paraMap
        )
    }

    suspend fun updateObjects(brokerName: String, updateRequest: ObjectBatchUpdateRequest, internalData: Boolean? = null, merge: Boolean = false): EmptyResponse {
        val paraMap: Map<String, Any?> = mapOf(
            "internalData" to internalData,
            "merge" to merge
        )
        return client.myPut("$api/brokers/$brokerName/objects", updateRequest, paraMap)
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

    suspend fun createRestoreJob(
        brokerName: String,
        restoreRequest: RestoreRequest,
        jobPriority: String? = null,
        sessionId: String? = null,
        failFast: Boolean? = null
    ): JobResponse {
        val paramMap = mapOf(
            Pair("priority", jobPriority),
            Pair("sessionId", sessionId),
            Pair("fail-fast", failFast)
        )
        return client.myPost("$api/brokers/$brokerName/restore", restoreRequest, paramMap)
    }

    suspend fun retryRestoreJob(brokerName: String, retry: UUID): JobResponse {
        return client.myPost("$api/brokers/$brokerName/restore?retry=$retry")
    }

    suspend fun jobStatus(jobId: UUID, withFileStatus: Boolean? = null): DetailedJobResponse =
        client.myGet("$api/jobs/$jobId", "withFileStatus", withFileStatus)

    suspend fun listJobs(
        job_type: String? = null,
        jobStatus: String? = null,
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
        return client.myGet("$api/jobs", paramMap = paramMap)
    }

    suspend fun deleteJob(jobId: UUID): EmptyResponse =
        client.myDelete("$api/jobs/$jobId")

    suspend fun deleteAllJobs(): EmptyResponse =
        client.myDelete("$api/jobs")

    suspend fun cancelJob(jobId: UUID): EmptyResponse =
        client.myPut("$api/jobs/$jobId?cancel=true")

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

    suspend fun deleteArchivedFiles(): EmptyResponse =
        client.myPost("$api/deletearchivedfiles")

    /**
     * Log
     */
    suspend fun createLogset(): LogsetResponse =
        client.myPost("$api/logs")

    suspend fun deleteLogset(logsetId: UUID): EmptyResponse =
        client.myDelete("$api/logs/$logsetId")

    suspend fun downloadLogset(logsetId: UUID): Path =
        client.myGet("$api/logs/$logsetId/download")

    suspend fun getLogset(logsetId: String): LogsetResponse =
        client.myGet("$api/logs/$logsetId")

    suspend fun headLogset(logsetId: UUID): Boolean =
        client.myHead("$api/logs/$logsetId")

    suspend fun listLogsets(page: Long? = null, perPage: Long? = null): LogsetListResponse =
        client.myGet("$api/logs", paramMap = pageParamMap(page, perPage))

    /**
     * Messages
     */
    suspend fun getMessage(messageId: UUID): MessageResponse =
        client.myGet("$api/messages/$messageId")

    suspend fun listMessages(page: Long? = null, perPage: Long? = null): MessageListResponse =
        client.myGet("$api/messages", paramMap = pageParamMap(page, perPage))

    suspend fun updateMessage(messageId: UUID, read: Boolean) =
        client.myPatch("$api/messages/$messageId", MessageUpdateRequest(read))

    suspend fun updateAllMessage(read: Boolean) =
        client.myPatch("$api/messages", MessageUpdateRequest(read))

    /**
     * System
     */
    suspend fun systemInfo(): SystemResponse =
        client.myGet("$api/system")

    suspend fun clientDataInsert(clientDataRequest: ClientDataRequest): ClientDataResponse =
        client.myPost("$api/system/clientData", clientDataRequest)
    suspend fun clientDataUpdate(dataId: UUID, clientDataRequest: ClientDataRequest): ClientDataResponse =
        client.myPut("$api/system/clientData/$dataId", clientDataRequest)
    suspend fun clientDataDelete(dataId: UUID): EmptyResponse =
        client.myDelete("$api/system/clientData/${dataId.toString()}")
    suspend fun clientDataList(
        clientDataId: String? = null,
        clientName: String? = null,
        tag: String? = null,
        page: Long? = null,
        perPage: Long? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
    ): ClientDataListResponse {
        val paramMap = pageParamMap(page, perPage)
            .plus(
                arrayOf(
                    Pair("clientDataId", clientDataId),
                    Pair("clientName", clientName),
                    Pair("tag", tag),
                    Pair("sort_by", sortBy),
                    Pair("sort_order", sortOrder)
                )
            )
        return client.myGet("$api/system/clientData", paramMap = paramMap)
    }
    suspend fun clientDataGet(dataId: UUID): ClientDataResponse =
        client.myGet("$api/system/clientData/$dataId")

    override fun close() {
        client.close()
    }

    /**
     * Private worker methods
     */

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

    private suspend inline fun <reified T : RioResponse> HttpClient.myDelete(url: String, key: String, value: Any? = null): T {
        return myDelete(url, paramMap(key, value))
    }

    private suspend inline fun <reified T : RioResponse> HttpClient.myDelete(url: String, paramMap: Map<String, Any?>? = null): T {
        val urlStr = "$url${paramMap.queryString()}"
        val response: HttpResponse = try {
            delete(urlStr) {
            }
        } catch (t: Throwable) {
            throw RioHttpException(HttpMethod.Post, urlStr, t)
        }
        val result: T = if ((response.contentLength() ?: 0L) == 0L) {
            EmptyResponse() as T
        } else {
            response.body()
        }
        result.statusCode = response.status
        return result
    }

    private suspend inline fun <reified T : RioResponse> HttpClient.myGet(url: String, key: String, value: Any? = null): T {
        return myGet(url, paramMap = paramMap(key, value))
    }

    private suspend inline fun <reified T : RioResponse> HttpClient.myGet(url: String, request: RioRequest? = null, paramMap: Map<String, Any?>? = null): T {
        val urlStr = "$url${paramMap.queryString()}"
        val requestBody: Any = request ?: EmptyContent
        val response: HttpResponse = try {
            get(urlStr) {
                contentType(jsonContentType)
                setBody(requestBody)
            }
        } catch (t: Throwable) {
            throw RioHttpException(HttpMethod.Get, urlStr, t)
        }
        val result: T = response.body()
        result.statusCode = response.status
        return result
    }

    private suspend inline fun HttpClient.myHead(url: String): Boolean {
        return try {
            head(url) {
            } as HttpResponse
            true
        } catch (t: ClientRequestException) {
            when (t.response.status.value) {
                HttpStatusCode.OK.value -> true
                HttpStatusCode.NoContent.value -> true
                HttpStatusCode.NotFound.value -> false
                else -> {
                    throw RioHttpException(HttpMethod.Head, url, t, t.response.status)
                }
            }
        } catch (t: Throwable) {
            throw RioHttpException(HttpMethod.Head, url, t)
        }
    }

    private suspend inline fun HttpClient.myPatch(url: String, request: RioRequest?): Boolean {
        val requestBody: Any = request ?: EmptyContent
        return try {
            patch(url) {
                contentType(jsonContentType)
                setBody(requestBody)
            } as HttpResponse
            true
        } catch (t: ClientRequestException) {
            when (t.response.status.value) {
                HttpStatusCode.OK.value -> true
                HttpStatusCode.NoContent.value -> true
                HttpStatusCode.NotFound.value -> false
                else -> throw RioHttpException(HttpMethod.Patch, url, t)
            }
        } catch (t: Throwable) {
            throw RioHttpException(HttpMethod.Patch, url, t)
        }
    }

    private suspend inline fun <reified T : RioResponse> HttpClient.myPost(url: String, request: RioRequest? = null, key: String, value: Any? = null): T {
        return myPost(url, request, paramMap(key, value))
    }

    private suspend inline fun <reified T : RioResponse> HttpClient.myPost(url: String, request: RioRequest? = null, paramMap: Map<String, Any?>? = null): T {
        val urlStr = "$url${paramMap.queryString()}"
        val requestBody: Any = request ?: EmptyContent
        val response: HttpResponse = try {
            post(urlStr) {
                contentType(jsonContentType)
                setBody(requestBody)
            }
        } catch (t: Throwable) {
            throw RioHttpException(HttpMethod.Post, urlStr, t)
        }
        val result: T = if ((response.contentLength() ?: 0L) == 0L) {
            EmptyResponse() as T
        } else {
            response.body()
        }
        result.statusCode = response.status
        return result
    }

    private suspend inline fun <reified T : RioResponse> HttpClient.myPut(url: String, request: RioRequest? = null, paramMap: Map<String, Any?>? = null): T {
        val urlStr = "$url${paramMap.queryString()}"
        val requestBody: Any = request ?: EmptyContent
        val response: HttpResponse = try {
            put(urlStr) {
                contentType(jsonContentType)
                setBody(requestBody)
            }
        } catch (t: Throwable) {
            throw RioHttpException(HttpMethod.Put, urlStr, t)
        }
        val result: T = if ((response.contentLength() ?: 0L) == 0L) {
            EmptyResponse() as T
        } else {
            response.body()
        }
        result.statusCode = response.status
        return result
    }

    // TODO: why is this RioCruise specific method here?
    suspend fun metadataValues(brokerName: String, metadataKey: String, page: Long = 0, perPage: Long = 100, internal: Boolean): ListMetadataValuesDistinct {
        val urlStr = "$api/brokers/$brokerName/metadata/$metadataKey?page=$page&per_page=$perPage&internalData=$internal"
        return try {
            client.get(urlStr) { }.body()
        } catch (t: Throwable) {
            throw RioHttpException(HttpMethod.Get, urlStr, t)
        }
    }
}

// TODO: why is this RioCruise specific method here?
data class ListMetadataValuesDistinct(
    @SerialName("results")
    override val objectList: List<Map<String, String>>,
    val page: PageInfo
) : RioListResponse<Map<String, String>>(objectList, page)
