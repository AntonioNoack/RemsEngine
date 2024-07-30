package me.anno.tests.utils

import net.boeckling.crc.CRC64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestCRC64 {
    @Test
    fun test() {
        val str = "123456789"
        val hash0 = -7395533204333446662L // consistency is better than nothing ^^
        val hash1 = CRC64.update(str.encodeToByteArray(), 0, str.length, 0L)
        assertEquals(hash0, hash1)
    }
}