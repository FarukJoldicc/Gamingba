package com.faruk.gamingba.model.data

import com.google.gson.annotations.SerializedName

data class Screenshot(
    @SerializedName("id") val id: Int,
    @SerializedName("image") val imageUrl: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
) 