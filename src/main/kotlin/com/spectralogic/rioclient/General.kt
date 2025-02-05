/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

const val RioRequestDiscriminator = "rio_request_type"

@Serializable
@JsonClassDiscriminator(RioRequestDiscriminator)
sealed interface RioRequest

@Serializable
data class RioEmptyRequest(
    val contentLength: Long = 0,
) : RioRequest

@Serializable
open class RioResponse {
    @Serializable(with = HttpStatusCodeSerializer::class)
    var statusCode = HttpStatusCode.Processing
}

interface RioListResponse<T> {
    fun page(): PageInfo

    fun results(): List<T>
}

@Serializable
class EmptyResponse : RioResponse()

@Serializable
data class PageInfo(
    val number: Long,
    val pageSize: Long,
    val totalPages: Long,
    val totalItems: Long = 0L,
)

class RioHttpException(
    val httpMethod: HttpMethod,
    val urlStr: String,
    override val cause: Throwable? = null,
    val statusCode: Int = HttpStatusCode.BadRequest.value,
    private val payload: String? = null,
) : RuntimeException(cause?.message ?: payload, cause) {
    private val rioErrorMessage: RioErrorMessage =
        payload.asRioErrorMessage(statusCode)
            ?: RioDefaultErrorMessage(cause?.message ?: "Error", statusCode)

    fun httpMethod() = httpMethod

    fun statusCode() = statusCode

    fun url() = urlStr

    fun cause() = cause

    fun errorMessage(): RioErrorMessage = rioErrorMessage
}

fun String?.asRioErrorMessage(statusCode: Int): RioErrorMessage? {
    if (!this.isNullOrBlank()) {
        try {
            return Json.decodeFromString<RioResourceErrorMessage>(this)
        } catch (_: Throwable) {
        }
        try {
            return Json.decodeFromString<RioValidationErrorMessage>(this)
        } catch (_: Throwable) {
        }
        try {
            return Json.decodeFromString<RioUnsupportedMediaErrorMessage>(this)
        } catch (_: Throwable) {
        }
        try {
            return Json.decodeFromString<RioDownstreamErrorMessage>(this)
        } catch (_: Throwable) {
        }
        try {
            return Json.decodeFromString<RioDefaultErrorMessage>(this)
        } catch (_: Throwable) {
        }
    }
    return null
}

interface RioErrorMessage {
    val message: String
    val statusCode: Int
}

@Serializable
data class RioDefaultErrorMessage
    constructor(
        override val message: String,
        override val statusCode: Int,
    ) : RioErrorMessage

@Serializable
data class RioResourceErrorMessage
    constructor(
        override val message: String,
        override val statusCode: Int,
        val resourceName: String,
        val resourceType: String,
    ) : RioErrorMessage

@Serializable
data class RioValidationErrorMessage
    constructor(
        override val message: String,
        override val statusCode: Int,
        val errors: List<RioValidationMessage>,
    ) : RioErrorMessage

@Serializable
data class RioValidationMessage
    constructor(
        val fieldName: String,
        val fieldType: String,
        val errorType: String,
        val value: String? = null,
        val reason: String? = null,
    )

@Serializable
data class RioUnsupportedMediaErrorMessage
    constructor(
        override val message: String,
        override val statusCode: Int,
        val suppliedMediaType: String,
        val supportedMediaType: String,
    ) : RioErrorMessage

@Serializable
data class RioDownstreamErrorMessage
    constructor(
        override val message: String,
        override val statusCode: Int,
        val resourceName: String?,
        val resourceType: String,
        val cause: String,
    ) : RioErrorMessage
