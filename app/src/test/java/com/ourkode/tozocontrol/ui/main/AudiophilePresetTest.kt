package com.ourkode.tozocontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiophilePresetTest {

    @Test
    fun testProfessionalPresetNamesStructure() {
        val expected = listOf(
            "Reference Flat",
            "Harman Target",
            "Diffuse Field",
            "Vocal Clarity",
            "Warm Punch",
            "Custom"
        )
        assertEquals("PRESET_NAMES must match the 6 curated audiophile presets", expected, PRESET_NAMES)
    }

    @Test
    fun testEveryPresetHasTenValidHardwareGains() {
        PRESET_MAP.forEach { (name, gains) ->
            assertEquals("Preset $name must contain exactly 10 frequency bands", 10, gains.size)
            gains.forEachIndexed { bandIdx, gain ->
                assertTrue(
                    "Preset $name band $bandIdx gain ($gain dB) must be within hardware register [-10.0 dB, +10.0 dB]",
                    gain in -10.0f..10.0f
                )
            }
        }
    }

    @Test
    fun testReferenceFlatIsCompletelyLinear() {
        val flat = PRESET_MAP["Reference Flat"]
        assertNotNull(flat)
        assertTrue(flat!!.all { it == 0.0f })
    }

    @Test
    fun testHarmanTargetAcousticCharacteristics() {
        val harman = PRESET_MAP["Harman Target"]
        assertNotNull(harman)
        // Sub-bass shelf boost
        assertTrue("Sub-bass (31 Hz) should have shelf boost", harman!![0] > 2.0f)
        assertTrue("Bass (62 Hz) should have shelf boost", harman[1] > 2.0f)
        // Ear gain presence (2 kHz)
        assertTrue("Ear canal gain (2 kHz) should be elevated", harman[6] >= 3.0f)
    }

    @Test
    fun testBackwardCompatibilityAliasesResolve() {
        val aliases = listOf("Flat", "Bass Boost", "Treble Boost", "Vocal Focus", "Acoustic")
        aliases.forEach { alias ->
            val gains = PRESET_MAP[alias]
            assertNotNull("Alias $alias must resolve for saved preference compatibility", gains)
            assertEquals("Alias $alias must have 10 bands", 10, gains!!.size)
        }
    }
}
