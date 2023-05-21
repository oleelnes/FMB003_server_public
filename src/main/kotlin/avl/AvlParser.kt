/**
 * PACKAGE AVL, files:
 *      - AvlParser.kt:             Parser for AVL.
 *      - AvlParserDataTypes.kt     File that contains all the data types (data classes and enum classes) required for
 *                                  the parsing of the AVL.
 *      - EventIoReader.kt          File that contains all functions for reading the CSV-file containing information
 *                                  about all AVL parameters.
 *
 * AvlParser.kt
 *      This file contains all the required functions for parsing an AVL (automatic vehicle location) sent by FMB003
 *      encoded in codec protocol 8E.
 */
package avl

import common_utils.crcCheck
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Avl handler: top-most level.
 * The way it works:
 *      - Checks codec ID: should be 8E
 *      - Checks CRC-16: compares received with calculated, see Crc16Ibm.kt file
 *      - If any of these checks fail, a (mostly) empty instance of AVL data class will be returned, along with a
 *      proper avlStatus, as given by the enum class AVLStatus (see AvlParserDataTypes)
 *      - If all checks are passed, it will parse the avl.
 *
 * @param avlString         The AVL fetched from FMB003 device, represented by a string in hexadecimal format.
 * @param idsOfInterest     The IDs of interest.
 * @param verbose           Boolean value stating the verbosity of the execution the function.
 * @param discardUnmatched  Whether to discard events that do not match any of the given IDs in list [idsOfInterest].
 * @return                  AVL, an instance of the AVL data class
 */
fun parseAvlCodec8eHandler(avlString: String, idsOfInterest: List<Int> = emptyList()
                           , verbose: Boolean = false, discardUnmatched: Boolean = true): AVL {
    if (!codecCheck(avlString, "8e"))
        return AVL(0, 0, 0, avlStatus = AVLStatus.CODEC_INCOMPATIBLE)
    if (!crcCheck(avlString))
        return AVL(0, 0, 0, avlStatus = AVLStatus.CRC_FAILED)
    return parseAVL(avlString, idsOfInterest, verbose = verbose, discardUnmatched = discardUnmatched)
}

/**
 * Parse one AVL record (one AVL may consist of several AVL records).
 * How it works
 *      - First, it parses generic information about the entire AVL
 *      - Then it will parse each of the AVL records separately
 *          - Parse generic information about each AVL record
 *          - Update highestPriority if needed
 *          - Parse IO elements, with which the avlData String is passed
 *          - A list of the parsed IO elements for each byte value category is returned, along with the updated
 *          avlData String (the beginning of the String now corresponds to the beginning of the next AVL record)
 *      - When all AVL records are parsed, the AVL will be finalized by creating an instance of AVL, where all the
 *      relevant information from the parsing will be stored.
 *
 * @param avlString         The AVL data in a string in hexadecimal format.
 * @param idsOfInterest     The IDs of interest. Optional parameter, emptyList by default.
 * @param verbose           Boolean value stating the verbosity of the execution the function.
 * @param discardUnmatched  Whether to discard events that do not match any of the given IDs in list [idsOfInterest].
 * @return                  The parsed AVL.
 */
fun parseAVL(avlString: String, idsOfInterest: List<Int> = emptyList(), verbose: Boolean = false
             , discardUnmatched: Boolean = true): AVL {
    val dataFieldLength = BigInteger(avlString.substring(8, 16), 16).toInt()
    val numRecords = avlString.substring(18, 20).toInt(16)

    //See enum class Priority in AvlParserDataTypes.kt file
    var highestPriority = 0
    var avlData = avlString.substring(20)
    val parsedAVL = mutableListOf<AvlRecord>()


    /**
     * An AVL can consist of several records, and usually does.
     * The number of AVL records are given by the field that occurs at the 18-20th bytes in the AVL.
     * In this for-loop, each record is parsed.
     */
    for (i in 0 until numRecords) {
        val eventIoId = avlData.substring(48, 52).toInt(16)
        val numberOfIds = avlData.substring(52, 56).toInt(16)
        // Get formatted timestamp
        val formattedTimestamp = formatTimestamp(avlData.substring(0, 16).toLong(16))
        val priority  = avlData.substring(16, 18).toInt(16)

        //If priority of record is higher than highestPriority, set highestPriority to be equal to priority
        if (priority > highestPriority) highestPriority = priority

        if (verbose) println("\n----------------------------- AVL PACKET ${i+1} ------------------------------ " +
                "\n\tTime: $formattedTimestamp\n\tEvent IO ID: $eventIoId, Number of IDs: $numberOfIds, " +
                "Priority: ${Priority.fromValue(priority)}\n\n\tEvents:")

        //Ignore GPS and other generic data
        avlData = avlData.substring(48)

        // Parse IO Elements.
        val resultPair = parseAvlRecord(avlData, formattedTimestamp, priority, idsOfInterest, verbose, discardUnmatched)
        avlData = resultPair.second

        parsedAVL.add(resultPair.first)
    }
    val numberOfEvents = parsedAVL.sumBy { it.allEvents.size }

    val avl = AVL(
        dataFieldLength = dataFieldLength,
        numberOfRecords = numRecords,
        numberOfEvents = numberOfEvents,
        avlList = parsedAVL,
        highestPriority = Priority.fromValue(highestPriority)
    )
    //Ignore avlStatus because it's standard value (NO_ERROR) is correct in this case.

    if (verbose) printGeneralInformation(avl)
    return avl
}



