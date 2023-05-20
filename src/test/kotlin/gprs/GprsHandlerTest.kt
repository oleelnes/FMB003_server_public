package gprs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GprsHandlerTest {

    /**
     * Testing decoder for codec 12. Comparing result with documentation:
     * https://wiki.teltonika-gps.com/view/Codec#Codec_12
     */
    @Test
    fun decodeResponseCodec12() {
        val response = "00000000000000370C01060000002F4449313A31204449323A30204449333A302041494E313A302041494E323A313639323420444F313A3020444F323A3101000066E3"
        val decoded = decodeResponseCodec12(response)
        assertEquals("DI1:1 DI2:0 DI3:0 AIN1:0 AIN2:16924 DO1:0 DO2:1", decoded.data)
    }

    @Test
    fun crcFailTest() {
        //Changed the value of a random byte (that is not the codec ID indicator)
        val response = "00000000000000370C01060000002F4449313A312044493FFA30204449333A302041494E313A302041494E323A313639323420444F313A3020444F323A3101000066E3"
        val thrown: Exception = assertThrows(Exception::class.java) {
            decodeResponseCodec12(response)
        }
        assertEquals("CRC discrepancy", thrown.message)
    }

    @Test
    fun decodeCodec13NegativeTest() {
        //In substring(16, 18), OC (12) is replaced with 0D (13)
        val response = "00000000000000370D01060000002F4449313A31204449323A30204449333A302041494E313A302041494E323A313639323420444F313A3020444F323A3101000066E3"
        val thrown: Exception = assertThrows(Exception::class.java) {
            decodeResponseCodec12(response)
        }
        assertEquals("Incompatible codec, should be codec 12", thrown.message)

    }

    @Test
    fun encodeCodec12Request() {
        val request = byteArrayOf(
            0x00, 0x00, 0x00, 0x00,                     //Zero bytes
            0x00, 0x00, 0x00, 0x0F,                     //Data size
            0x0C,                                       //Codec ID (12)
            0x01,                                       //Command quantity 1
            0x05,                                       //Command type
            0x00, 0x00, 0x00, 0x07,                     //Command size
            0x67, 0x65, 0x74, 0x69, 0x6E, 0x66, 0x6F,   //Command (getinfo)
            0x01,                                       //Command quantity 2
            0x00, 0x00, 0x43, 0x12                      //CRC-16
        )
        assertEquals(true, request.contentEquals(encodeCodec12Request("getinfo")))
    }

}