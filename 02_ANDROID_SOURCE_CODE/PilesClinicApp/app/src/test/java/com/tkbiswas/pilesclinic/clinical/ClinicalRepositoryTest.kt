package com.tkbiswas.pilesclinic.clinical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClinicalRepositoryTest {

    @Before
    fun resetState() {
        ClinicalRepository.currentPrescription.clear()
        ClinicalRepository.currentInvestigations.clear()
        ClinicalRepository.currentDiet.clear()
        ClinicalRepository.visitHistory.clear()
        ClinicalRepository.lastCheckup = null
        RoleSession.currentRole = UserRole.DOCTOR
    }

    @Test
    fun `reference lists are never empty (no blank-screen regressions)`() {
        assertTrue(ClinicalRepository.commonMedicines.isNotEmpty())
        assertTrue(ClinicalRepository.commonInvestigations.isNotEmpty())
        assertTrue(ClinicalRepository.dietAllowed.isNotEmpty())
        assertTrue(ClinicalRepository.dietAvoid.isNotEmpty())
    }

    @Test
    fun `addVisit inserts newest entry first`() {
        ClinicalRepository.addVisit("Check-up", "First visit", UserRole.DOCTOR)
        ClinicalRepository.addVisit("Prescription", "Second visit", UserRole.DOCTOR)

        assertEquals(2, ClinicalRepository.visitHistory.size)
        assertEquals("Prescription", ClinicalRepository.visitHistory.first().type)
        assertEquals("Check-up", ClinicalRepository.visitHistory.last().type)
    }

    @Test
    fun `role session defaults to doctor and can switch to staff`() {
        assertTrue(RoleSession.isDoctor())
        RoleSession.applyFrom("STAFF", null, null)
        assertFalse(RoleSession.isDoctor())
        RoleSession.applyFrom("DOCTOR", null, null)
        assertTrue(RoleSession.isDoctor())
    }

    @Test
    fun `role session ignores an invalid role string instead of crashing`() {
        RoleSession.applyFrom("DOCTOR", null, null)
        RoleSession.applyFrom("NOT_A_REAL_ROLE", null, null)
        assertTrue(RoleSession.isDoctor()) // unchanged, not crashed
    }
}
