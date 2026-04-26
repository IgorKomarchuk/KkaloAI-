package com.kkaloai.app.ui.planner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.Chat
import com.google.gson.Gson
import com.kkaloai.app.R
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.data.remote.GeminiRepository
import com.kkaloai.app.util.ScannerError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isSystemAddedPlan: Boolean = false
)

data class PlannerJsonBlock(
    val planned_meals: List<PlannedMeal>
)

data class PlannedMeal(
    val name: String,
    val calories: Int,
    val proteins: Float,
    val carbs: Float,
    val fats: Float
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiRepository: GeminiRepository,
    private val mealDao: MealDao,
    private val gson: Gson
) : ViewModel() {

    private var chat: Chat? = null

    private val _messages = MutableStateFlow(
        listOf(ChatMessage(text = "Привіт! Я твій AI-нутріціолог. Що плануємо сьогодні їсти?", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        _messages.value = _messages.value + ChatMessage(text = text, isUser = true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                if (chat == null) chat = geminiRepository.startPlannerChat()
                val activeChat = chat ?: throw IllegalStateException("Planner chat not initialized")

                val response = activeChat.sendMessage(text)
                var replyText = response.text ?: ""

                val jsonRegex = "```json(.*?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
                val match = jsonRegex.find(replyText)

                var mealsAdded = false
                if (match != null) {
                    val jsonStr = match.groupValues[1].trim()
                    try {
                        val parsed = gson.fromJson(jsonStr, PlannerJsonBlock::class.java)
                        val plannedBaseTime = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        parsed.planned_meals.forEachIndexed { index, pm ->
                            mealDao.insertMeal(
                                MealEntry(
                                    name = pm.name,
                                    calories = pm.calories,
                                    proteins = pm.proteins,
                                    carbs = pm.carbs,
                                    fats = pm.fats,
                                    timestamp = plannedBaseTime + index * 3_600_000L,
                                    isPlanned = true
                                )
                            )
                        }
                        mealsAdded = true
                        replyText = replyText.replace(match.value, "").trim()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (replyText.isNotEmpty()) {
                    _messages.value = _messages.value + ChatMessage(text = replyText, isUser = false)
                }

                if (mealsAdded) {
                    _messages.value = _messages.value + ChatMessage(
                        text = "✅ Збережено у ваш план на сьогодні. Натискайте на страву в Dashboard → 'Mark eaten' коли з'їсте.",
                        isUser = false,
                        isSystemAddedPlan = true
                    )
                }

            } catch (e: Exception) {
                val mapped = ScannerError.fromException(e)
                _messages.value = _messages.value + ChatMessage(
                    text = context.getString(mapped.messageRes),
                    isUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
