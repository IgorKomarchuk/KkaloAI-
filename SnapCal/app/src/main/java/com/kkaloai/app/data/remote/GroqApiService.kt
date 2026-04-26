package com.kkaloai.app.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val response_format: GroqResponseFormat? = null,
    val temperature: Double = 0.5
)

data class GroqMessage(
    val role: String,
    val content: Any // Can be a String or a List<GroqContentPart> for Vision
)

data class GroqContentPart(
    val type: String,
    val text: String? = null,
    val image_url: GroqImageUrl? = null
)

data class GroqImageUrl(
    val url: String // format: "data:image/jpeg;base64,{base64_string}"
)

data class GroqResponseFormat(
    val type: String = "json_object"
)

data class GroqChatResponse(
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: GroqResponseMessage
)

data class GroqResponseMessage(
    val role: String,
    val content: String
)

interface GroqApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse
}
