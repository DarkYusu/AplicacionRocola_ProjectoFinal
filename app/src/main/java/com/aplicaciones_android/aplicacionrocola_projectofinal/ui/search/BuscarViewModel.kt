package com.aplicaciones_android.aplicacionrocola_projectofinal.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.model.SongItem
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BuscarViewModel(private val repository: SearchRepository) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _state.value = _state.value.copy(isLoading = true, error = null, query = trimmed, items = emptyList())
        loadPage(trimmed, null, reset = true)
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoading || current.nextPageToken == null || current.query.isBlank()) return
        loadPage(current.query, current.nextPageToken, reset = false)
    }

    fun searchByChip(category: String) {
        val text = "música $category"
        search(text)
    }

    private fun loadPage(query: String, pageToken: String?, reset: Boolean) {
        viewModelScope.launch {
            try {
                val result = repository.search(query, pageToken)
                val combined = if (reset) result.items else _state.value.items + result.items
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = null,
                    items = combined,
                    nextPageToken = result.nextPageToken
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }

    data class SearchUiState(
        val query: String = "",
        val items: List<SongItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val nextPageToken: String? = null
    )
}

