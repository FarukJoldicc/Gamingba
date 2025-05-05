package com.faruk.gamingba.model.state

import com.faruk.gamingba.model.data.Game
import com.faruk.gamingba.model.data.Screenshot

data class GameCarouselState(
    val isLoading: Boolean = false,
    val games: List<Game> = emptyList(),
    val error: String? = null
)

data class GameDetailState(
    val isLoading: Boolean = false,
    val game: Game? = null,
    val screenshots: List<Screenshot> = emptyList(),
    val error: String? = null
) 