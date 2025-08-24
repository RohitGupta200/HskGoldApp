package org.cap.gold.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String = "",
    val price: Double,
    val imageUrl: String = "https://via.placeholder.com/150", // Default placeholder image
    val category: String = "",
    val description: String = "",
    val weight: Double = 0.0,
    val purity: String = "",
    val dimension: String = "",
    val maxQuantity: Int = 1
)
