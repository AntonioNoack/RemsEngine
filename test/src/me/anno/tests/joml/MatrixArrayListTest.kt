package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Matrix4d
import org.joml.Matrix4dArrayList
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3f
import org.joml.Matrix4x3fArrayList
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MatrixArrayListTest {
    @Test
    fun testArrayList4x3f() {
        val random = Random(1234)
        val states = Array(3) {
            Matrix4x3f().set(
                random.nextFloat(), random.nextFloat(), random.nextFloat(),
                random.nextFloat(), random.nextFloat(), random.nextFloat(),
                random.nextFloat(), random.nextFloat(), random.nextFloat(),
                random.nextFloat(), random.nextFloat(), random.nextFloat(),
            )
        }
        val list = Matrix4x3fArrayList()
        assertEquals(Matrix4x3f(), list)
        list.set(states[0])
        assertEquals(states[0], list)
        list.pushMatrix()
        assertEquals(states[0], list)
        list.set(states[1])
        assertEquals(states[1], list)
        list.popMatrix()
        assertEquals(states[0], list)
    }

    @Test
    fun testArrayList4f() {
        val random = Random(1234)
        val states = Array(3) {
            Matrix4f().set(
                random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(),
                random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(),
                random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(),
                random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(),
            )
        }
        val list = Matrix4fArrayList()
        assertEquals(Matrix4f(), list)
        list.set(states[0])
        assertEquals(states[0], list)
        list.pushMatrix()
        assertEquals(states[0], list)
        list.set(states[1])
        assertEquals(states[1], list)
        list.popMatrix()
        assertEquals(states[0], list)
    }

    @Test
    fun testArrayList4d() {
        val random = Random(1234)
        val states = Array(3) {
            Matrix4d().set(
                random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(),
                random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(),
                random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(),
                random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(),
            )
        }
        val list = Matrix4dArrayList()
        assertEquals(Matrix4d(), list)
        list.set(states[0])
        assertEquals(states[0], list)
        list.pushMatrix()
        assertEquals(states[0], list)
        list.set(states[1])
        assertEquals(states[1], list)
        list.popMatrix()
        assertEquals(states[0], list)
    }
}