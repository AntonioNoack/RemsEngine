package me.anno.tests.joml

import me.anno.tests.LOGGER
import me.anno.utils.assertions.assertEquals
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MatrixTransformTests {

    @Test
    fun testTranslatingPositions() {
        testMatrixTransform(
            Matrix3x2f().translate(1f, 2f), ::Vector2f,
            { m, v -> m.transformPosition(v) }, {
                it.add(1f, 2f)
            })
        testMatrixTransform(
            Matrix4x3f().translate(1f, 2f, 3f), ::Vector3f,
            { m, v -> m.transformPosition(v) }, {
                it.add(1f, 2f, 3f)
            })
        testMatrixTransform(
            Matrix4f().translate(1f, 2f, 3f), ::Vector3f,
            { m, v -> m.transformPosition(v) }, {
                it.add(1f, 2f, 3f)
            })
        testMatrixTransform(
            Matrix3x2d().translate(1.0, 2.0), ::Vector2d,
            { m, v -> m.transformPosition(v) }, {
                it.add(1.0, 2.0)
            })
        testMatrixTransform(
            Matrix4x3d().translate(1.0, 2.0, 3.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.add(1f, 2f, 3f)
            })
        testMatrixTransform(
            Matrix4d().translate(1.0, 2.0, 3.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.add(1.0, 2.0, 3.0)
            })
    }

    @Test
    fun testTranslatingDirections() {
        testMatrixTransform(
            Matrix3x2f().translate(1f, 2f), ::Vector2f,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix4x3f().translate(1f, 2f, 3f), ::Vector3f,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix4f().translate(1f, 2f, 3f), ::Vector3f,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix3x2d().translate(1.0, 2.0), ::Vector2d,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix4x3d().translate(1.0, 2.0, 3.0), ::Vector3d,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix4d().translate(1.0, 2.0, 3.0), ::Vector3d,
            { m, v -> m.transformDirection(v) }, { it })
    }

    @Test
    fun testScaling() {
        testMatrixTransform(
            Matrix2f().scale(2f, -3f), ::Vector2f,
            { m, v -> m.transform(v) }, {
                it.mul(2f, -3f)
            })
        testMatrixTransform(
            Matrix3x2f().scale(2f, -3f), ::Vector2f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2f, -3f)
            })
        testMatrixTransform(
            Matrix3f().scale(2f, -3f, 3f), ::Vector3f,
            { m, v -> m.transform(v) }, {
                it.mul(2f, -3f, 3f)
            })
        testMatrixTransform(
            Matrix4x3f().scale(2f, -3f, 3f), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2f, -3f, 3f)
            })
        testMatrixTransform(
            Matrix4f().scale(2f, -3f, 3f), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2f, -3f, 3f)
            })
        testMatrixTransform(
            Matrix3x2d().scale(2.0, -3.0), ::Vector2d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2.0, -3.0)
            })
        testMatrixTransform(
            Matrix4x3d().scale(2.0, -3.0, 3.0), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2.0, -3.0, 3.0)
            })
        testMatrixTransform(
            Matrix4d().scale(2.0, -3.0, 3.0), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2.0, -3.0, 3.0)
            })
    }

    @Test
    fun testRotating() {
        testMatrixTransform(
            Matrix3x2f().rotate(5f), ::Vector2f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.rotate(5f)
            })
        testMatrixTransform(
            Matrix3f().rotateX(1f).rotateY(2f).rotateZ(3f), ::Vector3f,
            { m, v -> m.transform(v) }, {
                it.rotateZ(3f).rotateY(2f).rotateX(1f)
            })
        testMatrixTransform(
            Matrix4x3f().rotateX(1f).rotateY(2f).rotateZ(3f), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.rotateZ(3f).rotateY(2f).rotateX(1f)
            })
        testMatrixTransform(
            Matrix4f().rotateX(1f).rotateY(2f).rotateZ(3f), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.rotateZ(3f).rotateY(2f).rotateX(1f)
            })
        testMatrixTransform(
            Matrix2d().rotate(5.0), ::Vector2d,
            { m, v -> m.transform(v) }, {
                it.rotate(5.0)
            })
        testMatrixTransform(
            Matrix3x2d().rotate(5.0), ::Vector2d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.rotate(5.0)
            })
        testMatrixTransform(
            Matrix3d().rotateX(1.0).rotateY(2.0).rotateZ(3.0), ::Vector3d,
            { m, v -> m.transform(v) }, {
                it.rotateZ(3.0).rotateY(2.0).rotateX(1.0)
            })
        testMatrixTransform(
            Matrix4x3d().rotateX(1.0).rotateY(2.0).rotateZ(3.0), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.rotateZ(3.0).rotateY(2.0).rotateX(1.0)
            })
        testMatrixTransform(
            Matrix4d().rotateX(1.0).rotateY(2.0).rotateZ(3.0), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.rotateZ(3.0).rotateY(2.0).rotateX(1.0)
            })
    }

    @Test
    fun testNegateX() {
        testMatrixTransform(
            Matrix3f().negateX(), ::Vector3f,
            { m, v -> m.transform(v) }, {
                it.x = -it.x; it
            })
        testMatrixTransform(
            Matrix4x3f().negateX(), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.x = -it.x; it
            })
        testMatrixTransform(
            Matrix3d().negateX(), ::Vector3d,
            { m, v -> m.transform(v) }, {
                it.x = -it.x; it
            })
        testMatrixTransform(
            Matrix4x3d().negateX(), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.x = -it.x; it
            })
        testMatrixTransform(
            Matrix4d().negateX(), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.x = -it.x; it
            })
    }

    @Test
    fun testNegateY() {
        testMatrixTransform(
            Matrix3f().negateY(), ::Vector3f,
            { m, v -> m.transform(v) }, {
                it.y = -it.y; it
            })
        testMatrixTransform(
            Matrix4x3f().negateY(), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.y = -it.y; it
            })
        testMatrixTransform(
            Matrix3d().negateY(), ::Vector3d,
            { m, v -> m.transform(v) }, {
                it.y = -it.y; it
            })
        testMatrixTransform(
            Matrix4x3d().negateY(), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.y = -it.y; it
            })
        testMatrixTransform(
            Matrix4d().negateY(), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.y = -it.y; it
            })
    }

    @Test
    fun testNegateZ() {
        testMatrixTransform(
            Matrix3f().negateZ(), ::Vector3f,
            { m, v -> m.transform(v) }, {
                it.z = -it.z; it
            })
        testMatrixTransform(
            Matrix4x3f().negateZ(), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.z = -it.z; it
            })
        testMatrixTransform(
            Matrix3d().negateZ(), ::Vector3d,
            { m, v -> m.transform(v) }, {
                it.z = -it.z; it
            })
        testMatrixTransform(
            Matrix4x3d().negateZ(), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.z = -it.z; it
            })
        testMatrixTransform(
            Matrix4d().negateZ(), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.z = -it.z; it
            })
    }

    fun <M : Any, V : Vector> testMatrixTransform(
        matrix: M,
        createVector: () -> V,
        transform: (M, V) -> V,
        transformManually: (V) -> V
    ) {
        testMatrixTransform(
            matrix, createVector,
            transform, transform,
            transformManually
        )
    }

    fun <M : Any, V : Vector> testMatrixTransform(
        matrix: M,
        createVector: () -> V,
        transform1: (M, V) -> V,
        transform2: (M, V) -> V,
        transformManually: (V) -> V
    ) {
        val random = Random(1234)
        for (i in 0 until 20) {
            if (i == 10) {
                try {
                    // for half the tests, clear the properties flag
                    matrix::class.java
                        .getMethod("_properties", Int::class.java)
                        .invoke(matrix, 0)
                } catch (e: NoSuchMethodException) {
                    LOGGER.warn("Missing ${e.message}")
                }
            }
            val a = createVector()
            val b = createVector()
            val c = createVector()
            for (j in 0 until a.numComponents) {
                val v = (random.nextDouble() - 0.5) * 20.0
                a.setComp(j, v)
                b.setComp(j, v)
                c.setComp(j, v)
            }
            val expected = transformManually(a)
            val actually1 = transform1(matrix, b)
            assertEquals(expected, actually1, 1e-4)
            val actually2 = transform2(matrix, c)
            assertEquals(expected, actually2, 1e-4)
        }
    }
}