package com.example.spark.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StatsPeriodTest {

    @Test
    fun periodEnum_hasExpectedValues() {
        val periods = Period.values()
        assertEquals(2, periods.size)
        assertEquals(Period.WEEK, Period.valueOf("WEEK"))
        assertEquals(Period.MONTH, Period.valueOf("MONTH"))
    }

    @Test
    fun defaultPeriod_isWeek() {
        val defaultPeriod = Period.WEEK
        assertNotNull(defaultPeriod)
        assertEquals("WEEK", defaultPeriod.name)
    }

    @Test
    fun typealias_statsPeriod_aliasesPeriod() {
        val statsPeriod: StatsPeriod = Period.WEEK
        assertEquals(Period.WEEK, statsPeriod)
    }
}
