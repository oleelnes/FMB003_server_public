package common_utils

import CRC16IBM
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class CRC16IBMTest {
    val avlString = "000000000000004A8E010000016B412CEE00010000" +
            "00000000000000000000000000000100050001000101000100110" +
            "01D00010010015E2C880002000B000000003544C87A000E00000000" +
            "1DD7E06A00000100002994"

    @Test
    fun generateTest() {
        val region = avlString.substring(16, avlString.length - 8)
        val crc16 = CRC16IBM.generate(region.hexStringToByteArray())
        assertEquals("00002994".toInt(16), crc16)
    }

    @Test
    fun generateNegativeTest() {
        val manipulatedAvlString = StringBuilder(avlString).apply {
            replace(34, 36, "ee")
        }.toString()
        val region = manipulatedAvlString.substring(16, manipulatedAvlString.length - 8)
        val crc16 = CRC16IBM.generate(region.hexStringToByteArray())
        assertNotEquals("00002994".toInt(16), crc16)
    }



}