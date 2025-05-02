package com.faruk.gamingba.model.repository

import android.util.Log
import com.faruk.gamingba.model.api.GameApiService
import com.faruk.gamingba.model.data.Game
import com.faruk.gamingba.model.data.Screenshot
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GameRepository"

@Singleton
class GameRepository @Inject constructor(
    private val apiService: GameApiService
) {
    private val apiKey = "166815e958f742e08d79303565ed807f"
    
    suspend fun getGames(page: Int = 1, pageSize: Int = 10, ordering: String = "-rating", fetchMultiplePages: Boolean = false): Result<List<Game>> {
        return try {
            Log.d(TAG, "getGames() called with page=$page, pageSize=$pageSize, ordering=$ordering, fetchMultiplePages=$fetchMultiplePages")
            
            // First page fetch
            val response = apiService.getGames(apiKey, page, pageSize, ordering)
            var allGames = response.results.toMutableList()
            
            // If fetchMultiplePages is true, fetch additional pages
            if (fetchMultiplePages) {
                var currentPage = page
                var nextPageUrl = response.nextPage
                val totalPagesToFetch = 6 // Reduced from 20 to 6 for faster loading
                
                // Continue fetching while we have more pages and haven't reached our limit
                for (i in 1 until totalPagesToFetch) {
                    if (nextPageUrl == null) {
                        Log.d(TAG, "No more pages available after page $currentPage")
                        break
                    }
                    
                    currentPage++
                    Log.d(TAG, "Fetching additional page: $currentPage")
                    
                    try {
                        val nextPageResponse = apiService.getGames(apiKey, currentPage, pageSize, ordering)
                        allGames.addAll(nextPageResponse.results)
                        nextPageUrl = nextPageResponse.nextPage
                        
                        Log.d(TAG, "Fetched page $currentPage, now have ${allGames.size} games total")
                        
                        // After fetching each page, log some sample years to verify we're getting a good distribution
                        val sampleYears = nextPageResponse.results.take(5).mapNotNull { game ->
                            if (game.releaseDate?.isNotEmpty() == true && game.releaseDate.length >= 4) {
                                game.releaseDate.substring(0, 4)
                            } else null
                        }
                        
                        if (sampleYears.isNotEmpty()) {
                            Log.d(TAG, "Sample years from page $currentPage: ${sampleYears.joinToString(", ")}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching page $currentPage: ${e.message}", e)
                        // Continue with what we have
                        break
                    }
                }
                
                Log.d(TAG, "Finished multi-page fetch with ${allGames.size} games total")
                
                // Analysis: Count number of games by year to verify distribution
                val yearGroups = allGames.groupBy { game ->
                    if (game.releaseDate?.isNotEmpty() == true && game.releaseDate.length >= 4) {
                        game.releaseDate.substring(0, 4)
                    } else "unknown"
                }
                
                Log.d(TAG, "Year distribution in fetched data:")
                yearGroups.forEach { (year, games) ->
                    Log.d(TAG, "Year $year: ${games.size} games")
                }
                
                return Result.success(allGames)
            } else {
                // Original single-page with potential small extension logic
                if (response.results.size < pageSize) {
                    Log.d(TAG, "Not enough games returned (${response.results.size}), fetching more...")
                    val nextPage = response.nextPage
                    if (nextPage != null) {
                        try {
                            val page2Response = apiService.getGames(apiKey, page + 1, pageSize - response.results.size, ordering)
                            val combinedResults = response.results + page2Response.results
                            Log.d(TAG, "Combined games: ${combinedResults.size}")
                            return Result.success(combinedResults.take(pageSize))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching additional games: ${e.message}", e)
                            // Continue with what we have
                        }
                    }
                }
                
                Log.d(TAG, "getGames() success: received ${response.results.size} games")
                return Result.success(response.results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getGames() error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getGameDetails(gameId: Int): Result<Game> {
        return try {
            Log.d(TAG, "getGameDetails() called with gameId=$gameId")
            val game = apiService.getGameDetails(gameId, apiKey)
            Log.d(TAG, "getGameDetails() success: received game ${game.name}")
            Result.success(game)
        } catch (e: Exception) {
            Log.e(TAG, "getGameDetails() error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getGameScreenshots(gameId: Int): Result<List<Screenshot>> {
        return try {
            Log.d(TAG, "getGameScreenshots() called with gameId=$gameId")
            val response = apiService.getGameScreenshots(gameId, apiKey)
            Log.d(TAG, "getGameScreenshots() success: received ${response.results.size} screenshots")
            Result.success(response.results)
        } catch (e: Exception) {
            Log.e(TAG, "getGameScreenshots() error: ${e.message}", e)
            Result.failure(e)
        }
    }
} 