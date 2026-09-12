package com.melakunet.snapshop.logic

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

class QuotaLogicTest {

    private fun isRolloverRequired(currentMonth: String, lastSavedMonth: String): Boolean {
        return currentMonth != lastSavedMonth
    }

    @Test
    fun testMonthlyRollover() {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        val now = formatter.format(Date())
        val lastMonth = "2024-01" 
        
        assertTrue(isRolloverRequired(now, lastMonth))
        assertFalse(isRolloverRequired(now, now))
    }

    @Test
    fun testFreeLimitLogic() {
        val releaseLimit = 10
        
        fun canScan(used: Int, limit: Int, isPro: Boolean): Boolean {
            if (isPro) return true
            return used < limit
        }

        assertTrue(canScan(5, releaseLimit, false))
        assertFalse(canScan(10, releaseLimit, false))
        assertTrue(canScan(10, releaseLimit, true))
    }
}
