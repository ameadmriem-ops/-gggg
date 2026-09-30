package com.example.core

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Prevents rapid accidental double-clicks and repeated requests
 * (e.g. login, subscribe, like, upload) within a debounce threshold.
 */
@Composable
fun Modifier.clickableDebounced(
    debounceTimeMs: Long = 600L,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return this.clickable(
        enabled = enabled,
        onClick = {
            val currentTime = SystemClock.uptimeMillis()
            if (currentTime - lastClickTime >= debounceTimeMs) {
                lastClickTime = currentTime
                onClick()
            }
        }
    )
}

/**
 * Standalone click guard helper for buttons or dialog triggers.
 */
class DebounceGuard(private val thresholdMs: Long = 600L) {
    private var lastTime = 0L

    fun canExecute(): Boolean {
        val now = SystemClock.uptimeMillis()
        return if (now - lastTime >= thresholdMs) {
            lastTime = now
            true
        } else {
            false
        }
    }
}
