package com.faruk.gamingba.model.data

import com.google.gson.annotations.SerializedName

data class GamesResponse(
    @SerializedName("results") val results: List<Game>,
    @SerializedName("count") val count: Int,
    @SerializedName("next") val nextPage: String?,
    @SerializedName("previous") val previousPage: String?
)

data class ScreenshotsResponse(
    @SerializedName("results") val results: List<Screenshot>,
    @SerializedName("count") val count: Int
) 