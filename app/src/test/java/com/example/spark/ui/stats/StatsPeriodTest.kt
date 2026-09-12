package com.example.spark.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StatsPeriodTest {

    @Test
    fun periodEnum_hasExpectedValues() {
        val periods = StatsFragment.Period.values()
        assertEquals(2, periods.size)
        assertEquals(StatsFragment.Period.WEEK, StatsFragment.Period.valueOf("WEEK"))
        assertEquals(StatsFragment.Period.MONTH, StatsFragment.Period.valueOf("MONTH"))
    }

    @Test
    fun defaultPeriod_isWeek() {
        val defaultPeriod = StatsFragment.Period.WEEK
        assertNotNull(defaultPeriod)
        assertEquals("WEEK", defaultPeriod.name)
    }
}
