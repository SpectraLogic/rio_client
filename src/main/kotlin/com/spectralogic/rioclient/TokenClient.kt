package com.spectralogic.rioclient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.net.URL

internal class TokenClient(
    private val endpoint: URL,
    private val username: String,
    private val password: String
) : Closeable {
    private val api by lazy { "$endpoint/api" }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
        engine {
            https {
                trustManager = TrustManager
            }
        }
        install(Logging)
    }

    suspend fun getShortToken(): String {
        val response: ShortTokenResponse = client.post("$api/tokens") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginCredentials(username, password))
        }.body()
        return response.token
    }

    override fun close() {
        client.close()
    }
}
