package avl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AvlParserKtTest {
    //IMPORTANT!!
    //THE BELOW AVLS HAVE BEEN ERASED FROM PROJECT BECAUSE THEY INCLUDE SENSITIVE INFORMATION AS THEY WERE
    //FROM REAL-WORLD TESTING.
    //THE OTHER AVLS IN THE REPOSITORY ARE FROM THE DOCUMENTATION AND REPRESENT NO CAUSE FOR CONCERN
    lateinit var avl: AVL
    val avlString1 = "0"
    val avlString2 = "0"
    val avlString3 = "0"
    val avlString4 = "0"
    val avlString5 = "0"

    @BeforeEach
    fun beforeEach(){
        parseAvlCodec8eHandler(avlString1, verbose = false)
        avl = parseAVL(avlString1)
        println(avl)
    }

    @Test
    fun crcFailedTest() {
        val manipulatedAvlString = StringBuilder(avlString1).apply {
            replace(34, 36, "ee")
        }.toString()
        val manipulatedAvl = parseAvlCodec8eHandler(manipulatedAvlString)
        println(manipulatedAvl.avlStatus)
        assertEquals(true, manipulatedAvl.avlStatus == AVLStatus.CRC_FAILED)
    }

    @Test
    fun avlNoError() {
        assertEquals(true, avl.avlStatus == AVLStatus.NO_ERROR)
    }

    @Test
    fun avlDataFieldLength() {
        assertEquals(1230, avl.dataFieldLength)
    }

    @Test
    fun avlNumberOfRecords() {
        assertEquals(12, avl.numberOfRecords)
    }

    /**
     * Incorrect codec test
     *
     * Comment: An incorrect codec will cause an incorrect CRC-16. However, by structuring the code correctly,
     *          it is possible circumvent this by not checking crc if codec has already failed. For this reason, it is
     *          important to test that an AVL with incorrect Codec ID will have the Error Type CODEC_INCOMPATIBLE
     */
    @Test
    fun incorrectCodecTest() {
        val incorrectCodecAVLString = StringBuilder(avlString1).apply {
            replace(16, 18, "08")
        }.toString()
        val incorrectCodecAVL = parseAvlCodec8eHandler(incorrectCodecAVLString)
        assertEquals(true, incorrectCodecAVL.avlStatus == AVLStatus.CODEC_INCOMPATIBLE)
    }



    @Test
    fun manyAvlsTest() {
        val avl1 = parseAvlCodec8eHandler(avlString2, verbose = true)
        val avl2 = parseAvlCodec8eHandler(avlString3)
        val avl3 = parseAvlCodec8eHandler(avlString4)
        val avl4 = parseAvlCodec8eHandler(avlString5)
        assertEquals(true, avl1.avlStatus == AVLStatus.NO_ERROR)
        assertEquals(true, avl2.avlStatus == AVLStatus.NO_ERROR)
        assertEquals(true, avl3.avlStatus == AVLStatus.NO_ERROR)
        assertEquals(true, avl4.avlStatus == AVLStatus.NO_ERROR)
    }

    @Test
    fun idsOfInterestTest() {
        val idsOfInterest = listOf(24, 68, 69, 200, 240)
        val avl1 = parseAvlCodec8eHandler(avlString2, idsOfInterest, verbose = true)
        val unique = avl1.avlList
            .flatMap { it.allEvents } // flatten the lists
            .distinctBy { it.id } // get distinct elements
            .count() // count the distinct elements
        assertEquals(idsOfInterest.size, unique )
    }

    @Test
    fun idsOfInterestAndIncludeUnmatchedTest() {
        val idsOfInterest = listOf(24, 68, 69, 200, 240)
        val avl1 = parseAvlCodec8eHandler(avlString2, idsOfInterest, verbose = true, discardUnmatched = false)
        val unique = avl1.avlList
            .flatMap { it.allEvents } // flatten the lists
            .distinctBy { it.id } // get distinct elements
            .count() // count the distinct elements
        assertNotEquals(idsOfInterest.size, unique )
    }
}