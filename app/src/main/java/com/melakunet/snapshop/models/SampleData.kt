package com.melakunet.snapshop.models

// Sample data so every screen renders before Room DB lands (Day 7).
// Deleted when real persistence is wired in.

data class SampleScan(
    val productName: String,
    val mode: String,        // "Precision" | "Deep"
    val date: String,
    val lowestPrice: Double,
)

data class SampleSaved(
    val productName: String,
    val savedPrice: Double,
    val currentPrice: Double,
    val source: String,
    val savedDate: String,
)

data class SampleAlert(
    val productName: String,
    val targetPrice: Double,
    val currentPrice: Double,
    val triggered: Boolean,
)

object SampleData {
    val scans = listOf(
        SampleScan("Sony WH-1000XM5", "Precision", "Today, 10:12 AM", 329.99),
        SampleScan("Stanley Quencher 40oz", "Precision", "Today, 9:48 AM", 45.00),
        SampleScan("Nintendo Switch OLED", "Deep", "Yesterday", 349.99),
        SampleScan("Monstera Deliciosa", "Precision", "Yesterday", 24.99),
        SampleScan("KitchenAid Stand Mixer", "Deep", "Sep 2", 279.95),
        SampleScan("Air Jordan 1 Mid", "Precision", "Sep 1", 125.00),
    )

    val saved = listOf(
        SampleSaved("Sony WH-1000XM5", 349.99, 329.99, "Best Buy", "Sep 2"),
        SampleSaved("KitchenAid Stand Mixer", 279.95, 279.95, "Amazon.com", "Sep 2"),
        SampleSaved("Nintendo Switch OLED", 349.99, 339.00, "Walmart", "Aug 30"),
    )

    val alerts = listOf(
        SampleAlert("Sony WH-1000XM5", 299.99, 329.99, false),
        SampleAlert("Nintendo Switch OLED", 340.00, 339.00, true),
    )

    val retailers = listOf(
        "Amazon.com", "Walmart", "Best Buy", "Target", "eBay", "Home Depot", "B&H",
    )
}
