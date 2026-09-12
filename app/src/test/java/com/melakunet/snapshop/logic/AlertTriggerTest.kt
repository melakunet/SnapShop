package com.melakunet.snapshop.logic

import org.junit.Test
import org.junit.Assert.*

class AlertTriggerTest {

    private fun shouldTrigger(currentPrice: Double, targetPrice: Double): Boolean {
        return currentPrice <= targetPrice
    }

    @Test
    fun testAlertTriggerCondition() {
        assertTrue(shouldTrigger(9.99, 10.0))
        assertTrue(shouldTrigger(10.0, 10.0))
        assertFalse(shouldTrigger(10.01, 10.0))
    }
}
