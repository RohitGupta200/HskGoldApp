package org.cap.gold.di

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.Response
import org.cap.gold.auth.TokenManager
import org.koin.dsl.module
import java.io.File

/**
 * Android override module: provides an HttpClient(OkHttp) with disk cache and
 * X-Cache-Ttl-aware response header rewrite to enable persistent caching
 * without server changes.
 */
val androidNetworkOverrideModule = module {
    single<HttpClient> {
        val context: Context = get()
        val tokenManager: TokenManager = get()
        val baseUrl: String = getKoin().getProperty("api.base.url") ?: "http://10.0.2.2:8080"

        HttpClient(OkHttp) {
            engine {
                config {
                    // 20 MB disk cache
                    cache(Cache(File(context.cacheDir, "http_cache"), 20L * 1024L * 1024L))
                    // Interceptor to respect our per-request TTL header
                    addNetworkInterceptor(Interceptor { chain ->
                        val req = chain.request()
                        val ttl = req.header("X-Cache-Ttl")?.toLongOrNull()
                        val res = chain.proceed(req)
                        if (ttl != null && ttl > 0) {
                            // If server didn't provide explicit caching, inject it
                            if (res.header("Cache-Control").isNullOrBlank()) {
                                res.newBuilder()
                                    .header("Cache-Control", "public, max-age=$ttl")
                                    .build()
                            } else res
                        } else res
                    })
                }
            }

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = false
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(Logging) { level = LogLevel.INFO }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        tokenManager.tokens.value?.let { t ->
                            BearerTokens(t.accessToken, t.refreshToken)
                        }
                    }
                    refreshTokens {
                        val nt = tokenManager.refreshToken()
                        nt?.let { BearerTokens(it.accessToken, it.refreshToken) }
                    }
                    sendWithoutRequest { true }
                }
            }
        }
    }
}
