package com.example.unstablestudio.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeyboardManager {
    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    private var onTextInput: ((String) -> Unit)? = null
    private var onSpecialKey: ((SpecialKey) -> Unit)? = null

    fun show() {
        _isVisible.value = true
    }

    fun hide() {
        _isVisible.value = false
        // Clear input callbacks when hidden to prevent leaking focus state
        unregisterInput()
    }

    fun toggle() {
        if (_isVisible.value) hide() else show()
    }

    fun registerInput(
        onTextInput: (String) -> Unit,
        onSpecialKey: (SpecialKey) -> Unit
    ) {
        this.onTextInput = onTextInput
        this.onSpecialKey = onSpecialKey
    }

    fun unregisterInput() {
        this.onTextInput = null
        this.onSpecialKey = null
    }

    fun emitTextInput(text: String) {
        onTextInput?.invoke(text)
    }

    fun emitSpecialKey(key: SpecialKey) {
        onSpecialKey?.invoke(key)
    }
}

val LocalKeyboardManager = staticCompositionLocalOf<KeyboardManager> {
    error("No KeyboardManager provided")
}
