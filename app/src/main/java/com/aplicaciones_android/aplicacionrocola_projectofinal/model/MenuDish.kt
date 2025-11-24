package com.aplicaciones_android.aplicacionrocola_projectofinal.model

data class MenuDish(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val imagePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

