package com.artifex.mupdf.viewer.app

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Universal AI Client supporting OpenAI-compatible APIs (OpenAI, DeepSeek, Qwen, etc.)
 */
class AiClient(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    private val systemPrompt: String
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json".toMediaType()

    fun explain(pageText: String): Flow<String> = callbackFlow {
        val isClaude = model.contains("claude") || baseUrl.contains("anthropic")
        
        val request = if (isClaude) {
            buildAnthropicRequest(pageText)
        } else {
            buildOpenAiRequest(pageText)
        }

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    close(IOException("HTTP ${response.code}: $errorBody"))
                    return
                }
                try {
                    response.body?.source()?.use { source ->
                        if (isClaude) {
                            parseAnthropicStream(source)
                        } else {
                            parseOpenAiStream(source)
                        }
                    }
                } finally {
                    close()
                }
            }
        })

        awaitClose { call.cancel() }
    }

    private fun buildOpenAiRequest(pageText: String): Request {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", "Please explain:\n\n$pageText"))
        }
        val apiUrl = if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("stream", true)
            .put("max_completion_tokens", 4096)
            .toString()
            .toRequestBody(jsonMediaType)

        return Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
    }

    private fun buildAnthropicRequest(pageText: String): Request {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "user").put("content", "Please explain:\n\n$pageText"))
        }
        val apiUrl = if (baseUrl.endsWith("/")) "${baseUrl}messages" else "$baseUrl/messages"
        val body = JSONObject()
            .put("model", model)
            .put("system", systemPrompt)
            .put("messages", messages)
            .put("stream", true)
            .put("max_tokens", 4096)
            .toString()
            .toRequestBody(jsonMediaType)

        return Request.Builder()
            .url(apiUrl)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body)
            .build()
    }

    private fun ProducerScope<String>.parseOpenAiStream(source: okio.BufferedSource) {
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ").trim()
            if (data == "[DONE]") break
            try {
                val delta = JSONObject(data).getJSONArray("choices").getJSONObject(0).getJSONObject("delta")
                if (delta.has("content") && !delta.isNull("content")) {
                    val content = delta.getString("content")
                    if (content.isNotEmpty()) trySend(content)
                }
            } catch (_: Exception) { }
        }
    }

    private fun ProducerScope<String>.parseAnthropicStream(source: okio.BufferedSource) {
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ").trim()
            try {
                val json = JSONObject(data)
                if (json.optString("type") == "content_block_delta") {
                    val delta = json.getJSONObject("delta")
                    if (delta.optString("type") == "text_delta") {
                        val text = delta.optString("text")
                        if (text.isNotEmpty()) trySend(text)
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
