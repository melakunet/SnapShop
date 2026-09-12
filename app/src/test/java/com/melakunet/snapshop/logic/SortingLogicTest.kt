package com.melakunet.snapshop.logic

import org.junit.Test
import org.junit.Assert.*

class SortingLogicTest {

    data class MockItem(val rating: Double?, val count: Int, val price: Double)

    private fun score(item: MockItem): Double {
        if (item.rating == null) return -1.0
        val prior = 3.5
        val weight = 20.0
        return (item.rating * item.count + prior * weight) / (item.count + weight)
    }

    @Test
    fun testSortingOrder() {
        val items = listOf(
            MockItem(null, 0, 10.0),      // Unrated
            MockItem(4.0, 100, 15.0),    // Solid count
            MockItem(5.0, 1, 12.0)       // Perfect but low count
        )

        val sorted = items.sortedByDescending { score(it) }
        
        assertEquals(4.0, sorted[0].rating!!, 0.1) // Solid count wins
        assertEquals(5.0, sorted[1].rating!!, 0.1) // Low count second
        assertNull(sorted[2].rating)               // Unrated last
    }

    @Test
    fun testPriceSortingFallback() {
        val items = listOf(
            MockItem(4.5, 50, 20.0),
            MockItem(4.5, 50, 10.0)
        )
        // Scores will be identical
        val sorted = items.sortedBy { it.price }
        assertEquals(10.0, sorted[0].price, 0.1)
    }
}
