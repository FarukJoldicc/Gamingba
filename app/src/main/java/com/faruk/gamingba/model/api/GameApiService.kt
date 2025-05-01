package com.faruk.gamingba.model.api

import com.faruk.gamingba.model.data.Game
import com.faruk.gamingba.model.data.GamesResponse
import com.faruk.gamingba.model.data.ScreenshotsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GameApiService {
    @GET("games")
    suspend fun getGames(
        @Query("key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
        @Query("ordering") ordering: String = "-rating"
    ): GamesResponse

    @GET("games/{id}")
    suspend fun getGameDetails(
        @Path("id") gameId: Int,
        @Query("key") apiKey: String
    ): Game

    @GET("games/{game_pk}/screenshots")
    suspend fun getGameScreenshots(
        @Path("game_pk") gamePk: Int,
        @Query("key") apiKey: String
    ): ScreenshotsResponse
} 