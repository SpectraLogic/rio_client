/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.ServerResponseException
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

val mapper = ObjectMapper()

data class PageInfo(
    val number: Long,
    val pageSize: Long,
    val totalPages: Long,
    val totalItems: Long = 0L
)

interface PageData<T> {
    val objects: List<T>
    val page: PageInfo
}

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
                cause.message?.substringAfterLast("Text: \"")?.substringBeforeLast("\"")
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
                RioResourceErrorMessage::class.java,
                RioValidationErrorMessage::class.java,
                RioUnsupportedMediaError::class.java,
                RioDownstreamErrorMessage::class.java,
                RioResourceErrorMessage::class.java
            ).forEach {
                try {
                    return mapper.readValue(errorResponse, it)
                } catch (t: Throwable) {
                }
            }
        }
        return RioDefaultErrorMessage("${cause.message}", statusCode.value)
    }
}

interface RioErrorMessage

data class RioDefaultErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    val message: String,
    @JsonProperty("statusCode")
    val statusCode: Int
) : RioErrorMessage

data class RioResourceErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    val message: String,
    @JsonProperty("statusCode")
    val statusCode: Int,
    @JsonProperty("resourceName")
    val resourceName: String,
    @JsonProperty("resourceType")
    val resourceType: String
) : RioErrorMessage

data class RioValidationErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    val message: String,
    @JsonProperty("statusCode")
    val statusCode: Int,
    @JsonProperty("errors")
    val errors: List<RioValidationMessage>
) : RioErrorMessage

data class RioValidationMessage
@JsonCreator constructor (
    @JsonProperty("fieldName")
    val fieldName: String,
    @JsonProperty("fieldType")
    val fieldType: String,
    @JsonProperty("errorType")
    val errorType: String,
    @JsonProperty("value")
    val value: String? = null,
    @JsonProperty("reason")
    val reason: String? = null
)

data class RioUnsupportedMediaError
@JsonCreator constructor (
    @JsonProperty("message")
    val message: String,
    @JsonProperty("statusCode")
    val statusCode: Int,
    @JsonProperty("suppliedMediaType")
    val suppliedMediaType: String,
    @JsonProperty("supportedMediaType")
    val supportedMediaType: String
) : RioErrorMessage

data class RioDownstreamErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    val message: String,
    @JsonProperty("statusCode")
    val statusCode: Int,
    @JsonProperty("resourceName")
    val resourceName: String?,
    @JsonProperty("resourceType")
    val resourceType: String,
    @JsonProperty("cause")
    val cause: String
) : RioErrorMessage
