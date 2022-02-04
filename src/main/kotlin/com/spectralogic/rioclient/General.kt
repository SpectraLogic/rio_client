/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
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
    fun httpMethod() = httpMethod
    fun url() = urlStr
    fun cause() = cause
    fun statusCode() = statusCode
    fun errorMessage(): RioDefaultErrorMessage {
        val payload = "{${cause.message?.substringAfter("{")?.substringBeforeLast("}") ?: "Unknown"}}"
        return cram(payload, statusCode.value) ?: RioDefaultErrorMessage("${cause.message}", statusCode.value)
    }
}

private fun cram(payload: String, statusCode: Int): RioDefaultErrorMessage? {
    listOf(
        RioResourceErrorMessage::class.java,
        RioValidationErrorMessage::class.java,
        RioUnsupportedMediaError::class.java,
        RioDownstreamErrorMessage::class.java,
        RioResourceErrorMessage::class.java
    ).forEach {
        try {
            return mapper.readValue(payload, it)
        } catch (t: Throwable) { }
    }
    return null
}

open class RioDefaultErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    open val message: String,
    @JsonProperty("statusCode")
    open val statusCode: Int
)

open class RioResourceErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    override val message: String,
    @JsonProperty("statusCode")
    override val statusCode: Int,
    @JsonProperty("resourceName")
    val resourceName: String,
    @JsonProperty("resourceType")
    val resourceType: String
) : RioDefaultErrorMessage(message, statusCode)

open class RioValidationErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    override val message: String,
    @JsonProperty("statusCode")
    override val statusCode: Int,
    @JsonProperty("errors")
    val errors: List<RioValidationMessage>
) : RioDefaultErrorMessage(message, statusCode)

open class RioValidationMessage
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

open class RioUnsupportedMediaError
@JsonCreator constructor (
    @JsonProperty("message")
    override val message: String,
    @JsonProperty("statusCode")
    override val statusCode: Int,
    @JsonProperty("suppliedMediaType")
    val suppliedMediaType: String,
    @JsonProperty("supportedMediaType")
    val supportedMediaType: String
) : RioDefaultErrorMessage(message, statusCode)

open class RioDownstreamErrorMessage
@JsonCreator constructor (
    @JsonProperty("message")
    override val message: String,
    @JsonProperty("statusCode")
    override val statusCode: Int,
    @JsonProperty("resourceName")
    val resourceName: String?,
    @JsonProperty("resourceType")
    val resourceType: String,
    @JsonProperty("cause")
    val cause: String
) : RioDefaultErrorMessage(message, statusCode)
