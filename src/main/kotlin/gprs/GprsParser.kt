package gprs

import CRC16IBM
import common_utils.crcCheck
import common_utils.decodeHex
import kotlin.text.StringBuilder
import common_utils.hexStringToByteArray
import common_utils.prependAndBuildString

/**
 * Decode the response to codec 12 request.
 *
 * @param hex   The response from the FMB003 device as hexadecimal string.
 * @return      The decoded response. Not optimized for storing in database
 * (optimize for database storage by creating data class and making it the return type).
 */
fun decodeResponseCodec12(hex: String): ResponseCodec12 {
    val codecID = hex.subSequence(16, 18).toString().toInt(16)

    require(hex.length % 2 == 0) {"Length of hexadecimal stream must be even!"}
    require(codecID == 12) {"Incompatible codec, should be codec 12"}
    require(crcCheck(hex)) {"CRC discrepancy"}

    val size : Int = hex.subSequence(8, 16).toString().toInt(16)

    val responseQuantity = hex.subSequence(18, 20).toString().toInt(16)

    val responseType = hex.subSequence(20, 22).toString().toInt(16)

    val responseSize = hex.subSequence(22, 30).toString().toInt(16)

    val commandResponse = decodeHex(hex.subSequence(30, 30 + responseSize * 2).toString())


    return ResponseCodec12(
        dataSize = size,
        responseQuantity = responseQuantity,
        responseType = responseType,
        data = commandResponse
    )
}



/**
 * Encode a request in codec 12, given by the command. Only one command is allowed. In future iterations, several
 * commands could be added.
 *
 * @param command   The command, as string.
 * @return          The encoded command.
 */
fun encodeCodec12Request(command: String): ByteArray {
    val request = StringBuilder()

    //Null bytes
    request.append("00000000")
    val requestSize = 8 + command.length // 8 is the constant length in bytes of static elements

    //Size of request to device
    val requestSizeHex = requestSize.toByte().toString(16)
    request.append(prependAndBuildString(requestSizeHex, 8, '0'))

    //Codec ID and command quantity 1
    request.append("0c") //codec ID: 0c = 12
    request.append("0105") //command quantity 1: 01 (always); type: 05 - command (always).

    //Size of the specific command (getinfo, etc.)
    val commandSize = command.length
    val commandSizeHex = commandSize.toByte().toString(16)
    request.append(prependAndBuildString(commandSizeHex, 8,'0'))

    //The command
    for (element in command) {
        request.append(element.toByte().toString(16))
    }

    //Command quantity 2
    request.append("01")

    //Calculating the CRC-16 (IBM) for the command.
    val crcSubString = request.toString().subSequence(16, 32 + (command.length * 2))
        .toString().hexStringToByteArray()
    val crcByteArray = CRC16IBM.generate(crcSubString)
    val crc = crcByteArray.toString(16)
    request.append(prependAndBuildString(crc, 8, '0'))
    return request.toString().hexStringToByteArray()
}
