package com.tkbiswas.pilesclinic.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSummaryTest {

    @Test
    fun `no failures and no errors means full success`() {
        val summary = SyncSummary(pushed = 3, pulled = 2, failed = 0, errors = emptyList())
        assertTrue(summary.isFullSuccess)
    }

    @Test
    fun `any failed row means not a full success`() {
        val summary = SyncSummary(pushed = 3, pulled = 2, failed = 1, errors = emptyList())
        assertFalse(summary.isFullSuccess)
    }
}
