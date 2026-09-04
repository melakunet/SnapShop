package com.melakunet.snapshop.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Field-for-field mirror of iOS Models/*.swift.
// Backend JSON is snake_case; @SerialName maps to camelCase.

@Serializable
data class IdentifyResult(
    val brand: String,
    val model: String,
    val category: String,
    @SerialName("distinguishing_features") val distinguishingFeatures: List<String> = emptyList(),
    val confidence: Double,
    @SerialName("search_query") val searchQuery: String,
    @SerialName("image_url") val imageURL: String? = null,
    val plant: PlantResult? = null,
    @SerialName("other_items") val otherItems: List<OtherItem>? = null,
)

@Serializable
data class OtherItem(
    val brand: String,
    val model: String,
    val category: String,
    val confidence: Double,
    @SerialName("search_query") val searchQuery: String,
    @SerialName("frame_index") val frameIndex: Int? = null,
) {
    val displayName: String
        get() {
            val parts = listOf(brand, model).filter { it.isNotEmpty() }
            return if (parts.isEmpty()) category.replaceFirstChar { it.uppercase() }
            else parts.joinToString(" ")
        }
}

@Serializable
data class ShopItem(
    val price: String,
    @SerialName("extracted_price") val extractedPrice: Double,
    val delivery: String,
    val source: String,
    val link: String,
    val thumbnail: String,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int? = null,
    val title: String? = null,
    val snippet: String? = null,
    @SerialName("product_id") val productId: String? = null,
)

@Serializable
data class PlantWarning(
    val level: String,
    val note: String,
)

@Serializable
data class PlantResult(
    @SerialName("common_name") val commonName: String,
    @SerialName("latin_name") val latinName: String,
    val confidence: Double,
    @SerialName("features_observed") val featuresObserved: List<String> = emptyList(),
    @SerialName("hazard_signals") val hazardSignals: List<String> = emptyList(),
    val warning: PlantWarning? = null,
    @SerialName("safety_note") val safetyNote: String? = null,
)

@Serializable
data class ReviewItem(
    val author: String? = null,
    val rating: Double? = null,
    val text: String,
    val date: String? = null,
)

@Serializable
data class RatingBreakdown(
    val five: Int,
    val four: Int,
    val three: Int,
    val two: Int,
    val one: Int,
) {
    val total: Int get() = five + four + three + two + one
    val counts: List<Pair<Int, Int>>
        get() = listOf(5 to five, 4 to four, 3 to three, 2 to two, 1 to one)
}

@Serializable
data class ProductReviews(
    val rating: Double,
    @SerialName("review_count") val reviewCount: Int,
    val breakdown: RatingBreakdown? = null,
    @SerialName("top_reviews") val topReviews: List<ReviewItem> = emptyList(),
)

@Serializable
data class TranscribeResponse(val transcript: String)
