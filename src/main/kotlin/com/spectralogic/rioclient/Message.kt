/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MessageResponse(
    @Serializable(with = UUIDSerializer::class)
    @SerialName("id")
    val messageId: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val read: Boolean,
    val subject: MessageSubjectResponse,
    val details: MessageDetailsResponse,
    val severity: String
) : RioResponse()

@Serializable
data class MessageData(
    @SerialName("id")
    @Serializable(with = UUIDSerializer::class)
    val messageId: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val read: Boolean,
    val subject: MessageSubjectResponse,
    val details: MessageDetailsResponse,
    val severity: String
)

@Serializable
data class MessageSubjectResponse(
    val key: String,
    val parameters: Map<String, String>?,
    val text: String
) : RioResponse()

@Serializable
data class MessageDetailsResponse(
    val key: String,
    val parameters: Map<String, String>?,
    val text: String
) : RioResponse()

@Serializable
data class MessageUpdateRequest(
    val read: Boolean
) : RioRequest

@Serializable
data class MessageListResponse(
    val data: List<MessageData>,
    val page: PageInfo
) : RioResponse()
