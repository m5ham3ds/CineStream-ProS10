package com.example.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.models.Movie
import com.example.domain.repository.MediaRepository
import com.example.utils.MediaDetailsCacheManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieDetailsUiState(
    val isLoading: Boolean = false,
    val movie: Movie? = null,
    val similarMovies: List<Movie> = emptyList(),
    val error: String? = null
)

class MovieDetailsViewModel(
    private val repository: MediaRepository
) : ViewModel() {
    companion object {
        private val cache = java.util.concurrent.ConcurrentHashMap<String, MovieDetailsUiState>()
        fun getCachedState(movieId: String, experienceMode: String = "STANDARD"): MovieDetailsUiState? {
            val key = "${movieId}_$experienceMode"
            val direct = cache[key]
                ?: try { cache["${java.net.URLDecoder.decode(movieId, "UTF-8")}_$experienceMode"] } catch (_: Exception) { null }
                ?: try { cache["${java.net.URLEncoder.encode(movieId, "UTF-8")}_$experienceMode"] } catch (_: Exception) { null }
            if (direct != null) return direct

            // Never fall back to unpartitioned/standard cache when in dubbed experience
            if (experienceMode.equals("ARABIC_DUBBED", ignoreCase = true) || experienceMode.equals("DUBBED", ignoreCase = true)) {
                return null
            }

            return cache[movieId]
                ?: try { cache[java.net.URLDecoder.decode(movieId, "UTF-8")] } catch (_: Exception) { null }
                ?: try { cache[java.net.URLEncoder.encode(movieId, "UTF-8")] } catch (_: Exception) { null }
        }

        fun saveState(movieId: String, state: MovieDetailsUiState, experienceMode: String = "STANDARD") {
            val key = "${movieId}_$experienceMode"
            cache[key] = state
            try { cache["${java.net.URLEncoder.encode(movieId, "UTF-8")}_$experienceMode"] = state } catch (_: Exception) {}
            try { cache["${java.net.URLDecoder.decode(movieId, "UTF-8")}_$experienceMode"] = state } catch (_: Exception) {}
            if (!experienceMode.equals("ARABIC_DUBBED", ignoreCase = true) && !experienceMode.equals("DUBBED", ignoreCase = true)) {
                cache.putIfAbsent(movieId, state)
            }
        }
    }

    private val _uiState = MutableStateFlow(MovieDetailsUiState())
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()
    private var networkObserverJob: Job? = null

    fun loadMovie(movieId: String, forceRefresh: Boolean = false, experienceMode: String = "STANDARD") {
        val cached = getCachedState(movieId, experienceMode)
        if (cached != null && !forceRefresh) {
            _uiState.value = cached
        }

        val isDubbedExp = experienceMode.equals("ARABIC_DUBBED", ignoreCase = true) || experienceMode.equals("DUBBED", ignoreCase = true)
        val targetVariantMode = if (isDubbedExp) "dubbed" else "subtitled"

        val app = try { AppContainer.application } catch (_: Exception) { null }
        val cachedMovie = if (app != null) {
            MediaDetailsCacheManager.getMovieDetails(app, movieId, variantMode = targetVariantMode)
                ?: try { MediaDetailsCacheManager.getMovieDetails(app, java.net.URLDecoder.decode(movieId, "UTF-8"), variantMode = targetVariantMode) } catch (_: Exception) { null }
                ?: try { MediaDetailsCacheManager.getMovieDetails(app, java.net.URLEncoder.encode(movieId, "UTF-8"), variantMode = targetVariantMode) } catch (_: Exception) { null }
        } else null
        if (cachedMovie != null && _uiState.value.movie == null) {
            _uiState.update { it.copy(movie = cachedMovie, isLoading = false, error = null) }
        }

        viewModelScope.launch {
            if (_uiState.value.movie == null) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val movie = repository.getMovieById(movieId)
                if (movie != null) {
                    val currentMovie = _uiState.value.movie
                    val mergedMovie = if (currentMovie != null && (currentMovie.id == movie.id || currentMovie.id.contains(movie.id) || movie.id.contains(currentMovie.id))) {
                        movie.copy(
                            sourceUrl = if (!currentMovie.sourceUrl.isNullOrBlank()) currentMovie.sourceUrl else movie.sourceUrl,
                            providerId = currentMovie.providerId ?: movie.providerId,
                            mode = if (isDubbedExp || currentMovie.mode == "dubbed") "dubbed" else (currentMovie.mode ?: movie.mode),
                            sourceCandidates = (currentMovie.sourceCandidates + movie.sourceCandidates).distinctBy { "${it.providerId}_${it.sourcePageUrl}" }
                        )
                    } else if (isDubbedExp) {
                        movie.copy(mode = "dubbed")
                    } else {
                        movie
                    }
                    if (app != null) {
                        MediaDetailsCacheManager.saveMovieDetails(app, mergedMovie, variantMode = targetVariantMode)
                    }
                    val newState = _uiState.value.copy(movie = mergedMovie, isLoading = false, error = null)
                    _uiState.value = newState
                    saveState(movieId, newState, experienceMode)
                    networkObserverJob?.cancel()
                } else if (_uiState.value.movie == null) {
                    val fallback = if (app != null) MediaDetailsCacheManager.getMovieDetails(app, movieId, variantMode = targetVariantMode) else null
                    if (fallback != null) {
                        val newState = _uiState.value.copy(movie = fallback, isLoading = false, error = null)
                        _uiState.value = newState
                        saveState(movieId, newState, experienceMode)
                        networkObserverJob?.cancel()
                    } else {
                        // Keep loading skeleton active and wait for internet connection
                        _uiState.update { it.copy(isLoading = true, error = null) }
                        startNetworkAutoReload(movieId)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                if (_uiState.value.movie == null) {
                    val fallback = if (app != null) MediaDetailsCacheManager.getMovieDetails(app, movieId) else null
                    if (fallback != null) {
                        val newState = _uiState.value.copy(movie = fallback, isLoading = false, error = null)
                        _uiState.value = newState
                        saveState(movieId, newState, experienceMode)
                        networkObserverJob?.cancel()
                    } else {
                        // Keep loading skeleton active and wait for internet connection
                        _uiState.update { it.copy(isLoading = true, error = null) }
                        startNetworkAutoReload(movieId)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun startNetworkAutoReload(movieId: String) {
        if (networkObserverJob?.isActive == true) return
        val appContext = try { com.example.MyApplication.appContext } catch (_: Throwable) { null } ?: return
        networkObserverJob = viewModelScope.launch {
            val observer = com.example.utils.NetworkConnectivityObserver(appContext)
            observer.observe().collect { isConnected ->
                if (isConnected && _uiState.value.movie == null) {
                    delay(500)
                    loadMovie(movieId, forceRefresh = true)
                }
            }
        }
    }
}