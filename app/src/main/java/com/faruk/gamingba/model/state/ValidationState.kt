package com.faruk.gamingba.model.state

data class ValidationState(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) 