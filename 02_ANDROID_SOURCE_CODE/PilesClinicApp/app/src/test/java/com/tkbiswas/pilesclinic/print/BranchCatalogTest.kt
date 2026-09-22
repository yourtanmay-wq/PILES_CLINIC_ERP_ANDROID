package com.tkbiswas.pilesclinic.print

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class BranchCatalogTest {

    @Before
    fun reset() {
        BranchSession.current = BranchCatalog.KISHANGANJ
    }

    @Test
    fun `catalog has at least the two known branches`() {
        assertEquals(2, BranchCatalog.all.size)
    }

    @Test
    fun `each branch has a distinct id`() {
        val ids = BranchCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `toggling branch cycles to a different branch`() {
        val before = BranchSession.current
        val currentIndex = BranchCatalog.all.indexOf(before)
        val next = BranchCatalog.all[(currentIndex + 1) % BranchCatalog.all.size]
        BranchSession.current = next
        assertNotEquals(before.id, BranchSession.current.id)
    }
}
