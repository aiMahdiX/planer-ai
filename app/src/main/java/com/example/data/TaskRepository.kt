package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.api.AIResponse
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val aiInsight: Flow<AiInsight?> = taskDao.getAiInsightFlow()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Int) = taskDao.deleteTaskById(id)

    suspend fun deleteAllTasks() = taskDao.deleteAllTasks()

    suspend fun saveAiInsight(insight: AiInsight) = taskDao.insertAiInsight(insight)

    /**
     * Sends current incomplete tasks to Gemini API, asks it to analyze deadlines,
     * time blocking, categories, and priority, returns sorted scores,
     * updates the tasks database, and caches the strategic insight text.
     */
    suspend fun performAiPrioritization(incompleteTasks: List<Task>): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("کلید دسترسی Gemini تنظیم نشده است. لطفاً از پنل Secrets آن را تنظیم کنید."))
        }

        if (incompleteTasks.isEmpty()) {
            return@withContext Result.failure(Exception("هیچ تسک در حال انجامی برای اولویت‌بندی وجود ندارد!"))
        }

        // 1. Build a rich prompt of current tasks
        val tasksBuilder = StringBuilder()
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        
        incompleteTasks.forEach { task ->
            tasksBuilder.append(
                """
                - شناسه: ${task.id}
                  عنوان: ${task.title}
                  توضیحات: ${task.description}
                  دسته بندی: ${task.category}
                  تاریخ ددلاین: ${sdf.format(Date(task.dueDate))}
                  اولویت فعلی: ${task.priority}
                  زمان تخمینی (دقیقه): ${task.estimatedMinutes}
                """.trimIndent()
            ).append("\n\n")
        }

        val prompt = """
            شما یک دستیار مدیریت زمان و مربی بهره‌وری حرفه‌ای فوق هوشمند هستید. 
            لیستی از تسک‌های من در ادامه آمده است. وظیفه شما این است که این کارها را به دقت بررسی کنید و منطقی‌ترین اولویت‌بندی اجرایی را اعمال کنید.
            
            فاکتورهای مهم جهت اولویت‌بندی:
            ۱. نزدیکی مهلت انجام (ددلاین)
            ۲. اهمیت و دسته‌بندی کار (مثلاً کارهای مربوط به شغل/کار یا سلامتی و مهارت مهم‌ترند)
            ۳. مدت زمان تخمینی و استراتژی مسدودسازی زمان (کارهای کوتاه‌تر و حیاتی‌تر سریع‌تر پردازش شوند)
            
            اطلاعات تسک‌ها:
            $tasksBuilder
            
            قوانین خروجی بسیار مهم:
            ۱. خروجی باید حتماً یک آبجکت معتبر JSON منطبق با فرمت زیر باشد (هیچ چیز دیگری خروج ندهید):
            {
              "overallInsight": "برنامه راهبردی، توصیه‌ها و نقل‌قول انگیزشی کوتاه امروز شما به زبان فارسی شیوا برای تشویق کاربر (حداقل ۳ الی ۴ جمله جذاب)",
              "prioritizedTasks": [
                {
                  "id": [شناسه عددی تسک],
                  "aiPriorityScore": [یک عدد بین ۰ تا ۱۰۰ به عنوان امتیاز دقیق اولویت - ۱۰۰ بالاترین],
                  "priority": "[یکی از مقادیر: HIGH, MEDIUM, LOW]",
                  "aiReasoning": "[دلیل جذاب، منطقی و بسیار خلاصه به زبان فارسی درباره اینکه چرا این کار این امتیاز اولویت را گرفته است]"
                }
              ]
            }
            
            ۲. فیلدهای "aiReasoning" و "overallInsight" باید حتما به زبان شیرین فارسی باشند.
            ۳. ساختار معتبر JSON باشد تا برنامه بتواند آن را بدون خطا پارس کند.
        """.trimIndent()

        // 2. Build direct REST request config
        val request = GenerateContentRequest(
            contents = listOf(com.example.api.Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.2f, // Low temperature for deterministic layout
                responseMimeType = "application/json"
            ),
            systemInstruction = com.example.api.Content(
                parts = listOf(Part(text = "You are an elite productivity AI. Your job is to prioritize task lists and return a perfectly structured JSON object conforming exactly to the requested scheme. Write the reasoning and insights in Persian. Do not include markdown wraps." ))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonTextRaw = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("خروجی از هوش مصنوعی دریافت نشد."))

            // Clean the output (strip ```json and ``` if they exist)
            val cleanedJson = cleanJsonString(jsonTextRaw)
            Log.d("TaskRepository", "Cleaned AI JSON response: $cleanedJson")

            // Parse response
            val adapter = RetrofitClient.moshiParser.adapter(AIResponse::class.java)
            val aiResponse = adapter.fromJson(cleanedJson)
                ?: return@withContext Result.failure(Exception("خطا در تحلیل ساختار خروجی هوش مصنوعی."))

            // 3. Update the tasks inside database with prioritization values
            val updatedTasks = incompleteTasks.mapNotNull { existingTask ->
                val aiData = aiResponse.prioritizedTasks.find { it.id == existingTask.id }
                if (aiData != null) {
                    existingTask.copy(
                        aiPriorityScore = aiData.aiPriorityScore,
                        priority = aiData.priority,
                        aiReasoning = aiData.aiReasoning
                    )
                } else {
                    null // or keep as is if not in list (unlikely)
                }
            }

            if (updatedTasks.isNotEmpty()) {
                taskDao.insertTasks(updatedTasks)
            }

            // Save the overall insight as cached
            val insightObject = AiInsight(
                id = 1,
                overallAdvice = aiResponse.overallInsight,
                lastUpdated = System.currentTimeMillis()
            )
            taskDao.insertAiInsight(insightObject)

            Result.success(aiResponse.overallInsight)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error prioritizing tasks: ", e)
            Result.failure(Exception("خطا در ارتباط یا تحلیل هوش مصنوعی: ${e.localizedMessage}"))
        }
    }

    private fun cleanJsonString(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removeSuffix("```")
            .trim()
    }
}