/**
 * Parses all elements in one AVL record -- an AVL may consist of several AVL records.
 * Utilizes [parseElement] to parse elements within the same byte value group.
 * How it works:
 *      - Read generic data about the AVL record that relates to the IO elements.
 *      - Parse each of the byte value categories. In every category there might be several IO elements/events.
 *      These are parsed by the parseElement function. The byte value of the length is denoted for each IO element.
 *      Each IO element is added to the same list (eventList) regardless of their byte value categories.
 *          - 1 byte
 *          - 2 byte
 *          - 4 byte
 *          - 8 byte
 *          - X byte
 *      - After the parsing of each byte value category, adjust the start point of the avlData String to the beginning
 *      of the next byte value category.
 *      - Pass all relevant information into the AvlRecord that will be returned. See explanation of the return type
 *      below for greater explanation.
 *
 * @param data              A string representing all AVL data.
 * @param time              The timestamp for the AVL packet.
 * @param idsOfInterest     The IDs of interest. If empty, will be ignored, and all IDs will be handled as "of interest".
 * @param verbose           Boolean value stating the verbosity of the execution the function.
 * @param priority          The priority of the AVL data/record.
 * @param discardUnmatched  Whether to discard events that do not match any of the given IDs in list [idsOfInterest].
 * @return                  A pair consisting of:
 *                              1) The parsed AVL data of type [ParsedAvlRecord];
 *                              2) A String that represents the remainder of the AVL data (data of the current AVl
 *                              is cut out).
 */
fun parseAvlRecord(data: String, time: String, priority: Int, idsOfInterest: List<Int> = emptyList()
                   , verbose: Boolean = false, discardUnmatched: Boolean = true) : Pair<AvlRecord, String> {
    var avlData = data
    val eventList = mutableListOf<AvlEventData>()
    val eventIoId = avlData.substring(0, 4).toInt(16)
    avlData = avlData.substring(4)

    val nTotalIo = avlData.substring(0, 4).toInt(16)
    avlData = avlData.substring(4)


    // ------------------  Parse IO elements ------------------
    // Parse all elements with length = 1 byte
    // Explanation of substring until: 4 (skip "N1 of One Byte IO) + 6 * n2 ->
    //                               ... 6: 4 (ID length) + 2 (One byte * 2) * n2 (number of elements with value = 1B)
    val n1 = avlData.substring(0, 4).toInt(16)
    eventList.addAll(parseElement(BYTE.ONE, avlData.substring(4, 4 + 6 * n1), n1,
        idsOfInterest, verbose, discardUnmatched = discardUnmatched))
    avlData = avlData.substring(4 + 6 * n1)

    // Parse all elements with length = 2 bytes.
    val n2 = avlData.substring(0, 4).toInt(16)
    eventList.addAll(parseElement(BYTE.TWO, avlData.substring(4, 4 + 8 * n2), n2,
        idsOfInterest, verbose, discardUnmatched = discardUnmatched))
    avlData = avlData.substring(4 + 8 * n2)

    // Parse all elements with length = 4 bytes
    val n4 = avlData.substring(0, 4).toInt(16)
    eventList.addAll(parseElement(BYTE.FOUR, avlData.substring(4, 4 + 12 * n4), n4,
        idsOfInterest, verbose, discardUnmatched = discardUnmatched))
    avlData = avlData.substring(4 + 12 * n4)

    // Parse all elements with length = 8 bytes
    val n8 = avlData.substring(0, 4).toInt(16)
    eventList.addAll(parseElement(BYTE.EIGHT, avlData.substring(4, 4 + 20 * n8), n8,
        idsOfInterest, verbose, discardUnmatched = discardUnmatched))
    avlData = avlData.substring(4 + 20 * n8)

    // Parse all elements with length = X bytes
    val nX = avlData.substring(0, 4).toInt(16)
    //First, find the byte value of the length of the value to the IO element
    val xLength = avlData.substring(4, 8).toInt(16)
    eventList.addAll(parseElement(BYTE.X, avlData.substring(4, 4 + (4 + xLength) * nX), nX,
        idsOfInterest, verbose, discardUnmatched = discardUnmatched))
    avlData = avlData.substring(4 + 20 * nX)
    if(verbose) println("\n\tNumber of events by byte value: n1: $n1, n2: $n2, n4: $n4, n8: $n8, nX: $nX\n\n")

    //The number of matched elements.
    val matchedCount = eventList.count { avlIOData -> avlIOData.matched }

    return Pair(AvlRecord(
        id = eventIoId,
        totalIo =  nTotalIo,
        matchedIo = matchedCount,
        time = time,
        allEvents = eventList,
        priority = Priority.fromValue(priority)
    ), avlData)
}

