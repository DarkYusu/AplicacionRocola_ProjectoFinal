package com.aplicaciones_android.aplicacionrocola_projectofinal.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.repository.SearchRepository

class BuscarViewModelFactory(private val repository: SearchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BuscarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BuscarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}

