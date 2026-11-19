package com.example.network

import com.example.model.ApiConfiguration
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AirLabsAuthAndGatewayInterceptor(
    private val configRef: AtomicReference<ApiConfiguration>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val cfg = configRef.get()

        val targetBase = if (cfg.useGateway && cfg.gatewayBaseUrl.isNotBlank()) {
            val normalized = if (cfg.gatewayBaseUrl.endsWith("/")) cfg.gatewayBaseUrl else "${cfg.gatewayBaseUrl}/"
            normalized.toHttpUrlOrNull()
        } else {
            null
        }

        val urlBuilder = original.url.newBuilder()
        if (targetBase != null) {
            urlBuilder.scheme(targetBase.scheme)
            urlBuilder.host(targetBase.host)
            urlBuilder.port(targetBase.port)
            val originalSegments = original.url.pathSegments.filter { it.isNotEmpty() }
            // Keep endpoint segment (e.g. flights, flight, schedules) appended to gateway path
            val endpoint = originalSegments.lastOrNull() ?: ""
            urlBuilder.encodedPath(targetBase.encodedPath + endpoint)
        } else if (cfg.apiKey.isNotBlank()) {
            urlBuilder.addQueryParameter("api_key", cfg.apiKey)
        }

        val reqBuilder = original.newBuilder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")

        if (cfg.useGateway && cfg.gatewayBearerToken.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer ${cfg.gatewayBearerToken}")
        }

        val request = reqBuilder.build()
        var attempt = 0
        var lastException: IOException? = null
        while (attempt < 2) {
            try {
                val response = chain.proceed(request)
                if (response.code in listOf(502, 503, 504) && attempt == 0) {
                    response.close()
                    attempt++
                    Thread.sleep(600L)
                    continue
                }
                return response
            } catch (e: IOException) {
                lastException = e
                if (attempt == 0) {
                    attempt++
                    try {
                        Thread.sleep(500L)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IOException("Unknown network error")
    }
}

object NetworkModule {
    val apiConfigRef = AtomicReference(ApiConfiguration())

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val airLabsOkHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AirLabsAuthAndGatewayInterceptor(apiConfigRef))
            .build()
    }

    private val weatherOkHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val airLabsService: AirLabsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AirLabsApiService.DEFAULT_BASE_URL)
            .client(airLabsOkHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AirLabsApiService::class.java)
    }

    val openMeteoService: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApiService.BASE_URL)
            .client(weatherOkHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }
}
