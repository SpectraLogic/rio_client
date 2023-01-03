/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


interface RioRequest

@Serializable
open class RioResponse {
    @Serializable(with = HttpStatusCodeSerializer::class)
    var statusCode = HttpStatusCode.Processing
}

@Serializable
open class RioListResponse<T> (
    open val objectList: List<T>,
    open val pageInfo: PageInfo
) : RioResponse()

@Serializable
class EmptyResponse : RioResponse()

@Serializable
data class PageInfo(
    val number: Long,
    val pageSize: Long,
    val totalPages: Long,
    val totalItems: Long = 0L
)

class RioHttpException(
    val httpMethod: HttpMethod,
    val urlStr: String,
    override val cause: Throwable,
    val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
) : RuntimeException(cause.message, cause) {
    private val errorResponse: String =
        when (cause) {
            is ClientRequestException -> {
                cause.message.substringAfter("Text: \"").substringBeforeLast("\"")
            }
            is ServerResponseException -> {
                cause.message?.substringAfter("Text: \"")?.substringBeforeLast("\"")
            }
            else -> { null }
        } ?: "{\"message\":\"${cause.message} (${cause::class.java})\"}"

    fun httpMethod() = httpMethod
    fun statusCode() = statusCode
    fun url() = urlStr
    fun cause() = cause
    fun errorResponse() = errorResponse
    fun errorMessage(): RioErrorMessage {
        if (errorResponse.isNotBlank()) {
            listOf(
                RioResourceErrorMessageSerializer,
                RioValidationErrorMessageSerializer,
                RioUnsupportedMediaErrorSerializer,
                /*RioDownstreamErrorMessageSerializer,
                RioDefaultErrorMessageSerializer*/
            ).forEach {
                try {
                    return Json.decodeFromString(it, errorResponse)
                } catch (_: Throwable) { }
            }
        }
        return RioDefaultErrorMessage("${cause.message}", statusCode.value)
    }
}

interface RioErrorMessage {
    val message: String
    val statusCode: Int
}

@Serializable
data class RioDefaultErrorMessage
constructor (
    override val message: String,
    override val statusCode: Int
) : RioErrorMessage

@Serializable
data class RioResourceErrorMessage
constructor (
    override val message: String,
    override val statusCode: Int,
    val resourceName: String,
    val resourceType: String
) : RioErrorMessage

@Serializable
data class RioValidationErrorMessage
constructor (
    override val message: String,
    override val statusCode: Int,
    val errors: List<RioValidationMessage>
) : RioErrorMessage

@Serializable
data class RioValidationMessage
constructor (
    val fieldName: String,
    val fieldType: String,
    val errorType: String,
    val value: String? = null,
    val reason: String? = null
)

@Serializable
data class RioUnsupportedMediaErrorMessage
constructor (
    override val message: String,
    override val statusCode: Int,
    val suppliedMediaType: String,
    val supportedMediaType: String
) : RioErrorMessage

@Serializable
data class RioDownstreamErrorMessage
constructor (
    override val message: String,
    override val statusCode: Int,
    val resourceName: String?,
    val resourceType: String,
    val cause: String
) : RioErrorMessage
