package avl

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class EventIoReaderKtTest {

    @Test
    fun findId() {
        assertEquals(true, findId(25, listOf(25)))
    }
}