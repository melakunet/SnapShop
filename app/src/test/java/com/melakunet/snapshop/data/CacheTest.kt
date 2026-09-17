package com.melakunet.snapshop.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheTest {
    
    private fun normalizeQuery(query: String): String {
        return query.lowercase().trim().replace("\\s+".toRegex(), " ")
    }

    @Test
    fun testQueryNormalization() {
        val cases = mapOf(
            "  Logitech Mouse  " to "logitech mouse",
            "logitech   mouse" to "logitech mouse",
            "LOGITECH MOUSE" to "logitech mouse",
            "logitech mouse" to "logitech mouse"
        )
        
        cases.forEach { (input, expected) ->
            assertEquals(expected, normalizeQuery(input))
        }
    }
}
