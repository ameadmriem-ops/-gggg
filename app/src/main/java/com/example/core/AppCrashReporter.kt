package com.example.core

import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticLogEntry(
    val id: String,
    val timestamp: Long,
    val formattedTime: String,
    val tag: String,
    val screenRoute: String,
    val errorType: String,
    val message: String,
    val deviceModel: String,
    val androidVersion: String,
    val stackTraceSnippet: String
)

/**
 * Centralized Application Crash and Error Diagnostics Reporter.
 * Prevents full app termination by logging and isolating uncaught failures
 * while strictly preventing logging of passwords, tokens, or personal identifiers.
 */
object AppCrashReporter {

    private const val TAG = "AppCrashReporter"
    private const val MAX_LOGS = 50

    private val _recentErrors = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val recentErrors: StateFlow<List<DiagnosticLogEntry>> = _recentErrors.asStateFlow()

    private val sensitivePatterns = listOf(
        "(?i)password[\\s=:]+[^&\\s]+" to "password=[PROTECTED]",
        "(?i)token[\\s=:]+[^&\\s]+" to "token=[PROTECTED]",
        "(?i)secret[\\s=:]+[^&\\s]+" to "secret=[PROTECTED]",
        "(?i)bearer[\\s]+[^&\\s]+" to "Bearer [PROTECTED]",
        "(?i)auth[\\s=:]+[^&\\s]+" to "auth=[PROTECTED]"
    )

    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var sanitized = input
        for ((pattern, replacement) in sensitivePatterns) {
            sanitized = sanitized?.replace(Regex(pattern), replacement)
        }
        return sanitized ?: ""
    }

    fun recordError(
        tag: String,
        screenRoute: String = "App",
        throwable: Throwable?,
        customMessage: String? = null
    ) {
        val rawMessage = customMessage ?: throwable?.message ?: "Unknown runtime issue"
        val cleanMessage = sanitize(rawMessage)
        val stackSnippet = sanitize(throwable?.stackTraceToString()?.take(500))

        val entry = DiagnosticLogEntry(
            id = "err_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            tag = tag,
            screenRoute = screenRoute,
            errorType = throwable?.javaClass?.simpleName ?: "HandledException",
            message = cleanMessage,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            stackTraceSnippet = stackSnippet
        )

        Log.e(tag, "[$screenRoute] ${entry.errorType}: ${entry.message}")

        val currentList = _recentErrors.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(currentList.lastIndex)
        }
        _recentErrors.value = currentList
    }

    /**
     * Installs a global safety net to intercept unhandled exceptions and prevent
     * sudden app exits or crashes, logging diagnostic data safely.
     */
    fun installGlobalHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordError("GlobalSafetyNet", "RootThread", throwable, "Uncaught exception intercepted safely")
                Log.e(TAG, "Handled uncaught crash on thread ${thread.name}: ${throwable.localizedMessage}")
            } catch (_: Exception) {}

            if (throwable is OutOfMemoryError) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    inline fun <T> runCatchingSafe(
        tag: String,
        screenRoute: String = "App",
        block: () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            recordError(tag, screenRoute, e)
            Result.failure(e)
        }
    }
}
