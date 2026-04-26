package com.kkaloai.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val TAG = "KkaloAiDebug"
    private var logFile: File? = null

    fun init(context: Context) {
        try {
            val docsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            docsFolder?.mkdirs()
            logFile = File(docsFolder, "KkaloAi_Logs.txt")
            if (logFile?.exists() == false) {
                logFile?.createNewFile()
            }
            d("FileLogger", "Logger initialized. Absolute path: ${logFile?.absolutePath}")
            
            // Set up global exception handler
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                e("CRASH", "Uncaught exception on thread ${thread.name}: ${exception.message}", exception)
                // Write stacktrace to log
                exception.stackTraceToString().lines().forEach {
                    e("CRASH_TRACE", it)
                }
                
                // Important: let the app crash normally after logging so the OS cleans up or sends standard reports
                defaultHandler?.uncaughtException(thread, exception)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FileLogger", e)
        }
    }

    fun d(tag: String, message: String) {
        val fullMessage = "DEBUG: $tag - $message"
        Log.d(TAG, fullMessage)
        appendLog(fullMessage)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = "ERROR: $tag - $message ${throwable?.message ?: ""}"
        Log.e(TAG, fullMessage, throwable)
        appendLog(fullMessage)
        throwable?.let {
            appendLog(it.stackTraceToString())
        }
    }

    private fun appendLog(text: String) {
        try {
            logFile?.let { file ->
                val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val logEntry = "$timeStamp: $text\n"
                
                FileOutputStream(file, true).use { fos ->
                    OutputStreamWriter(fos, "UTF-8").use { writer ->
                        writer.append(logEntry)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
}
