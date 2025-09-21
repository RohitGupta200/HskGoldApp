package org.cap.gold.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String = "",
    val price: Double,
    val margin: Double = 0.0,
    val multiplier: Double = 1.0,
    val imageUrl: String = "https://via.placeholder.com/150", // Default placeholder image
    val imageBase64: String? = null,
    val category: String = "",
    val description: String = "",
    val weight: String = "",
    val purity: String = "",
    val dimension: String = "",
    val maxQuantity: Int = 0,
    val customFields: String? = ""
)

@Serializable
data class ListProduct(
    val id: String,
    val name: String = "",
    val price: Double,
    val margin: Double = 0.0,
    val multiplier: Double = 1.0,
    val category: String = "",

)

