package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.joml.Planed
import org.joml.Planef
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

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

    @Test
    fun testFindXYZF() {
        val rnd = Random(123)
        for (i in 0 until 20) {
            val pos = Vector3f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
            val dir = Vector3f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
                .sub(0.5f).normalize()
            val plane = Planef(pos, dir)
            assertEquals(pos.x, plane.findX(pos.y, pos.z), 1e-5f)
            assertEquals(pos.y, plane.findY(pos.x, pos.z), 1e-5f)
            assertEquals(pos.z, plane.findZ(pos.x, pos.y), 1e-5f)
        }
    }

    @Test
    fun testFindXYZD() {
        val rnd = Random(123)
        for (i in 0 until 20) {
            val pos = Vector3d(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble())
            val dir = Vector3d(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble())
                .sub(0.5).normalize()
            val planeF = Planed(pos, dir)
            assertEquals(pos.x, planeF.findX(pos.y, pos.z), 1e-15)
            assertEquals(pos.y, planeF.findY(pos.x, pos.z), 1e-15)
            assertEquals(pos.z, planeF.findZ(pos.x, pos.y), 1e-15)
        }
    }

    @Test
    fun testHashCode() {
        val mapF = HashSet<Planef>()
        val mapD = HashSet<Planed>()
        val rnd = Random(123)
        for (i in 0 until 20) {
            val pos = Vector3f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
            val dir = Vector3f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
                .sub(0.5f).normalize()
            val pos1 = Vector3d(pos)
            val dir1 = Vector3d(dir)
            assertTrue(mapF.add(Planef(pos, dir)))
            assertFalse(mapF.add(Planef(pos, dir)))
            assertTrue(mapD.add(Planed(pos1, dir1)))
            assertFalse(mapD.add(Planed(pos1, dir1)))
        }
        assertEquals(20, mapF.size)
        assertEquals(20, mapD.size)
    }
}