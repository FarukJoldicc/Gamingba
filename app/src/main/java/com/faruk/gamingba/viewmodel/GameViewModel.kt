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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.faruk.gamingba.model.data.Game

private const val TAG = "GameViewModel"
private const val MAX_CAROUSEL_GAMES = 10
private const val MIN_RATING = 0.1f
private const val PREFERRED_MAX_YEAR = 2025
private const val PREFERRED_MIN_YEAR = 2024
private const val EXCLUDED_YEAR = 2026

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
                Log.d(TAG, "Calling repository.getGames with multi-page fetch")
                
                // Use larger page size to reduce number of API calls needed
                val pageSize = 80 
                
                repository.getGames(
                    pageSize = pageSize, 
                    ordering = "-released",
                    fetchMultiplePages = true
                ).fold(
                    onSuccess = { games ->
                        Log.d(TAG, "Games loaded successfully: ${games.size} games")
                        
                        // Function to extract year from release date string
                        fun getGameYear(game: Game): Int {
                            if (game.releaseDate.isNullOrEmpty()) return 0
                            
                            return try {
                                when {
                                    game.releaseDate.length >= 4 -> game.releaseDate.substring(0, 4).toIntOrNull() ?: 0
                                    else -> 0
                                }
                            } catch (e: Exception) {
                                0
                            }
                        }
                        
                        // First, filter out games with no background image, ensure positive rating, and exclude 2026
                        val filteredGames = games.filter { game -> 
                            val hasImage = !game.backgroundImage.isNullOrEmpty()
                            val hasPositiveRating = game.rating > 0.0f
                            val year = getGameYear(game)
                            val isValidYear = year != EXCLUDED_YEAR
                            
                            hasImage && hasPositiveRating && isValidYear
                        }
                        
                        Log.d(TAG, "${filteredGames.size} games after basic filtering")
                        
                        // Get games by year
                        val games2025 = filteredGames.filter { getGameYear(it) == PREFERRED_MAX_YEAR }.sortedByDescending { it.rating }
                        val games2024 = filteredGames.filter { getGameYear(it) == PREFERRED_MIN_YEAR }.sortedByDescending { it.rating }
                        val otherYearGames = filteredGames.filter { 
                            val year = getGameYear(it)
                            year != PREFERRED_MAX_YEAR && 
                            year != PREFERRED_MIN_YEAR && 
                            year != EXCLUDED_YEAR &&
                            year > 0
                        }.sortedByDescending { getGameYear(it) * 100 + it.rating }
                        
                        Log.d(TAG, "Found ${games2025.size} games from 2025")
                        Log.d(TAG, "Found ${games2024.size} games from 2024")
                        Log.d(TAG, "Found ${otherYearGames.size} games from other valid years")
                        
                        // Combine the games with the right priority to fill the carousel
                        val selectedGames = mutableListOf<Game>()
                        
                        // Add 2025 games first
                        selectedGames.addAll(games2025)
                        Log.d(TAG, "Added ${games2025.size} games from 2025")
                        
                        // Add 2024 games if needed
                        if (selectedGames.size < MAX_CAROUSEL_GAMES) {
                            val needed = MAX_CAROUSEL_GAMES - selectedGames.size
                            val toAdd = games2024.take(needed)
                            selectedGames.addAll(toAdd)
                            Log.d(TAG, "Added ${toAdd.size} games from 2024, now have ${selectedGames.size} games")
                        }
                        
                        // Add other year games if still needed
                        if (selectedGames.size < MAX_CAROUSEL_GAMES) {
                            val needed = MAX_CAROUSEL_GAMES - selectedGames.size
                            val toAdd = otherYearGames.take(needed)
                            selectedGames.addAll(toAdd)
                            Log.d(TAG, "Added ${toAdd.size} games from other years, now have ${selectedGames.size} games")
                        }
                        
                        // No longer adding duplicates, just use what we have
                        Log.d(TAG, "Using ${selectedGames.size} unique games without duplicates")
                        
                        // Final sort and prepare for display
                        val finalGames = selectedGames
                            .sortedWith(
                                compareByDescending<Game> { getGameYear(it) } // First by year
                                .thenByDescending { it.rating }              // Then by rating
                            )
                        
                        Log.d(TAG, "Final selected games (${finalGames.size}):")
                        
                        // Log distribution by year
                        val yearDistribution = finalGames.groupBy { getGameYear(it) }
                        yearDistribution.forEach { (year, yearGames) ->
                            Log.d(TAG, "Year $year: ${yearGames.size} games")
                        }
                        
                        // Log individual games
                        finalGames.forEachIndexed { index, game ->
                            val year = getGameYear(game)
                            Log.d(TAG, "Game $index: ${game.name} - Year: $year, Released: ${game.releaseDate}, Rating: ${game.rating}")
                        }
                        
                        if (finalGames.isEmpty()) {
                            _carouselState.value = GameCarouselState(
                                error = "No games found matching your criteria. Please check your filters."
                            )
                        } else {
                            // Display available games without duplicates
                            Log.d(TAG, "Setting carousel with ${finalGames.size} games")
                            _carouselState.value = GameCarouselState(games = finalGames)
                            _currentPosition.value = 0
                        }
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