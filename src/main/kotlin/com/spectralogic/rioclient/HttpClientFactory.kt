package com.spectralogic.rioclient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.jetty.Jetty
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.eclipse.jetty.util.ssl.SslContextFactory

object HttpClientFactory {
    fun createHttpClient(
        username: String,
        password: String,
        verbose: Boolean,
        requestTimeout: Long,
    ): HttpClient =
        HttpClient(Jetty) {
            engine {
                sslContextFactory = SslContextFactory.Client(true)
                clientCacheSize = 12
            }
            install(HttpTimeout) {
                requestTimeoutMillis = requestTimeout
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
            install(Auth) {
                bearer {
                    refreshTokens {
                        val url = this.response.request.url
                        val host = url.host
                        val port = url.port
                        val protocol = url.protocol.name
                        val response: LoginTokenResponse =
                            client
                                .post("$protocol://$host:$port/api/tokens") {
                                    contentType(ContentType.Application.Json)
                                    setBody(UserLoginCredentials(username, password))
                                }.body()
                        BearerTokens(response.token, "")
                    }
                }
            }
            install(Logging) {
                level =
                    if (verbose) {
                        LogLevel.ALL
                    } else {
                        LogLevel.NONE
                    }
            }
        }
}
