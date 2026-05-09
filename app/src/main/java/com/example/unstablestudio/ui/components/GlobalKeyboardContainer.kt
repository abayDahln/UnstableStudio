package com.example.unstablestudio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun GlobalKeyboardContainer(
    content: @Composable () -> Unit
) {
    val keyboardManager = remember { KeyboardManager() }
    val isKeyboardVisible by keyboardManager.isVisible.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Hide keyboard when activity/container is paused or disposed
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                keyboardManager.hide()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            keyboardManager.hide()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalKeyboardManager provides keyboardManager) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            AnimatedVisibility(
                visible = isKeyboardVisible,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                CodingKeyboard(
                    onKeyPress = { text, ctrl, alt ->
                        // In a real system, we'd handle ctrl/alt combinations here or pass them down
                        // For now, if ctrl is pressed, we might want to emit a special shortcut string
                        if (ctrl) {
                            keyboardManager.emitTextInput("CTRL+$text")
                        } else {
                            keyboardManager.emitTextInput(text)
                        }
                    },
                    onSpecialKey = { key ->
                        keyboardManager.emitSpecialKey(key)
                    },
                    onClose = {
                        keyboardManager.hide()
                    }
                )
            }
        }
    }
}
