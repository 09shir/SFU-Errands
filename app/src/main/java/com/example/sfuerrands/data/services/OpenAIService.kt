package com.example.sfuerrands.data.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

import com.example.sfuerrands.BuildConfig

class OpenAIService {

    private val apiKey = BuildConfig.OPENAI_API_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun refineDescription(userText: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openai.com/v1/chat/completions"

                // Construct the JSON Body using Android's native JSONObject
                val jsonBody = JSONObject()
                jsonBody.put("model", "gpt-3.5-turbo")

                val messages = JSONArray()

                // System prompt to set behavior
                val systemMessage = JSONObject()
                systemMessage.put("role", "system")
                systemMessage.put("content", "You are a professional editor. Rewrite the following errand description to be clear, concise, and professional. Return ONLY the rewritten text, do not add conversational filler.")
                messages.put(systemMessage)

                // User prompt
                val userMessage = JSONObject()
                userMessage.put("role", "user")
                userMessage.put("content", userText)
                messages.put(userMessage)

                jsonBody.put("messages", messages)
                jsonBody.put("temperature", 0.7)

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e("OpenAI", "Error: $responseString")
                    return@withContext null
                }

                if (responseString != null) {
                    // Parse response
                    val jsonResponse = JSONObject(responseString)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val message = firstChoice.getJSONObject("message")
                        return@withContext message.getString("content").trim()
                    }
                }
                return@withContext null

            } catch (e: Exception) {
                Log.e("OpenAI", "Exception", e)
                return@withContext null
            }
        }
    }

    suspend fun generateSmartReplies(incomingMessage: String, contextTitle: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openai.com/v1/chat/completions"
                val jsonBody = JSONObject()
                jsonBody.put("model", "gpt-3.5-turbo")

                val messages = JSONArray()

                // System prompt: crucial for formatting
                val systemMessage = JSONObject()
                systemMessage.put("role", "system")
                systemMessage.put(
                    "content",
                    "You are a helpful assistant in an errand-running app. " +
                            "Based on the last message received, generate exactly 3 short, casual, and polite reply options (1-5 words each). " +
                            "Do not use numbering. Separate the three options with '|||'. " +
                            "Example output: Sounds good!|||On my way.|||Can you clarify?"
                )
                messages.put(systemMessage)

                // User prompt: include context
                val userMessage = JSONObject()
                userMessage.put("role", "user")
                userMessage.put("content", "Context: Errand '$contextTitle'. Received message: '$incomingMessage'")
                messages.put(userMessage)

                jsonBody.put("messages", messages)
                jsonBody.put("temperature", 0.7) // Slightly creative
                jsonBody.put("max_tokens", 50)   // Keep it short to save money

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string()

                if (responseString != null && response.isSuccessful) {
                    val jsonResponse = JSONObject(responseString)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val content = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        // Split by our separator and clean up
                        return@withContext content.split("|||").map { it.trim() }
                    }
                }
                return@withContext emptyList()

            } catch (e: Exception) {
                Log.e("OpenAIService", "Smart Reply Error", e)
                return@withContext emptyList()
            }
        }
    }
}