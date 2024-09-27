package com.spectralogic.rioclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import nl.altindag.ssl.util.TrustManagerUtils
import java.net.URL

internal class TokenClient(private val endpoint: URL, private val username: String, private val password: String) {
    private val api by lazy { "$endpoint/api" }

    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            acceptContentTypes = listOf(ContentType.Application.Json, ContentType("text", "json"))
            serializer = JacksonSerializer {
                registerModule(KotlinModule.Builder().build())
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            }
        }
        engine {
            https {
                this.trustManager = TrustManagerUtils.createUnsafeTrustManager()
            }
        }
        install(Logging)
    }

    suspend fun getShortToken(): String {
        val response: LoginTokenResponse = client.post("$api/tokens") {
            contentType(ContentType.Application.Json)
            body = UserLoginCredentials(username, password)
        }
        return response.token
    }
}
