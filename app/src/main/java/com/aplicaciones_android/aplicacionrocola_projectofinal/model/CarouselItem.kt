package com.aplicaciones_android.aplicacionrocola_projectofinal.model

data class CarouselItem(
    val id: String,
    val title: String,
    val imageResId: Int,
    var bandName: String? = null
)
