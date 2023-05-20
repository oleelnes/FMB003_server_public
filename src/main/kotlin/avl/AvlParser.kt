package avl

import common_utils.crcCheck
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Avl handler
 *
 * @param avlString         The AVL fetched from FMB003 device, represented by a string in hexadecimal format.
 * @param idsOfInterest     The IDs of interest.
 * @param verbose           Boolean value stating the verbosity of the execution the function.
 * @param discardUnmatched  Whether to discard events that do not match any of the given IDs in list [idsOfInterest].
 * @return
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
 * Parse one AVL record (one AVL may consist of several AVL records).
 *
 * @param avlString         The AVL data in a string in hexadecimal format.
 * @param idsOfInterest     The IDs of interest. Optional parameter, emptyList by default.
 * @param verbose           Boolean value stating the verbosity of the execution the function.
 * @param discardUnmatched  Whether to discard events that do not match any of the given IDs in list [idsOfInterest].
 * @return
 */
fun parseAVL(avlString: String, idsOfInterest: List<Int> = emptyList(), verbose: Boolean = false
             , discardUnmatched: Boolean = true): AVL {
    val dataFieldLength = BigInteger(avlString.substring(8, 16), 16).toInt()
    val numRecords = avlString.substring(18, 20).toInt(16)
    var highestPriority = 0


    var avlData = avlString.substring(20)
    val parsedAVL = mutableListOf<AvlRecord>()


    for (i in 0 until numRecords) {
        val eventIoId = avlData.substring(48, 52).toInt(16)
        val numberOfIds = avlData.substring(52, 56).toInt(16)
        // Get formatted timestamp
        val formattedTimestamp = formatTimestamp(avlData.substring(0, 16).toLong(16))
        val priority  = avlData.substring(16, 18).toInt(16)
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


