package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.AxisAngle4d
import org.joml.AxisAngle4f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.junit.jupiter.api.Test

class AxisAngleTest {

    @Test
    fun testEquality3x3f() {
        assertEquals(Matrix3f().rotateX(1f), Matrix3f().rotate(AxisAngle4f(1f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix3f().rotateX(-2f), Matrix3f().rotate(AxisAngle4f(-2f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix3f().rotateY(1f), Matrix3f().rotate(AxisAngle4f(1f, 0f, 1f, 0f)), 1e-6)
        assertEquals(Matrix3f().rotateZ(1f), Matrix3f().rotate(AxisAngle4f(1f, 0f, 0f, 1f)), 1e-6)
    }

    @Test
    fun testEquality4x3f() {
        assertEquals(Matrix4x3f().rotateX(1f), Matrix4x3f().rotate(AxisAngle4f(1f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4x3f().rotateX(-2f), Matrix4x3f().rotate(AxisAngle4f(-2f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4x3f().rotateY(1f), Matrix4x3f().rotate(AxisAngle4f(1f, 0f, 1f, 0f)), 1e-6)
        assertEquals(Matrix4x3f().rotateZ(1f), Matrix4x3f().rotate(AxisAngle4f(1f, 0f, 0f, 1f)), 1e-6)
    }

    @Test
    fun testEquality4x4f() {
        assertEquals(Matrix4f().rotateX(1f), Matrix4f().rotate(AxisAngle4f(1f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4f().rotateX(-2f), Matrix4f().rotate(AxisAngle4f(-2f, 1f, 0f, 0f)), 1e-6)
        assertEquals(Matrix4f().rotateY(1f), Matrix4f().rotate(AxisAngle4f(1f, 0f, 1f, 0f)), 1e-6)
        assertEquals(Matrix4f().rotateZ(1f), Matrix4f().rotate(AxisAngle4f(1f, 0f, 0f, 1f)), 1e-6)
    }

    @Test
    fun testEquality3x3d() {
        assertEquals(Matrix3d().rotateX(1.0), Matrix3d().rotate(AxisAngle4d(1.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix3d().rotateX(-2.0), Matrix3d().rotate(AxisAngle4d(-2.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix3d().rotateY(1.0), Matrix3d().rotate(AxisAngle4d(1.0, 0.0, 1.0, 0.0)), 1e-15)
        assertEquals(Matrix3d().rotateZ(1.0), Matrix3d().rotate(AxisAngle4d(1.0, 0.0, 0.0, 1.0)), 1e-15)
    }

    @Test
    fun testEquality4x3d() {
        assertEquals(Matrix4x3d().rotateX(1.0), Matrix4x3d().rotate(AxisAngle4d(1.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4x3d().rotateX(-2.0), Matrix4x3d().rotate(AxisAngle4d(-2.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4x3d().rotateY(1.0), Matrix4x3d().rotate(AxisAngle4d(1.0, 0.0, 1.0, 0.0)), 1e-15)
        assertEquals(Matrix4x3d().rotateZ(1.0), Matrix4x3d().rotate(AxisAngle4d(1.0, 0.0, 0.0, 1.0)), 1e-15)
    }

    @Test
    fun testEquality4x4d() {
        assertEquals(Matrix4d().rotateX(1.0), Matrix4d().rotate(AxisAngle4d(1.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4d().rotateX(-2.0), Matrix4d().rotate(AxisAngle4d(-2.0, 1.0, 0.0, 0.0)), 1e-15)
        assertEquals(Matrix4d().rotateY(1.0), Matrix4d().rotate(AxisAngle4d(1.0, 0.0, 1.0, 0.0)), 1e-15)
        assertEquals(Matrix4d().rotateZ(1.0), Matrix4d().rotate(AxisAngle4d(1.0, 0.0, 0.0, 1.0)), 1e-15)
    }

    @Test
    fun testFromM3x3fAndBack() {
        val original = Matrix3f().rotateYXZ(1f, 2f, 3f)
        val tested = AxisAngle4f().set(original)
        val cloned = Matrix3f().rotate(tested)
        assertEquals(original, cloned, 1e-6)
    }

    @Test
    fun testFromM4x3fAndBack() {
        val original = Matrix4x3f().rotateYXZ(1f, 2f, 3f)
        val tested = AxisAngle4f().set(original)
        val cloned = Matrix4x3f().rotate(tested)
        assertEquals(original, cloned, 1e-6)
    }

    @Test
    fun testFromM4x4fAndBack() {
        val original = Matrix4f().rotateYXZ(1f, 2f, 3f)
        val tested = AxisAngle4f().set(original)
        val cloned = Matrix4f().rotate(tested)
        assertEquals(original, cloned, 1e-6)
    }

    @Test
    fun testFromM3x3dAndBack() {
        val original = Matrix3d().rotateYXZ(1.0, 2.0, 3.0)
        val tested = AxisAngle4d().set(original)
        val cloned = Matrix3d().rotate(tested)
        assertEquals(original, cloned, 1e-15)
    }

    @Test
    fun testFromM4x3dAndBack() {
        val original = Matrix4x3d().rotateYXZ(1.0, 2.0, 3.0)
        val tested = AxisAngle4d().set(original)
        val cloned = Matrix4x3d().rotate(tested)
        assertEquals(original, cloned, 1e-15)
    }

    @Test
    fun testFromM4x4dAndBack() {
        val original = Matrix4d().rotateYXZ(1.0, 2.0, 3.0)
        val tested = AxisAngle4d().set(original)
        val cloned = Matrix4d().rotate(tested)
        assertEquals(original, cloned, 1e-15)
    }
}