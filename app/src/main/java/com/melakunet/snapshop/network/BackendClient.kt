package com.melakunet.snapshop.network

import com.melakunet.snapshop.BuildConfig
import com.melakunet.snapshop.models.IdentifyResult
import com.melakunet.snapshop.models.ProductReviews
import com.melakunet.snapshop.models.ShopItem
import com.melakunet.snapshop.models.TranscribeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Mirrors iOS BackendError cases. */
sealed class BackendException(message: String) : Exception(message) {
    class Http(val code: Int, body: String) : BackendException("HTTP " + code + ": " + body)
    class NoProductsFound(message: String) : BackendException(message)
    class PlantUnidentified(message: String) : BackendException(message)
    class Decoding(cause: Throwable) : BackendException("Decode error: " + cause.message)
}

/**
 * 1:1 port of iOS Network/BackendClient.swift — same endpoints, same multipart
 * field names ("image", "barcode", "frames[]", "hint", "audio"), same 422
 * error-envelope handling.
 */
object BackendClient {

    /** Set at startup; returns the current auth token or null when signed out. */
    var tokenProvider: (() -> String?)? = null

    private val baseUrl = BuildConfig.BACKEND_URL

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val longPollingClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val jpeg = "image/jpeg".toMediaType()
    private val jsonMedia = "application/json".toMediaType()

    suspend fun identifyPrecision(imageBytes: ByteArray, barcode: String? = null): IdentifyResult {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            addFormDataPart("image", "scan.jpg", imageBytes.toRequestBody(jpeg))
            if (!barcode.isNullOrEmpty()) addFormDataPart("barcode", barcode)
        }.build()
        return execute("/identify/precision", "POST", body)
    }

    suspend fun shop(
        query: String,
        retailerWhitelist: List<String> = emptyList(),
        sort: String = "price",
    ): List<ShopItem> {
        val payload = json.encodeToString(
            ShopRequestBody.serializer(),
            ShopRequestBody(query, retailerWhitelist, sort),
        )
        return execute("/shop", "POST", payload.toRequestBody(jsonMedia))
    }

    /** Full precision scan. Returns (product, emptyList()) when shopping is suppressed or fails. */
    suspend fun scan(
        imageBytes: ByteArray,
        barcode: String? = null,
        whitelist: List<String> = emptyList(),
    ): Pair<IdentifyResult, List<ShopItem>> {
        val product = identifyPrecision(imageBytes, barcode)
        val query = product.searchQuery.trim()
        if (query.isEmpty()) return product to emptyList()
        val prices = try {
            shop(query, whitelist)
        } catch (e: Exception) {
            android.util.Log.e("BackendClient", "Soft-failure on shop() during scan", e)
            emptyList()
        }
        return product to prices
    }

    suspend fun identifyDeep(frames: List<ByteArray>, hint: String? = null): IdentifyResult {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            frames.forEachIndexed { i, frame ->
                addFormDataPart("frames[]", "frame_" + i + ".jpg", frame.toRequestBody(jpeg))
            }
            if (!hint.isNullOrEmpty()) addFormDataPart("hint", hint)
        }.build()
        return call(authorizedRequest(baseUrl + "/identify/deep", "POST", body), longPollingClient)
    }

    suspend fun identifyUrl(
        url: String,
        whitelist: List<String> = emptyList(),
    ): Pair<IdentifyResult, List<ShopItem>> {
        val payload = json.encodeToString(UrlIdentifyBody.serializer(), UrlIdentifyBody(url))
        val product: IdentifyResult = execute("/identify/url", "POST", payload.toRequestBody(jsonMedia))
        val prices = if (product.searchQuery.isBlank()) emptyList()
        else {
            try {
                shop(product.searchQuery, whitelist)
            } catch (e: Exception) {
                android.util.Log.e("BackendClient", "Soft-failure on shop() during identifyUrl", e)
                emptyList()
            }
        }
        return product to prices
    }

    suspend fun productReviews(productId: String): ProductReviews {
        val url = (baseUrl + "/product/reviews").toHttpUrl()
            .newBuilder().addQueryParameter("product_id", productId).build()
        return call(authorizedRequest(url.toString(), "GET", null))
    }

    suspend fun transcribe(audioBytes: ByteArray, filename: String = "audio.m4a"): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("audio", filename, audioBytes.toRequestBody("audio/m4a".toMediaType()))
            .build()
        val response: TranscribeResponse = execute("/transcribe", "POST", body)
        return response.transcript
    }

    private fun authorizedRequest(url: String, method: String, body: RequestBody?): Request {
        val builder = Request.Builder().url(url).method(method, body)
        tokenProvider?.invoke()?.let { builder.header("Authorization", "Bearer " + it) }
        return builder.build()
    }

    private suspend inline fun <reified T> execute(path: String, method: String, body: RequestBody?): T =
        call(authorizedRequest(baseUrl + path, method, body), client)

    private suspend inline fun <reified T> call(request: Request, okHttpClient: OkHttpClient = client): T = withContext(Dispatchers.IO) {
        val response = okHttpClient.newCall(request).await()
        response.use {
            val bytes = it.body?.string() ?: ""
            checkHttp(it.code, bytes)
            try {
                json.decodeFromString<T>(bytes)
            } catch (e: Exception) {
                throw BackendException.Decoding(e)
            }
        }
    }

    private fun checkHttp(code: Int, body: String) {
        if (code in 200..299) return
        if (code == 422) {
            runCatching { json.decodeFromString<ErrEnvelope>(body) }.getOrNull()?.let { env ->
                if (env.error.code == "plant_unidentified") {
                    throw BackendException.PlantUnidentified(env.error.message)
                }
                throw BackendException.NoProductsFound(env.error.message)
            }
        }
        throw BackendException.Http(code, body.take(300))
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = cont.resume(response)
            override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)
        })
        cont.invokeOnCancellation { runCatching { cancel() } }
    }

    @Serializable
    private data class ShopRequestBody(
        val query: String,
        val retailer_whitelist: List<String>,
        val sort: String,
    )

    @Serializable
    private data class UrlIdentifyBody(val url: String)

    @Serializable
    private data class ErrEnvelope(val error: Inner) {
        @Serializable
        data class Inner(val code: String? = null, val message: String)
    }
}
