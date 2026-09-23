package com.ourkode.tozocontrol.ui.main

import com.ourkode.tozocontrol.data.NoiseMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun noiseModes_haveValidCommands() {
        assertEquals("1004010101", NoiseMode.ANC.hexCmd)
        assertEquals("1005010101", NoiseMode.TRANSPARENCY.hexCmd)
        assertEquals("1008010101", NoiseMode.LEISURE.hexCmd)
        assertEquals("1007010101", NoiseMode.REDUCE_WIND.hexCmd)
        assertEquals("1004010000", NoiseMode.NORMAL.hexCmd)
    }

    @Test
    fun presets_allHaveTenBands() {
        for ((presetName, gains) in PRESET_MAP) {
            assertEquals("Preset $presetName should have exactly 10 bands", 10, gains.size)
        }
    }

    @Test
    fun frequencyLabels_matchTenBands() {
        assertEquals(10, FREQ_LABELS.size)
    }
}
