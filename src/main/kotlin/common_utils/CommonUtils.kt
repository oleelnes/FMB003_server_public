package common_utils


/**
 * Convert hex string to byte array
 *
 * @return  Converted hex string as bytearray
 */
fun String.hexStringToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in 0 until length step 2) {
        val first = substring(i, i + 1).toInt(16)
        val second = substring(i + 1, i + 2).toInt(16)
        result[i / 2] = ((first shl 4) + second).toByte()
    }
    return result
}

/**
 * Prepends a given (hex) String with zeros until the difference between length and endOfString.length is zero, then
 * append the endOfString String.
 *
 * @param endOfString   The String to be prepended.
 * @param length        The total length of the String.
 * @param prepend       The char to prepend the req String.
 * @return
 */
fun prependAndBuildString(endOfString : String, length : Int, prepend : Char) : String {
    require(endOfString.length <= length)
    if (endOfString.length == length) return endOfString

    val ret = StringBuilder()
    var i = 0
    while (i++ < length - endOfString.length) ret.append(prepend)
    return ret.append(endOfString).toString()
}

/**
 * Decode hex
 *
 * @param hex   The received data from the FMB003 device.
 * @return      A String representing the decoded
 */
fun decodeHex(hex: String): String {
    require(hex.length % 2 == 0) {"Length of hex must be even"}
    return hex.chunked(2)
        .map{it.toInt(16).toByte()}
        .toByteArray()
        .toString(Charsets.US_ASCII)
}

/**
 * Compares calculated CRC (based on received data) with the CRC sent with the codec. CRC is calculated by generate
 * function in Crc16Ibm kotlin file in package common_utils.
 *
 * @param hexString The bytestream represented as a hexadecimal string.
 * @return          Boolean value: true if they match; false if not.
 */
fun crcCheck(hexString: String): Boolean {
    val crc = hexString.substring(hexString.length - 8, hexString.length).toInt(16)
    val crcAreaAsString = hexString.substring(16, hexString.length - 8)
    val crcAreaAsByteArray = crcAreaAsString.hexStringToByteArray()
    return crc == CRC16IBM.generate(crcAreaAsByteArray)
}