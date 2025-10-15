package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import org.joml.AxisAngle4d
import org.joml.AxisAngle4f
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.random.Random

class MatrixTransformTests {

    companion object {
        private val LOGGER = LogManager.getLogger(MatrixTransformTests::class)
    }

    @Test
    fun testIdentity() {
        assertEquals(
            Matrix2f(), Matrix2f(
                1f, 2f,
                3f, 4f
            ).identity()
        )
        assertEquals(
            Matrix2d(), Matrix2d(
                1.0, 2.0,
                3.0, 4.0
            ).identity()
        )
        assertEquals(
            Matrix3x2f(), Matrix3x2f(
                1f, 2f, 3f,
                4f, 5f, 6f
            ).identity()
        )
        assertEquals(
            Matrix3x2d(), Matrix3x2d(
                1.0, 2.0, 3.0,
                4.0, 5.0, 6.0
            ).identity()
        )
        assertEquals(
            Matrix3f(), Matrix3f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f, 9f
            ).identity()
        )
        assertEquals(
            Matrix3d(), Matrix3d(
                1.0, 2.0, 3.0, 4.0,
                5.0, 6.0, 7.0, 8.0, 9.0
            ).identity()
        )
        assertEquals(
            Matrix4x3f(), Matrix4x3f(
                1f, 2f, 3f, 4f, 5f, 6f,
                7f, 8f, 9f, 10f, 11f, 12f
            ).identity()
        )
        assertEquals(
            Matrix4x3d(), Matrix4x3d(
                1.0, 2.0, 3.0, 4.0, 5.0, 6.0,
                7.0, 8.0, 9.0, 10.0, 11.0, 12.0
            ).identity()
        )
        assertEquals(
            Matrix4f(), Matrix4f(
                1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f
            ).identity()
        )
        assertEquals(
            Matrix4d(), Matrix4d(
                1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0,
                9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0
            ).identity()
        )
    }

    @Test
    fun testTranslatingPositions() {
        testMatrixTransform(
            Matrix3x2f().translate(1f, 2f), ::Vector2f,
            { m, v -> m.transformPosition(v) }, {
                it.add(1f, 2f)
            })
        testMatrixTransform(
            Matrix3x2f().translate(1f, 2f), ::Vector2f,
            { m, v -> m.transformPosition(v.x, v.y, v) }, {
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
            Matrix3x2d().translate(1.0, 2.0), ::Vector2d,
            { m, v -> m.transformPosition(v.x, v.y, v) }, {
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
        // todo check mulTranspose...
        testMatrixTransform(
            Matrix3x2f().translate(1f, 2f), ::Vector2f,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix3x2f().translate(1f, 2f), ::Vector2f,
            { m, v -> m.transformDirection(v.x, v.y, v) }, { it })
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
            Matrix3x2d().translate(1.0, 2.0), ::Vector2d,
            { m, v -> m.transformDirection(v.x, v.y, v) }, { it })
        testMatrixTransform(
            Matrix4x3().translate(1.0, 2.0, 3.0), ::Vector3f,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix4x3().translate(1.0, 2.0, 3.0), ::Vector3d,
            { m, v -> m.transformDirection(v) }, { it })
        testMatrixTransform(
            Matrix4x3d().translate(1.0, 2.0, 3.0), ::Vector3f,
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
            Matrix4x3().scale(2f, -3f, 3f), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2f, -3f, 3f)
            })
        testMatrixTransform(
            Matrix4x3().scale(2f, -3f, 3f), ::Vector3d,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.mul(2.0, -3.0, 3.0)
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
    fun testScaleAround() {
        testMatrixTransform(
            Matrix3x2f().scaleAround(2f, -3f, 5f, 6f), ::Vector2f,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5f, 6f).mul(2f, -3f).add(5f, 6f)
            })
        testMatrixTransform(
            Matrix4x3f().scaleAround(2f, -3f, 3f, 5f, 6f, 7f), ::Vector3f,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5f, 6f, 7f).mul(2f, -3f, 3f).add(5f, 6f, 7f)
            })
        testMatrixTransform(
            Matrix4f().scaleAround(2f, -3f, 3f, 5f, 6f, 7f), ::Vector3f,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5f, 6f, 7f).mul(2f, -3f, 3f).add(5f, 6f, 7f)
            })
        testMatrixTransform(
            Matrix3x2d().scaleAroundLocal(2.0, -3.0, 5.0, 6.0), ::Vector2d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5.0, 6.0).mul(2.0, -3.0).add(5.0, 6.0)
            })
        testMatrixTransform(
            Matrix4x3().scaleAround(2f, -3f, 3f, 5.0, 6.0, 7.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5.0, 6.0, 7.0).mul(2.0, -3.0, 3.0).add(5.0, 6.0, 7.0)
            })
        testMatrixTransform(
            Matrix4x3d().scaleAround(2.0, -3.0, 3.0, 5.0, 6.0, 7.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5.0, 6.0, 7.0).mul(2.0, -3.0, 3.0).add(5.0, 6.0, 7.0)
            })
        testMatrixTransform(
            Matrix4d().scaleAround(2.0, -3.0, 3.0, 5.0, 6.0, 7.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5.0, 6.0, 7.0).mul(2.0, -3.0, 3.0).add(5.0, 6.0, 7.0)
            })
    }

    @Test
    fun testRotating() {
        testMatrixTransform(
            Matrix2f().rotate(5f), ::Vector2f,
            { m, v -> m.transform(v) }, {
                it.rotate(5f)
            })
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
            Matrix4x3().rotateX(1f).rotateY(2f).rotateZ(3f), ::Vector3f,
            { m, v -> m.transformPosition(v) },
            { m, v -> m.transformDirection(v) }, {
                it.rotateZ(3f).rotateY(2f).rotateX(1f)
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
    fun testRotateAround() {
        testMatrixTransform(
            Matrix3x2f().rotateAbout(5f, 6f, 7f), ::Vector2f,
            { m, v -> m.transformPosition(v) }, {
                it.sub(6f, 7f)
                    .rotate(5f)
                    .add(6f, 7f)
            })
        val qf = Quaternionf().rotateX(1f).rotateY(2f).rotateZ(3f)
        val qd = Quaterniond().rotateX(1.0).rotateY(2.0).rotateZ(3.0)
        testMatrixTransform(
            Matrix4x3f().rotateAround(qf, 5f, 6f, 7f), ::Vector3f,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5f, 6f, 7f)
                    .rotateZ(3f).rotateY(2f).rotateX(1f)
                    .add(5f, 6f, 7f)
            })
        testMatrixTransform(
            Matrix4f().rotateAround(qf, 5f, 6f, 7f), ::Vector3f,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5f, 6f, 7f)
                    .rotateZ(3f).rotateY(2f).rotateX(1f)
                    .add(5f, 6f, 7f)
            })
        testMatrixTransform(
            Matrix3x2d().rotateAbout(5.0, 6.0, 7.0), ::Vector2d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(6.0, 7.0).rotate(5.0).add(6.0, 7.0)
            })
        testMatrixTransform(
            Matrix4x3().rotateAround(qf, 5.0, 6.0, 7.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5.0, 6.0, 7.0)
                    .rotateZ(3.0).rotateY(2.0).rotateX(1.0)
                    .add(5.0, 6.0, 7.0)
            })
        testMatrixTransform(
            Matrix4x3d().rotateAround(qd, 5.0, 6.0, 7.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5.0, 6.0, 7.0)
                    .rotateZ(3.0).rotateY(2.0).rotateX(1.0)
                    .add(5.0, 6.0, 7.0)
            })
        testMatrixTransform(
            Matrix4d().rotateAround(qd, 5.0, 6.0, 7.0), ::Vector3d,
            { m, v -> m.transformPosition(v) }, {
                it.sub(5.0, 6.0, 7.0)
                    .rotateZ(3.0).rotateY(2.0).rotateX(1.0)
                    .add(5.0, 6.0, 7.0)
            })
    }

    @Test
    fun testTransformTranspose() {
        val rnd3f = Matrix3f(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f)
        testMatrixTransform(Matrix3f(rnd3f).transpose(), ::Vector3f, { m, v -> v.mulTranspose(m) }, { it.mul(rnd3f) })
        val rnd3d = Matrix3d(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
        testMatrixTransform(Matrix3d(rnd3d).transpose(), ::Vector3d, { m, v -> v.mulTranspose(m) }, { it.mul(rnd3d) })
    }

    @Test
    fun testTranspose() {
        assertEquals(
            Matrix2f(1f, 2f, 3f, 4f),
            Matrix2f(1f, 3f, 2f, 4f).transpose()
        )
        assertEquals(
            Matrix3f(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f),
            Matrix3f(1f, 4f, 7f, 2f, 5f, 8f, 3f, 6f, 9f).transpose()
        )
        assertEquals(
            Matrix4f(
                1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f
            ),
            Matrix4f(
                1f, 5f, 9f, 13f, 2f, 6f, 10f, 14f,
                3f, 7f, 11f, 15f, 4f, 8f, 12f, 16f
            ).transpose()
        )
        assertEquals(
            Matrix4x3f(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f),
            Matrix4x3f(1f, 4f, 7f, 2f, 5f, 8f, 3f, 6f, 9f, 10f, 11f, 12f).transpose3x3()
        )
        assertEquals(
            Matrix3f(1f, 4f, 7f, 2f, 5f, 8f, 3f, 6f, 9f),
            Matrix4x3f(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f).transpose3x3(Matrix3f()),
        )
        assertEquals(
            Matrix2d(1.0, 2.0, 3.0, 4.0),
            Matrix2d(1.0, 3.0, 2.0, 4.0).transpose()
        )
        assertEquals(
            Matrix3d(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0),
            Matrix3d(1.0, 4.0, 7.0, 2.0, 5.0, 8.0, 3.0, 6.0, 9.0).transpose()
        )
        assertEquals(
            Matrix4d(
                1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0,
                9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0
            ),
            Matrix4d(
                1.0, 5.0, 9.0, 13.0, 2.0, 6.0, 10.0, 14.0,
                3.0, 7.0, 11.0, 15.0, 4.0, 8.0, 12.0, 16.0
            ).transpose()
        )
        assertEquals(
            Matrix4x3(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10.0, 11.0, 12.0),
            Matrix4x3(1f, 4f, 7f, 2f, 5f, 8f, 3f, 6f, 9f, 10.0, 11.0, 12.0).transpose3x3()
        )
        assertEquals(
            Matrix4x3d(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0),
            Matrix4x3d(1.0, 4.0, 7.0, 2.0, 5.0, 8.0, 3.0, 6.0, 9.0, 10.0, 11.0, 12.0).transpose3x3()
        )
        assertEquals(
            Matrix3d(1.0, 4.0, 7.0, 2.0, 5.0, 8.0, 3.0, 6.0, 9.0),
            Matrix4x3d(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0).transpose3x3(Matrix3d()),
        )
    }

    @Test
    fun testAxisAngle() {
        for (aa in getAxisAnglesF()) {
            testMatrixTransform(Matrix3f().set(aa), ::Vector3f, { m, v -> m.transform(v) }, { aa.transform(it) })
            testMatrixTransform(Matrix4f().set(aa), ::Vector4f, { m, v -> m.transform(v) }, { aa.transform(it) })
            testMatrixTransform(
                Matrix4x3f().set(aa), ::Vector3f,
                { m, v -> m.transformPosition(v) },
                { aa.transform(it) })
            testMatrixTransform(
                Matrix4x3f().set(aa), ::Vector3f,
                { m, v -> m.transformDirection(v) },
                { aa.transform(it) })
            testMatrixTransform(Matrix4x3f().set(aa), ::Vector4f, { m, v -> m.transform(v) }, { aa.transform(it) })

            testMatrixTransform(
                Matrix4x3().set(aa), ::Vector3f,
                { m, v -> m.transformPosition(v) },
                { aa.transform(it) })
            testMatrixTransform(
                Matrix4x3().set(aa), ::Vector3f,
                { m, v -> m.transformDirection(v) },
                { aa.transform(it) })
            testMatrixTransform(Matrix4x3().set(aa), ::Vector4f, { m, v -> m.transform(v) }, { aa.transform(it) })
        }
        for (aa in getAxisAnglesD()) {
            testMatrixTransform(Matrix3d().set(aa), ::Vector3d, { m, v -> m.transform(v) }, { aa.transform(it) })
            testMatrixTransform(Matrix4d().set(aa), ::Vector4d, { m, v -> m.transform(v) }, { aa.transform(it) })
            testMatrixTransform(
                Matrix4x3d().set(aa), ::Vector3d,
                { m, v -> m.transformPosition(v) },
                { aa.transform(it) })
            testMatrixTransform(
                Matrix4x3d().set(aa), ::Vector3d,
                { m, v -> m.transformDirection(v) },
                { aa.transform(it) })
            testMatrixTransform(Matrix4x3d().set(aa), ::Vector4d, { m, v -> m.transform(v) }, { aa.transform(it) })
        }
    }

    // todo test all chaining operations,
    //  so translation, rotation (quaternion/axis-angle), scale

    fun getAxisAnglesF(): List<AxisAngle4f> {
        return getAxisAnglesD().map { AxisAngle4f().set(it) }
    }

    fun getAxisAnglesD(): List<AxisAngle4d> {
        val q = sqrt(0.5)
        return listOf(
            AxisAngle4d(1.0, 1.0, 0.0, 0.0),
            AxisAngle4d(2.0, 1.0, 0.0, 0.0),
            AxisAngle4d(-2.0, 1.0, 0.0, 0.0),
            AxisAngle4d(1.0, 0.0, 1.0, 0.0),
            AxisAngle4d(2.0, 0.0, 1.0, 0.0),
            AxisAngle4d(-2.0, 0.0, 1.0, 0.0),
            AxisAngle4d(1.0, 0.0, 0.0, 1.0),
            AxisAngle4d(2.0, 0.0, 0.0, 1.0),
            AxisAngle4d(-2.0, 0.0, 0.0, 1.0),
            AxisAngle4d(1.0, -1.0, 0.0, 0.0),
            AxisAngle4d(2.0, -1.0, 0.0, 0.0),
            AxisAngle4d(-2.0, -1.0, 0.0, 0.0),
            AxisAngle4d(1.0, 0.0, -1.0, 0.0),
            AxisAngle4d(2.0, 0.0, -1.0, 0.0),
            AxisAngle4d(-2.0, 0.0, -1.0, 0.0),
            AxisAngle4d(1.0, 0.0, 0.0, -1.0),
            AxisAngle4d(2.0, 0.0, 0.0, -1.0),
            AxisAngle4d(-2.0, 0.0, 0.0, -1.0),
            AxisAngle4d(1.0, q, q, 0.0),
            AxisAngle4d(1.0, q, 0.0, q),
            AxisAngle4d(1.0, 0.0, q, q),
        )
    }

    fun <M : Any, V : Vector> testMatrixTransform(
        matrix: M,
        createVector: () -> V,
        transform: (M, V) -> V,
        transformManually: (V) -> V
    ) {
        testMatrixTransform(
            matrix, createVector,
            transform, null,
            transformManually
        )
    }

    fun <M : Any, V : Vector> testMatrixTransform(
        matrix: M,
        createVector: () -> V,
        transform1: (M, V) -> V,
        transform2: ((M, V) -> V)?,
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
            if (transform2 != null) {
                val actually2 = transform2(matrix, c)
                assertEquals(expected, actually2, 1e-4)
            }
        }
    }
}