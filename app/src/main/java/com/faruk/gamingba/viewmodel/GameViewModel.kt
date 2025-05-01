package com.faruk.gamingba.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faruk.gamingba.model.repository.GameRepository
import com.faruk.gamingba.model.state.GameCarouselState
import com.faruk.gamingba.model.state.GameDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GameViewModel"
private const val MAX_CAROUSEL_GAMES = 10

@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _carouselState = MutableStateFlow(GameCarouselState())
    val carouselState: StateFlow<GameCarouselState> = _carouselState.asStateFlow()

    private val _gameDetailState = MutableStateFlow(GameDetailState())
    val gameDetailState: StateFlow<GameDetailState> = _gameDetailState.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    init {
        Log.d(TAG, "GameViewModel initialized, loading games...")
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            Log.d(TAG, "loadGames() called, setting isLoading = true")
            _carouselState.value = GameCarouselState(isLoading = true)
            try {
                Log.d(TAG, "Calling repository.getGames(ordering = \"-rating\")")
                repository.getGames(pageSize = MAX_CAROUSEL_GAMES, ordering = "-rating").fold(
                    onSuccess = { games ->
                        Log.d(TAG, "Games loaded successfully: ${games.size} games")
                        // Take at most MAX_CAROUSEL_GAMES
                        val limitedGames = games.take(MAX_CAROUSEL_GAMES)
                        _carouselState.value = GameCarouselState(games = limitedGames)
                        
                        // Set initial position
                        _currentPosition.value = 0
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading games: ${error.message}", error)
                        _carouselState.value = GameCarouselState(error = error.message)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadGames(): ${e.message}", e)
                _carouselState.value = GameCarouselState(error = e.message)
            }
        }
    }

    fun setCurrentPosition(position: Int) {
        val gameCount = _carouselState.value.games.size
        if (gameCount > 0) {
            _currentPosition.value = position.coerceIn(0, gameCount - 1)
            Log.d(TAG, "Current position set to: ${_currentPosition.value}")
        }
    }

    fun loadGameDetails(gameId: Int) {
        viewModelScope.launch {
            Log.d(TAG, "loadGameDetails() called for gameId: $gameId")
            _gameDetailState.value = GameDetailState(isLoading = true)
            try {
                val gameResult = repository.getGameDetails(gameId)
                val screenshotsResult = repository.getGameScreenshots(gameId)
                
                val game = gameResult.getOrNull()
                val screenshots = screenshotsResult.getOrNull() ?: emptyList()
                
                if (game != null) {
                    Log.d(TAG, "Game details loaded successfully for: ${game.name}")
                    _gameDetailState.value = GameDetailState(
                        game = game,
                        screenshots = screenshots
                    )
                } else {
                    Log.e(TAG, "Game details not found for gameId: $gameId")
                    _gameDetailState.value = GameDetailState(
                        error = gameResult.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadGameDetails(): ${e.message}", e)
                _gameDetailState.value = GameDetailState(error = e.message)
            }
        }
    }

    fun resetGameDetailState() {
        _gameDetailState.value = GameDetailState()
    }
} 