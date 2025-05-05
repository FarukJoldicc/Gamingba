package com.faruk.gamingba.model.data

import com.google.gson.annotations.SerializedName

data class Game(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("background_image") val backgroundImage: String?,
    @SerializedName("rating") val rating: Float = 0f,
    @SerializedName("released") val releaseDate: String? = null,
    @SerializedName("metacritic") val metaCritic: Int? = null,
    @SerializedName("genres") val genres: List<Genre> = emptyList(),
    @SerializedName("publishers") val publishers: List<Publisher> = emptyList(),
    @SerializedName("esrb_rating") val esrbRating: EsrbRating? = null
)

data class Genre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
)

data class Publisher(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
)

data class EsrbRating(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
) 