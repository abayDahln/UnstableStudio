package com.example.unstablestudio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File

class TerminalViewModelFactory(private val runtimeDir: File) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TerminalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TerminalViewModel(runtimeDir) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
