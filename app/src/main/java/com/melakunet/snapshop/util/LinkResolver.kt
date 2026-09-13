package com.melakunet.snapshop.util

import com.melakunet.snapshop.models.ShopItem
import java.net.URLEncoder

object LinkResolver {
    fun resolveBestUrl(item: ShopItem, fallbackQuery: String): String {
        val originalLink = item.link
        if (originalLink.isBlank()) return originalLink
        
        // If it's a direct retailer URL (not google.com), use it as-is
        if (!originalLink.contains("google.com", ignoreCase = true)) {
            return originalLink
        }

        // It's a Google Shopping URL, build a retailer-native search URL
        val title = item.title ?: fallbackQuery
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        
        // Extract retailer prefix before the dash (e.g., "Walmart - TopShoes" -> "Walmart")
        val sourcePrefix = item.source.split("-").first().trim()

        return when {
            sourcePrefix.contains("Amazon", ignoreCase = true) -> "https://www.amazon.com/s?k=$encodedTitle"
            sourcePrefix.contains("Walmart", ignoreCase = true) -> "https://www.walmart.com/search?q=$encodedTitle"
            sourcePrefix.contains("Target", ignoreCase = true) -> "https://www.target.com/s?searchTerm=$encodedTitle"
            sourcePrefix.contains("Best Buy", ignoreCase = true) -> "https://www.bestbuy.com/site/searchpage.jsp?st=$encodedTitle"
            sourcePrefix.contains("Home Depot", ignoreCase = true) -> "https://www.homedepot.com/s/$encodedTitle"
            sourcePrefix.contains("eBay", ignoreCase = true) -> "https://www.ebay.com/sch/i.html?_nkw=$encodedTitle"
            sourcePrefix.contains("Newegg", ignoreCase = true) -> "https://www.newegg.com/p/pl?d=$encodedTitle"
            else -> originalLink
        }
    }
}
