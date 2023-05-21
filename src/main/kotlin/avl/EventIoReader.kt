/**
 * PACKAGE AVL, files:
 *      - AvlParser.kt:             Parser for AVL.
 *      - AvlParserDataTypes.kt     File that contains all the data types (data classes and enum classes) required for
 *                                  the parsing of the AVL.
 *      - EventIoReader.kt          File that contains all functions for reading the CSV-file containing information
 *                                  about all AVL parameters.
 *
 * EventIoReader.kt
 */
package avl

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Finds whether a target ID is marked an ID of interest AND makes sure that it is a valid ID.
 * There is most likely a more efficient way to do this.
 *
 * @param targetId          The ID to check.
 * @param idsOfInterest     The IDs to check against.
 * @return                  Boolean value that represents whether the ID is matched and is valid.
 */
fun findId(targetId: Int, idsOfInterest: List<Int>) : Boolean {
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("avl_ids.csv")
    var found = false
    BufferedReader(InputStreamReader(inputStream)).use { reader ->
        reader.readLine() // Ignore the header row
        reader.forEachLine { line ->
            val columns = line.split(",")

            val id = columns[0].toInt()

            if (id == targetId && idsOfInterest.contains(targetId)) {
                found = true
                return@forEachLine
            }
        }
    }
    return found
}

/**
 * Finds the information about a given ID from the csv file "avl_ids.csv" (in resources folder).
 *
 * @param targetId  The target ID
 * @return          The data related to the ID, if target ID was found. If not, null.
 */
fun idInformation(targetId: Int) : AvlIdData? {
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("avl_ids.csv")
    val avlIdList = mutableListOf<AvlIdData>()
    BufferedReader(InputStreamReader(inputStream)).use { reader ->
        reader.readLine() // Ignore the header row

        reader.forEachLine { line ->
            val columns = line.split(",")

            val id = columns[0].toInt()

            if (id == targetId) {
                avlIdList.add ((AvlIdData(
                    id = id,
                    name = columns[1],
                    bytes = columns[2],
                    type = columns[3],
                    minValue = columns[4],
                    maxValue = columns[5],
                    multiplier = columns[6],
                    units = columns[7],
                    description = columns[8],
                    group = columns[9]
                )))
                return@forEachLine
            }
        }
    }
    if (avlIdList.size > 0) return avlIdList[0]
    return null
}


