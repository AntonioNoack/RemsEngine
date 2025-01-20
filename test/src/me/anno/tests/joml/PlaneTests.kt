package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Planed
import org.joml.Planef
import org.junit.jupiter.api.Test

class PlaneTests {
    @Test
    fun testGetCompF() {
        val plane = Planef(1f, 2f, 3f, 4f)
        assertEquals(1.0, plane.getComp(0))
        assertEquals(2.0, plane.getComp(1))
        assertEquals(3.0, plane.getComp(2))
        assertEquals(4.0, plane.getComp(3))
    }

    @Test
    fun testGetCompD() {
        val plane = Planed(1.0, 2.0, 3.0, 4.0)
        assertEquals(1.0, plane.getComp(0))
        assertEquals(2.0, plane.getComp(1))
        assertEquals(3.0, plane.getComp(2))
        assertEquals(4.0, plane.getComp(3))
    }

    @Test
    fun testSetCompF() {
        val plane = Planef()
        plane.setComp(0, 1.0)
        plane.setComp(1, 2.0)
        plane.setComp(2, 3.0)
        plane.setComp(3, 4.0)
        assertEquals(Planef(1f, 2f, 3f, 4f), plane)
    }

    @Test
    fun testSetCompD() {
        val plane = Planed()
        plane.setComp(0, 1.0)
        plane.setComp(1, 2.0)
        plane.setComp(2, 3.0)
        plane.setComp(3, 4.0)
        assertEquals(Planed(1.0, 2.0, 3.0, 4.0), plane)
    }
}