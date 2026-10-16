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

    @Test
    fun buildPacket_emptyPayload_hasZeroLengthAndZeroChecksum() {
        val packet = com.ourkode.tozocontrol.data.TozoBleManager.buildPacket(byteArrayOf(0x10, 0x0C))
        val hex = packet.joinToString("") { "%02x".format(it) }
        assertEquals("100c0000", hex)
    }


    @Test
    fun buildPacket_powerOff_hasExpectedFormat() {
        val packet = com.ourkode.tozocontrol.data.TozoBleManager.buildPacket(byteArrayOf(0x10, 0x10), byteArrayOf(0x00))
        val hex = packet.joinToString("") { "%02x".format(it) }
        assertEquals("1010010000", hex)
    }

    @Test
    fun touchConfig_serializationAndParsing() {
        val config = com.ourkode.tozocontrol.data.TouchConfig(
            leftSingle = com.ourkode.tozocontrol.data.TouchAction.PLAY_PAUSE,
            leftDouble = com.ourkode.tozocontrol.data.TouchAction.PREV_TRACK,
            leftTriple = com.ourkode.tozocontrol.data.TouchAction.VOICE_ASSISTANT,
            leftLong = com.ourkode.tozocontrol.data.TouchAction.VOLUME_DOWN,
            rightSingle = com.ourkode.tozocontrol.data.TouchAction.NONE,
            rightDouble = com.ourkode.tozocontrol.data.TouchAction.NEXT_TRACK,
            rightTriple = com.ourkode.tozocontrol.data.TouchAction.ANC_SWITCH,
            rightLong = com.ourkode.tozocontrol.data.TouchAction.VOLUME_UP
        )

        val payload = byteArrayOf(
            config.leftSingle.code,
            config.leftDouble.code,
            config.leftTriple.code,
            config.leftLong.code,
            0x00,
            config.rightSingle.code,
            config.rightDouble.code,
            config.rightTriple.code,
            config.rightLong.code,
            0x00
        )
        val packet = com.ourkode.tozocontrol.data.TozoBleManager.buildPacket(byteArrayOf(0x10, 0x03), payload)
        assertEquals(14, packet.size)

        // Mock notification response: [00, 03, 0A, <payload 10 bytes>, checksum]
        val recvData = ByteArray(14)
        recvData[0] = 0x00
        recvData[1] = 0x03
        recvData[2] = 0x0A
        System.arraycopy(payload, 0, recvData, 3, 10)
        recvData[13] = packet[13] // same checksum

        val parsed = com.ourkode.tozocontrol.data.TozoBleManager.parseTouchConfig(recvData)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.PLAY_PAUSE, parsed.leftSingle)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.PREV_TRACK, parsed.leftDouble)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.VOICE_ASSISTANT, parsed.leftTriple)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.VOLUME_DOWN, parsed.leftLong)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.NONE, parsed.rightSingle)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.NEXT_TRACK, parsed.rightDouble)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.ANC_SWITCH, parsed.rightTriple)
        assertEquals(com.ourkode.tozocontrol.data.TouchAction.VOLUME_UP, parsed.rightLong)
    }
}
