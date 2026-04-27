package com.kkaloai.app.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.kkaloai.app.data.local.BiofeedbackDao
import com.kkaloai.app.data.local.BiofeedbackEntry
import com.kkaloai.app.data.local.HealthGoal
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.data.model.GeminiFoodResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val groqApiService: GroqApiService,
    private val gson: Gson,
    private val userPreferences: UserPreferences,
    private val biofeedbackDao: BiofeedbackDao,
    private val foodCache: FoodCacheRepository
) {
    private suspend fun buildUserContext(): String {
        val goal = userPreferences.healthGoal.first()
        val glp1 = userPreferences.isGlp1Mode.first()
        val hormonal = userPreferences.isHormonalMode.first()

        val lines = mutableListOf<String>()
        when (goal) {
            HealthGoal.LOSS -> lines += "GOAL: Fat loss — conservative portion estimates, highlight protein content."
            HealthGoal.MAINTAIN -> lines += "GOAL: Maintenance — balanced tone."
            HealthGoal.BULK -> lines += "GOAL: Muscle gain / bulking — celebrate calorie-dense whole foods; never shame quantity."
        }
        if (glp1) lines += "GLP-1 MEDICATION (Ozempic/Wegovy): prioritize protein adequacy and nutrient density; note that early satiety is common so dense small portions are good."
        if (hormonal) lines += "HORMONAL/PCOS FOCUS: pay attention to glycemic load, fat quality (omega-3 vs saturated), and insulin-balancing nutrients."
        return lines.joinToString("\n")
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val maxSize = 1024
        val ratio = Math.min(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        val width = Math.round(ratio * bitmap.width)
        val height = Math.round(ratio * bitmap.height)
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun recognizeFood(bitmap: Bitmap, userHint: String? = null): Result<GeminiFoodResponse> = withContext(Dispatchers.IO) {
        try {
            // Cache lookup — only when the user gave no override hint
            if (userHint.isNullOrBlank()) {
                val hash = com.kkaloai.app.util.ImageHasher.aHashHex(bitmap)
                foodCache.lookup(hash)?.let { return@withContext Result.success(it) }
            }
            val userContext = buildUserContext()
            val prompt = """
                As an expert nutritionist, analyze this food image.
                Identify all food items, estimate their portion sizes, and calculate nutritional values.

                USER CONTEXT:
                $userContext

                CRITICAL - COOKING METHOD DETECTION:
                Analyze the visual appearance to determine if the food is fried, sautéed, steamed, boiled, raw, or baked.
                Pay close attention to oil sheen, charring, or textures indicating added fats (butter, oil).
                If the user provides a hint: "${userHint ?: "No hint provided"}", prioritize it if it matches visual evidence.

                IMPORTANT: Return ONLY a raw JSON object with the following structure:
                {
                  "items": [
                    {
                      "name": "string",
                      "calories": int,
                      "proteins": float,
                      "carbs": float,
                      "fats": float,
                      "confidence": float (0-1),
                      "cookingMethod": "string (e.g., Deep Fried, Steamed, Pan Sautéed)",
                      "servingSize": "string",
                      "ingredients": ["string"]
                    }
                  ],
                  "totalCalories": int,
                  "summary": "string (1 sentence; adapt tone to USER CONTEXT above)"
                }

                If you detect hidden oils or frying, ensure calories and fats reflect this.
                If unsure, estimate based on the most likely preparation method.
            """.trimIndent()

            val base64Image = encodeImageToBase64(bitmap)
            
            val request = GroqChatRequest(
                model = "meta-llama/llama-4-scout-17b-16e-instruct",
                messages = listOf(
                    GroqMessage(
                        role = "user",
                        content = listOf(
                            GroqContentPart(type = "text", text = prompt),
                            GroqContentPart(
                                type = "image_url",
                                image_url = GroqImageUrl(url = "data:image/jpeg;base64,$base64Image")
                            )
                        )
                    )
                ),
                response_format = GroqResponseFormat(type = "json_object")
            )

            val response = withRetry { groqApiService.createChatCompletion("Bearer ${com.kkaloai.app.BuildConfig.GROQ_API_KEY}", request) }
            val responseText = response.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response")
            val parsed = parseJson(responseText, GeminiFoodResponse::class.java)
            parsed.onSuccess { resp ->
                if (userHint.isNullOrBlank()) {
                    val hash = com.kkaloai.app.util.ImageHasher.aHashHex(bitmap)
                    foodCache.save(hash, resp)
                }
            }
            parsed
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recognizeFoodFromText(spoken: String): Result<GeminiFoodResponse> = withContext(Dispatchers.IO) {
        try {
            val userContext = buildUserContext()
            val prompt = """
                As an expert nutritionist, parse the user's spoken meal description and estimate nutrition.
                User said: "$spoken"

                USER CONTEXT:
                $userContext

                IMPORTANT: Return ONLY a raw JSON object with this structure:
                {
                  "items": [
                    {
                      "name": "string",
                      "calories": int,
                      "proteins": float,
                      "carbs": float,
                      "fats": float,
                      "confidence": float (0-1),
                      "cookingMethod": "string or null",
                      "servingSize": "string",
                      "ingredients": ["string"]
                    }
                  ],
                  "totalCalories": int,
                  "summary": "string"
                }

                Use typical serving sizes if unspecified. Be realistic and conservative on calories.
            """.trimIndent()

            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile",
                messages = listOf(
                    GroqMessage(role = "user", content = prompt)
                ),
                response_format = GroqResponseFormat(type = "json_object")
            )
            val response = withRetry { groqApiService.createChatCompletion("Bearer ${com.kkaloai.app.BuildConfig.GROQ_API_KEY}", request) }
            val responseText = response.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response")
            parseJson(responseText, GeminiFoodResponse::class.java)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeWeeklyProgress(meals: List<com.kkaloai.app.data.local.MealEntry>): Result<com.kkaloai.app.data.model.WeeklyReport> = withContext(Dispatchers.IO) {
        try {
            val userContext = buildUserContext()
            val mealHistory = meals.joinToString("\n") {
                "${it.name}: ${it.calories} kcal, P:${it.proteins}g, C:${it.carbs}g, F:${it.fats}g"
            }

            val now = System.currentTimeMillis()
            val sevenDaysAgo = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -7)
            }.timeInMillis
            val biofeedback = biofeedbackDao.getBetweenSync(sevenDaysAgo, now)
            val biofeedbackSummary = if (biofeedback.isEmpty()) {
                "No daily check-ins recorded."
            } else {
                biofeedback.joinToString("\n") { bf ->
                    val symptomsPart = if (bf.symptoms.isBlank()) "" else " symptoms: ${bf.symptoms};"
                    "Energy:${bf.energyLevel}/5 mood:${bf.mood};$symptomsPart"
                }
            }

            val prompt = """
                You are an elite AI Health Coach. Analyze the following meal history from the past week:
                $mealHistory

                RECENT WELLNESS CHECK-INS:
                $biofeedbackSummary

                USER CONTEXT:
                $userContext

                Identify trends (e.g., "low protein for 3 days", "energy dipped on high-carb days"), praise successes,
                and provide 3 ultra-targeted recipes that fix the current nutritional gaps while respecting USER CONTEXT.

                IMPORTANT: Return ONLY a raw JSON object with this structure:
                {
                  "summary": "Overall look at the week...",
                  "insights": ["Trend 1", "Trend 2"],
                  "recommendations": ["Do this...", "Avoid that..."],
                  "personalizedRecipes": [
                    {
                      "name": "Recipe Name",
                      "reason": "Why this is good for them right now",
                      "macros": "e.g. 400 kcal, 40g Protein",
                      "briefInstructions": "1-2 sentences"
                    }
                  ]
                }
            """.trimIndent()

            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile",
                messages = listOf(
                    GroqMessage(role = "user", content = prompt)
                ),
                response_format = GroqResponseFormat(type = "json_object")
            )
            val response = withRetry { groqApiService.createChatCompletion("Bearer ${com.kkaloai.app.BuildConfig.GROQ_API_KEY}", request) }
            val responseText = response.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response")
            parseJson(responseText, com.kkaloai.app.data.model.WeeklyReport::class.java)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recognizeRecipe(bitmap: Bitmap): Result<com.kkaloai.app.data.model.RecipeScanResponse> = withContext(Dispatchers.IO) {
        try {
            val userContext = buildUserContext()
            val prompt = """
                You are a recipe parser. The user photographed a recipe (printed page, screenshot, hand-written, or label).
                Extract the recipe and compute nutrition per serving.

                USER CONTEXT:
                $userContext

                Return ONLY raw JSON:
                {
                  "recipeName": "string",
                  "servings": int,
                  "ingredients": ["string with grams or units"],
                  "perServing": {
                     "name": "string", "calories": int, "proteins": float, "carbs": float, "fats": float,
                     "confidence": float (0-1), "cookingMethod": "string or null",
                     "servingSize": "1 serving", "ingredients": ["string"]
                  },
                  "totalCalories": int,
                  "summary": "1 sentence"
                }
            """.trimIndent()
            val request = visionRequest(prompt, encodeImageToBase64(bitmap))
            val response = withRetry { groqApiService.createChatCompletion(authHeader(), request) }
            val txt = response.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response")
            parseJson(txt, com.kkaloai.app.data.model.RecipeScanResponse::class.java)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun recognizeMenu(bitmap: Bitmap): Result<com.kkaloai.app.data.model.MenuScanResponse> = withContext(Dispatchers.IO) {
        try {
            val userContext = buildUserContext()
            val prompt = """
                Parse this restaurant menu photo. For each visible dish:
                - estimate calories from typical preparation
                - score 1-10 by nutrient density (high protein + veggies = high; deep-fried/sweets = low)
                - briefly note WHY it's high or low if useful

                USER CONTEXT:
                $userContext

                Return ONLY raw JSON, sorted by healthScore DESC:
                {
                  "items": [
                    { "name": "string", "estimatedCalories": int, "healthScore": int, "notes": "string or null" }
                  ],
                  "summary": "1 sentence on best 2-3 picks"
                }
            """.trimIndent()
            val request = visionRequest(prompt, encodeImageToBase64(bitmap))
            val response = withRetry { groqApiService.createChatCompletion(authHeader(), request) }
            val txt = response.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response")
            parseJson(txt, com.kkaloai.app.data.model.MenuScanResponse::class.java)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun suggestFromFridge(bitmap: Bitmap): Result<com.kkaloai.app.data.model.FridgeScanResponse> = withContext(Dispatchers.IO) {
        try {
            val userContext = buildUserContext()
            val prompt = """
                Identify all visible food items in this fridge photo.
                Then suggest 3 realistic recipes the user can make using these items + common pantry staples
                (oil, salt, pepper, flour, basic spices). Match USER CONTEXT.

                USER CONTEXT:
                $userContext

                Return ONLY raw JSON:
                {
                  "detectedItems": ["string"],
                  "suggestions": [
                    {
                      "name": "string",
                      "whyGoodForUser": "1 sentence respecting USER CONTEXT",
                      "ingredientsAvailable": ["string"],
                      "ingredientsMissing": ["string"],
                      "estimatedKcalPerServing": int,
                      "briefSteps": "2-3 sentences"
                    }
                  ],
                  "summary": "1 sentence"
                }
            """.trimIndent()
            val request = visionRequest(prompt, encodeImageToBase64(bitmap))
            val response = withRetry { groqApiService.createChatCompletion(authHeader(), request) }
            val txt = response.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response")
            parseJson(txt, com.kkaloai.app.data.model.FridgeScanResponse::class.java)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun generateWeeklyMealPlan(): Result<com.kkaloai.app.data.model.WeeklyMealPlanResponse> = withContext(Dispatchers.IO) {
        try {
            val userContext = buildUserContext()
            val calorieGoal = userPreferences.calorieGoal.first()
            val prompt = """
                Build a realistic 7-day meal plan with 3-4 meals per day for the user.
                Total daily calories should be near $calorieGoal kcal (±150 kcal).
                Respect USER CONTEXT (goal, GLP-1, PCOS).

                USER CONTEXT:
                $userContext

                Return ONLY raw JSON:
                {
                  "days": [
                    {
                      "dayLabel": "Monday",
                      "meals": [
                         {"name":"string","calories":int,"proteins":float,"carbs":float,"fats":float,"timeOfDay":"breakfast|lunch|dinner|snack"}
                      ],
                      "totalKcal": int
                    }
                  ],
                  "summary": "1-2 sentence overview"
                }
            """.trimIndent()
            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile",
                messages = listOf(GroqMessage(role = "user", content = prompt)),
                response_format = GroqResponseFormat(type = "json_object")
            )
            val response = withRetry { groqApiService.createChatCompletion(authHeader(), request) }
            val txt = response.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response")
            parseJson(txt, com.kkaloai.app.data.model.WeeklyMealPlanResponse::class.java)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun authHeader() = "Bearer ${com.kkaloai.app.BuildConfig.GROQ_API_KEY}"

    private fun visionRequest(prompt: String, base64: String) = GroqChatRequest(
        model = "meta-llama/llama-4-scout-17b-16e-instruct",
        messages = listOf(
            GroqMessage(
                role = "user",
                content = listOf(
                    GroqContentPart(type = "text", text = prompt),
                    GroqContentPart(type = "image_url", image_url = GroqImageUrl(url = "data:image/jpeg;base64,$base64"))
                )
            )
        ),
        response_format = GroqResponseFormat(type = "json_object")
    )

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        val delaysMs = longArrayOf(200L, 800L, 2_000L)
        var lastError: Throwable? = null
        for (attempt in 0..delaysMs.size) {
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                val msg = (e.message ?: "").lowercase()
                val transient = msg.contains("429") ||
                    msg.contains("rate") && msg.contains("limit") ||
                    msg.contains("503") ||
                    msg.contains("unavailable") ||
                    msg.contains("resource_exhausted")
                if (!transient || attempt == delaysMs.size) throw e
                delay(delaysMs[attempt])
            }
        }
        throw lastError ?: IllegalStateException("withRetry exited unexpectedly")
    }

    private fun <T : Any> parseJson(raw: String?, clazz: Class<T>): Result<T> {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() }
            ?: return Result.failure(IllegalStateException("AI returned empty response"))
        val unwrapped = trimmed
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = unwrapped.indexOf('{')
        val end = unwrapped.lastIndexOf('}')
        if (start < 0 || end <= start) {
            return Result.failure(IllegalStateException("AI response contained no JSON object"))
        }
        val jsonString = unwrapped.substring(start, end + 1)
        return try {
            val parsed = gson.fromJson(jsonString, clazz)
                ?: return Result.failure(IllegalStateException("AI response parsed to null"))
            Result.success(parsed)
        } catch (e: JsonSyntaxException) {
            Result.failure(e)
        }
    }

    suspend fun buildPlannerSystemMessage(): GroqMessage {
        val userContext = buildUserContext()
        val systemText = """
            You are KkaloAI Planner. Help the user plan their daily nutrition.

            USER CONTEXT:
            $userContext

            Chat with them to find out what they want. Once they agree to a specific meal plan, you MUST append a JSON block at the very end of your message with this EXACT format wrapped in ```json ... ```:
            ```json
            {"planned_meals": [{"name": "Chicken Salad", "calories": 400, "proteins": 30, "carbs": 10, "fats": 15}]}
            ```
            Always keep your tone encouraging, non-judgy, and health-focused. Respect the USER CONTEXT above.
        """.trimIndent()
        return GroqMessage(role = "system", content = systemText)
    }

    suspend fun sendPlannerMessage(history: List<GroqMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile",
                messages = history,
                temperature = 0.7
            )
            val response = withRetry {
                groqApiService.createChatCompletion(
                    "Bearer ${com.kkaloai.app.BuildConfig.GROQ_API_KEY}",
                    request
                )
            }
            val text = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty response")
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
