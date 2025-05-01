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
    
    suspend fun getGames(page: Int = 1, pageSize: Int = 10, ordering: String = "-rating"): Result<List<Game>> {
        return try {
            Log.d(TAG, "getGames() called with page=$page, pageSize=$pageSize, ordering=$ordering")
            val response = apiService.getGames(apiKey, page, pageSize, ordering)
            
            // Ensure we have enough games
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
            Result.success(response.results)
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