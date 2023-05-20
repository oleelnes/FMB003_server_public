package avl


/**
 * The return type of the parsing of the AVL.
 *
 * @property dataFieldLength    The length, in bytes, of the AVL bytestream.
 * @property numberOfRecords    Number of AVL data packets in AVL bytestream.
 * @property avlList            The list of AVLs, ordered naturally (first to last), list of [AvlRecord].
 * @property avlStatus          The status of the AVL, [AVLStatus].
 * @property highestPriority    The highest priority that appears in the AVL.
 */
data class AVL (
    val dataFieldLength: Int,
    val numberOfRecords: Int,
    val numberOfEvents: Int,
    val avlList: List<AvlRecord> = emptyList(),
    val avlStatus: AVLStatus = AVLStatus.NO_ERROR,
    val highestPriority: Priority = Priority.NOT_DETERMINED
)

/**
 * Parsed AVL record.
 *
 * @property id         The ID of the AVL parameter
 * @property totalIo    The total number of events/records in the AVL.
 * @property matchedIo  The total number of events/records (from AvlIoData) whose MATHCED boolean value is TRUE
 * @property time       The timestamp which represents when the AVL was created (not sent).
 * @property priority   The priority of the AVL record.
 * @property allEvents  All events. When a list consisting of the IDs of interest are given, allEvents will only be
 *                      the events with IDs of interest.
 */
data class AvlRecord(
    val id: Int,
    val totalIo: Int,
    val matchedIo: Int,
    val time: String,
    val priority: Priority = Priority.NOT_DETERMINED,
    val allEvents: List<AvlEventData> = emptyList()
)

/**
 * Avl I/O event/element data.
 *
 * @property id             The ID of the AVL parameter.
 * @property name           The name of the AVL parameter.
 * @property value          The value of the AVL parameter
 * @property matched        A boolean value, representing whether the ID is matches an ID in the idsOfInterest.
 * @property information    All information about the parameter. This field is not strictly speaking necessary,
 *                          which is why it is nullable AND optional (with the standard value set to null).
 */
data class AvlEventData (
    val id: Int,
    val name: String,
    val value: Any,
    val matched: Boolean,
    val information: AvlIdData? = null
)

/**
 * Avl id data
 *
 * @property id
 * @property name
 * @property bytes
 * @property type
 * @property minValue
 * @property maxValue
 * @property multiplier
 * @property units
 * @property description
 * @property group
 * @constructor Create empty Avl id data
 */
data class AvlIdData (
    val id: Int,
    val name: String,
    val bytes: String,
    val type: String,
    val minValue: String,
    val maxValue: String,
    val multiplier: String,
    val units: String,
    val description: String,
    val group: String
)

/**
 * The status of the AVL.
 */
enum class AVLStatus {
    NO_ERROR,
    CODEC_INCOMPATIBLE,
    CRC_FAILED
}

/**
 * Byte
 *
 * @property value  Value refers to the number of chars that represent one byte in hexadecimal. Two chars represents
 *                  one byte.
 */
enum class BYTE(val value: Int) {
    ONE(2),
    TWO(4),
    FOUR(8),
    EIGHT(16),
    X(16)
}

/**
 * Priority of AVL record.
 *
 * @property value  The priority represented as value
 */
enum class Priority(val value: Int) {
    NOT_DETERMINED(-1),
    LOW(0),
    HIGH(1),
    PANIC(2);

    companion object {
        fun fromValue(value: Int): Priority {
            return values().find { it.value == value } ?: NOT_DETERMINED
        }
    }
}