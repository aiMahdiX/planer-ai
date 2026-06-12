package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null // "application/json"
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

/**
 * The expected structure returned by the Gemini model for our tasks prioritization.
 */
@JsonClass(generateAdapter = true)
data class AIResponse(
    @Json(name = "prioritizedTasks") val prioritizedTasks: List<AIPrioritizedTask>,
    @Json(name = "overallInsight") val overallInsight: String
)

@JsonClass(generateAdapter = true)
data class AIPrioritizedTask(
    @Json(name = "id") val id: Int,
    @Json(name = "aiPriorityScore") val aiPriorityScore: Int,
    @Json(name = "priority") val priority: String, // HIGH, MEDIUM, LOW
    @Json(name = "aiReasoning") val aiReasoning: String
)
