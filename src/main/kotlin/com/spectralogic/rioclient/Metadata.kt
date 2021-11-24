package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.request.get
import io.ktor.client.request.header

data class ListMetadataValuesDistinct(@JsonProperty("results") override val objects: List<Map<String, String>>, override val page: PageInfo) : PageData<Map<String, String>>


