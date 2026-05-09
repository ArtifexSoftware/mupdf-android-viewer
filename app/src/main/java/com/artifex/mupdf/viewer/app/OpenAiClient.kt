package com.artifex.mupdf.viewer.app

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class OpenAiClient(val apiKey: String) {

    companion object {
        // Update this to match your OpenAI model ID exactly
        const val MODEL = "gpt-5.4-nano"
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
        private val SYSTEM_PROMPT = """
            You are an expert academic reading assistant embedded in a PDF reader.
            When given text from a PDF page, respond in two sections:

            ## AI Explanation
            Explain the core concepts clearly in 2-4 sentences. Use plain language.

            ## Atomic Note
            **Core Concept:** [one sentence summary]
            **Key Insight:** [the most important takeaway]
            **Connections:** [2-3 related concepts or fields]

            Be concise. Focus on depth over breadth.
        """.trimIndent()
    }

    private val httpClient = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()

    fun explain(pageText: String): Flow<String> = callbackFlow {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            put(JSONObject().put("role", "user").put("content",
                "Please explain and generate an atomic note for:\n\n$pageText"))
        }

        val requestBody = JSONObject()
            .put("model", MODEL)
            .put("messages", messages)
            .put("stream", true)
            .put("max_completion_tokens", 800)
            .toString()
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = httpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    close(IOException("HTTP ${response.code}: ${response.body?.string()}"))
                    return
                }
                try {
                    response.body?.source()?.use { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data: ")) continue
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            try {
                                val content = JSONObject(data)
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("delta")
                                    .optString("content", "")
                                if (content.isNotEmpty()) trySend(content)
                            } catch (_: Exception) { }
                        }
                    }
                } finally {
                    close()
                }
            }
        })

        awaitClose { call.cancel() }
    }
}
