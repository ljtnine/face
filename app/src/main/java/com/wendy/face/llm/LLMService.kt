package com.wendy.face.llm

import android.content.Context
import android.util.Log
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.wendy.face.model.ThreeCourtFiveEyeResult
import com.wendy.face.model.TwelvePalacesData
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class LLMService(private val context: Context) {

    private val openAI: OpenAI

    init {
        val sharedPreferences = context.getSharedPreferences("face_app_settings", Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString("llm_base_url", "https://ark.cn-beijing.volces.com/api/v3/") ?: "https://ark.cn-beijing.volces.com/api/v3/"
        val apiKey = sharedPreferences.getString("llm_api_key", "") ?: ""

        openAI = OpenAI(
            token = if (apiKey.isNotBlank()) apiKey else com.wendy.face.BuildConfig.VOLC_API_KEY,
            timeout = Timeout(socket = 60.seconds * 5),
            host = OpenAIHost(baseUrl = baseUrl),
            httpClientConfig = {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        isLenient = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 300000 // 整个请求的超时时间 5分钟
                    socketTimeoutMillis = 900000 // 读取数据的超时时间 15分钟
                }
            }
        )
    }

    fun getDestiny(
        palaces: TwelvePalacesData,
        threeCourtFiveEyeResult: ThreeCourtFiveEyeResult?,
        personalization: String
    ): Flow<String> {
        val sharedPreferences = context.getSharedPreferences("face_app_settings", Context.MODE_PRIVATE)
        val model = sharedPreferences.getString("llm_model", "deepseek-v3-250324") ?: "deepseek-v3-250324"
        val personalizationFromSettings = sharedPreferences.getString("personalization", "") ?: ""

        val finalPersonalization = if (personalization.isNotBlank()) {
            personalization
        } else {
            personalizationFromSettings
        }

        val threeCourtFiveEyePrompt = if (threeCourtFiveEyeResult != null) {
            """
            三庭五眼分析：
            - 三庭：${threeCourtFiveEyeResult.threeCourt.description}
            - 五眼：${threeCourtFiveEyeResult.fiveEye.description}
            """
        } else {
            ""
        }

        val prompt = """
            请根据以下面相分析结果，综合推断命格。请重点分析各部分之间的关联，并给出整体性的结论。

            十二宫分析：
            ${palaces.palaces.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
            
            $threeCourtFiveEyePrompt

            个人化解读需求：
            ${if (finalPersonalization.isNotBlank()) finalPersonalization else "无特定需求"}

            不要输出markdown格式
        """.trimIndent()

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "你是一位精通命理学的专家。"
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = prompt
                )
            )
        )

        return try {
            // Try streaming first
            val completions: Flow<ChatCompletionChunk> = openAI.chatCompletions(chatCompletionRequest)

            completions
                .map { chunk ->
                    val content = chunk.choices.firstOrNull()?.delta?.content.orEmpty()
                    android.util.Log.d("LLMService", "Received chunk: $content")
                    content
                }
                .catch { exception ->
                    Log.e("LLMService", "Error in streaming response, falling back to non-streaming", exception)
                    emit("抱歉，无法获取命理分析结果。请稍后重试。 错误信息: ${exception.message}")
                }
        } catch (exception: Exception) {
            Log.e("LLMService", "Error creating streaming request, using non-streaming", exception)
            flow { emitAll(getDestinyNonStreaming(chatCompletionRequest)) }
        }
    }

    private fun getDestinyNonStreaming(request: ChatCompletionRequest): Flow<String> = flow {
        try {
            val completion: ChatCompletion = openAI.chatCompletion(request)
            val content = completion.choices.firstOrNull()?.message?.content.orEmpty()
            Log.d("LLMService", "Received non-streaming response: $content")

            // Emit the content character by character to simulate streaming
            content.forEach { char ->
                emit(char.toString())
                kotlinx.coroutines.delay(10) // Small delay to simulate streaming
            }
        } catch (exception: Exception) {
            Log.e("LLMService", "Error in non-streaming response", exception)
            emit("抱歉，无法获取命理分析结果。请稍后重试。 错误信息: ${exception.message}")
        }
    }
}