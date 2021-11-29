package com.spectralogic.rioclient

import com.fasterxml.jackson.annotation.JsonProperty

data class ListMetadataValuesDistinct(@JsonProperty("results") override val objects: List<Map<String, String>>, override val page: PageInfo) : PageData<Map<String, String>>
