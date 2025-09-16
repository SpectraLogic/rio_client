package com.spectralogic.rioclient

import kotlinx.serialization.Serializable

@Serializable
data class CopyRequest(
    val name: String? = null,
    val targetAgent: String,
    val files: List<String>,
) : RioRequest

@Serializable
data class DeleteRequest(
    val name: String? = null,
    val agent: String,
    val files: List<String>,
) : RioRequest
