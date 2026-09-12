package com.melakunet.snapshop.logic

import org.junit.Test
import org.junit.Assert.*

class BayesianSortTest {

    private fun calculateScore(rating: Double, count: Int): Double {
        val prior = 3.5
        val weight = 20.0
        return (rating * count + prior * weight) / (count + weight)
    }

    @Test
    fun testBayesianRanking() {
        val highRatingLowCount = calculateScore(5.0, 1)    // (5*1 + 3.5*20)/21 = 3.57
        val midRatingHighCount = calculateScore(4.2, 100)  // (4.2*100 + 3.5*20)/120 = 4.08
        
        assertTrue(midRatingHighCount > highRatingLowCount)
    }

    @Test
    fun testPriorInfluence() {
        val zeroCount = calculateScore(5.0, 0)
        assertEquals(3.5, zeroCount, 0.001)
    }
}
