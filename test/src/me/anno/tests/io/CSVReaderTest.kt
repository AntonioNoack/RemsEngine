package me.anno.tests.io

import me.anno.io.csv.CSVReader
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class CSVReaderTest {
    @Test
    fun testReadCSV() {
        val file = """
            Address, Name ,Phone, Email,
            Wonder Land Street 17, Donald, 012345 456, donald@disney.com
            Big Birch Street, Daisy, , daisy@disney.com
        """.trimIndent()
        val data = CSVReader.read(file, ',', '\n')
        assertEquals(
            mapOf(
                "Address" to listOf("Wonder Land Street 17", "Big Birch Street"),
                "Name" to listOf("Donald", "Daisy"),
                "Phone" to listOf("012345 456", ""),
                "Email" to listOf("donald@disney.com", "daisy@disney.com")
            ), data
        )
    }
}