/**
 * Parse I/O elements.
 * How it works:
 *      - Parses each IO element until number of entries is reached
 *          - Parses id and value -> At this point, we have already fetched all information about the IO element.
 *          - Finds information about the id with the idInformation function. This variable contains all information
 *          about the id.
 *          - Updates the avl String if we are not at the last element
 *          - If information equals null, the ID does not exist, and neither the ID nor the value should thus be added
 *          to the list containing the IO elements.
 *          - If a list containing IDs of interest has been passed, check whether the current ID matches any in that
 *          list. If it does, the matched boolean value will be set to true.
 *          - If the current ID is not matched, and the idsOfInterest list is not empty, AND if discardUnmatched is
 *          set to true, do not add the IO element to the list. Because, if discardUnmatched is true, and the ID is not
 *          matched, the IO element should not be stored by definition. If, however, discardUnmatched is false, then
 *          all IO elements should be added to the list, regardless of the state of the match variable.
 *          - Information field in the AvlEventData instance should only be other than null if withInformation variable
 *          is true.
 *          - Add instance of AvlEventData to the list eventRecords.
 *      - Return the list eventRecords.
 *
 * @param byte              The length of the value in bytes.
 * @param avlData           The AVL data to parse
 * @param entries           The number of entries to parse (all entries share the same byte length)
 * @param idsOfInterest     The parameter IDs that are of interest. This list is empty by default, in which case it is
 *                          ignored.
 * @param verbose           Boolean value stating the verbosity of the execution the function.
 * @param withInformation   Whether to store information about parameters alongside the fetched data.
 * @param discardUnmatched  Whether to discard events that do not match any of the given IDs in list [idsOfInterest].
 * @return                  A list of the resulting IO Data, defined by the data class [AvlEventData].
 * @see                     AvlEventData
 */
fun parseElement(byte: BYTE, avlData: String, entries: Int,
                 idsOfInterest: List<Int> = emptyList(), verbose: Boolean = false,
                 withInformation: Boolean = false , discardUnmatched: Boolean = true): List<AvlEventData> {
    var avl = avlData
    val eventRecords = mutableListOf<AvlEventData>()

    for (i in 0 until entries) {
        val id = avl.substring(0, 4).toInt(16)
        val value = avl.substring(4, 4+byte.value).toInt(16)
        val information = idInformation(id)
        var matched = false

        // If we are not at the last entry, update the substring
        if (i < entries - 1) avl = avl.substring(4 + byte.value)

        //If ID was not found, go to next (smart casting).
        if (information == null) continue
        if (idsOfInterest.isNotEmpty()) matched = findId(id, idsOfInterest)

        // Do not add record if there is an idsOfInterest list AND the current record did not match an ID from the list.
        if (!matched && idsOfInterest.isNotEmpty() && discardUnmatched) continue

        val informationField = if (withInformation) information else null

        eventRecords.add(AvlEventData(
            id=id,
            name = information.name,
            value = value,
            matched = matched,
            information = informationField)
        )

        if (verbose) println("\t$i(${byte.value / 2} byte) id: ${id + 1}, name: ${information.name}, " +
                    "value: $value, matched: ${matched}")

    }
    return eventRecords
}

/**
 * Checks if the codec of the AVL corresponds to the expected codec (8E).
 *
 * @param avlString The AVl data as hexadecimal string.
 * @param codecId   The expected codec ID (for now, always 8E).
 * @return          Boolean value: true if codec IDs correspond, false if not.
 */
fun codecCheck(avlString: String, codecId: String): Boolean {
    val avlCodecId = avlString.substring(16, 18)
    return avlCodecId.equals(codecId, ignoreCase = true)
}

/**
 * Formats timestamp to the same format as given in the documentation.
 *
 * @param timestamp The difference, in milliseconds, between the current time and midnight, January, 1970, UTC.
 * @return          The formatted timestamp as a String.
 */
fun formatTimestamp(timestamp : Long) : String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm:ss a")
        .withZone(ZoneId.of("GMT+2"))
        .withLocale(Locale.ENGLISH)
    return formatter.format(instant)
}

fun printGeneralInformation(avl: AVL) {
    println("--------------------------- PARSE SUCCESSFUL ---------------------------\n" +
            "GENERAL INFORMATION ABOUT AVL: " +
            "\n\tData field length: ${avl.dataFieldLength} \n\tRecord/packet count: ${avl.avlList.size}" +
            "\n\tNumber of events: ${avl.numberOfEvents}\n\tHighest priority: ${avl.highestPriority}\n\n\n")
}


