package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class MessageResponse(
    val messageId: UUID,
    val creationDate: String,
    val lastUpdated: String,
    val read: Boolean,
    val subject: MessageSubjectResponse,
    val details: MessageDetailsResponse,
    val severity: String
)

data class MessageSubjectResponse(
    val key: String,
    val parameters: Map<String, String>,
    val text: String
)

data class MessageDetailsResponse(
    val key: String,
    val parameters: Map<String, String>,
    val text: String
)

data class MessageUpdateRequest(
    val read: Boolean
) : RioRequest

data class MessagesListResponse(
    @JsonProperty("messaGES") override val objects: List<MessageResponse>,
    override val page: PageInfo
) : PageData<MessageResponse>
