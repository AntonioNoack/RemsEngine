package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.AABBd
import org.joml.AxisAngle4d
import org.joml.AxisAngle4f
import org.joml.Matrix
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
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Check that MatrixF and MatrixD operations are the same
 * */
class MatrixEquivalenceTests {

    fun <M : Matrix<*, *, *>> M.fill(seed: Long = 1234): M {
        val random = Random(seed)
        for (i in 0 until numCols * numRows) {
            this[i / numRows, i % numRows] = random.nextDouble()
        }
        return this
    }

    fun <M : Vector> M.fill(seed: Long = 1234): M {
        val random = Random(seed)
        for (i in 0 until numComponents) {
            setComp(i, random.nextDouble())
        }
        return this
    }

    @Test
    fun testIdentity() {
        assertEquals1(Matrix4f(), Matrix4d())
        assertEquals1(
            Matrix4f().fill().identity(),
            Matrix4d().fill().identity()
        )
        assertEquals1(
            Matrix4f().fill().zero(),
            Matrix4d().fill().zero()
        )
    }

    @Test
    fun testTranslate() {
        assertEquals1(
            Matrix3x2f().translate(Vector2f().fill(17)),
            Matrix3x2d().translate(Vector2d().fill(17))
        )
        assertEquals1(
            Matrix3x2f().fill().translate(Vector2f().fill(17)),
            Matrix3x2d().fill().translate(Vector2d().fill(17)), 2e-7
        )
        assertEquals1(
            Matrix4x3f().translate(Vector3f().fill(17)),
            Matrix4x3d().translate(Vector3d().fill(17))
        )
        assertEquals1(
            Matrix4x3f().fill().translate(Vector3f().fill(17)),
            Matrix4x3d().fill().translate(Vector3d().fill(17))
        )
        assertEquals1(
            Matrix4f().translate(Vector3f().fill(17)),
            Matrix4d().translate(Vector3d().fill(17))
        )
        assertEquals1(
            Matrix4f().fill().translate(Vector3f().fill(17)),
            Matrix4d().fill().translate(Vector3d().fill(17))
        )
    }

    @Test
    fun testTranslateLocal() {
        assertEquals1(
            Matrix3x2f().translateLocal(Vector2f().fill(17)),
            Matrix3x2d().translateLocal(Vector2d().fill(17))
        )
        assertEquals1(
            Matrix3x2f().fill().translateLocal(Vector2f().fill(17)),
            Matrix3x2d().fill().translateLocal(Vector2d().fill(17))
        )
        assertEquals1(
            Matrix4x3f().translateLocal(Vector3f().fill(17)),
            Matrix4x3d().translateLocal(Vector3d().fill(17))
        )
        assertEquals1(
            Matrix4x3f().fill().translateLocal(Vector3f().fill(17)),
            Matrix4x3d().fill().translateLocal(Vector3d().fill(17))
        )
        assertEquals1(
            Matrix4f().translateLocal(Vector3f().fill(17)),
            Matrix4d().translateLocal(Vector3d().fill(17))
        )
        assertEquals1(
            Matrix4f().fill().translateLocal(Vector3f().fill(17)),
            Matrix4d().fill().translateLocal(Vector3d().fill(17))
        )
    }

    @Test
    fun testRotate() {
        assertEquals1(
            Matrix2f().fill(1234).rotate(1f),
            Matrix2d().fill(1234).rotate(1.0)
        )
        assertEquals1(
            Matrix3x2f().fill(1234).rotate(1f),
            Matrix3x2d().fill(1234).rotate(1.0)
        )
        assertEquals1(
            Matrix3f().fill(1234).rotate(Quaternionf().fill(4567)),
            Matrix3d().fill(1234).rotate(Quaterniond().fill(4567))
        )
        assertEquals1(
            Matrix4x3f().fill(1234).rotate(Quaternionf().fill(4567)),
            Matrix4x3d().fill(1234).rotate(Quaterniond().fill(4567))
        )
        assertEquals1(
            Matrix4f().fill(1234).rotate(Quaternionf().fill(4567)),
            Matrix4d().fill(1234).rotate(Quaterniond().fill(4567))
        )
    }

    @Test
    fun testRotateTranslation() {
        assertEquals1(
            Matrix4x3f().fill(1234).rotateTranslation(Quaternionf().fill(4567)),
            Matrix4x3d().fill(1234).rotateTranslation(Quaterniond().fill(4567))
        )
        assertEquals1(
            Matrix4x3f().fill(1234).rotateTranslation(1f, 2f, 3f, 4f),
            Matrix4x3d().fill(1234).rotateTranslation(1.0, 2.0, 3.0, 4.0), 1e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateTranslation(Quaternionf().fill(4567)),
            Matrix4d().fill(1234).rotateTranslation(Quaterniond().fill(4567)), 2e-7
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateTranslation(1f, 2f, 3f, 4f),
            Matrix4d().fill(1234).rotateTranslation(1.0, 2.0, 3.0, 4.0), 1e-6
        )
    }

    @Test
    fun testRotateAffine() {
        assertEquals1(
            Matrix4f().fill(1234).rotateAffine(Quaternionf().fill(4567)),
            Matrix4d().fill(1234).rotateAffine(Quaterniond().fill(4567))
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateAffine(1f, 2f, 3f, 4f),
            Matrix4d().fill(1234).rotateAffine(1.0, 2.0, 3.0, 4.0), 1e-5
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateAffineXYZ(1f, 2f, 3f),
            Matrix4d().fill(1234).rotateAffineXYZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateAffineYXZ(1f, 2f, 3f),
            Matrix4d().fill(1234).rotateAffineYXZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateAffineZYX(1f, 2f, 3f),
            Matrix4d().fill(1234).rotateAffineZYX(1.0, 2.0, 3.0)
        )
    }

    @Test
    fun testRotate2() {
        assertEquals1(Matrix3f().fill(1234).rotateX(1f), Matrix3d().fill(1234).rotateX(1.0))
        assertEquals1(Matrix3f().fill(1234).rotateY(1f), Matrix3d().fill(1234).rotateY(1.0))
        assertEquals1(Matrix3f().fill(1234).rotateZ(1f), Matrix3d().fill(1234).rotateZ(1.0))
        assertEquals1(Matrix4x3f().fill(1234).rotateX(1f), Matrix4x3d().fill(1234).rotateX(1.0))
        assertEquals1(Matrix4x3f().fill(1234).rotateY(1f), Matrix4x3d().fill(1234).rotateY(1.0))
        assertEquals1(Matrix4x3f().fill(1234).rotateZ(1f), Matrix4x3d().fill(1234).rotateZ(1.0))
        assertEquals1(Matrix4f().fill(1234).rotateX(1f), Matrix4d().fill(1234).rotateX(1.0))
        assertEquals1(Matrix4f().fill(1234).rotateY(1f), Matrix4d().fill(1234).rotateY(1.0))
        assertEquals1(Matrix4f().fill(1234).rotateZ(1f), Matrix4d().fill(1234).rotateZ(1.0))
    }

    @Test
    fun testScale() {
        assertEquals1(
            Matrix2f().fill(1234).scale(1f, 2f),
            Matrix2d().fill(1234).scale(1.0, 2.0)
        )
        assertEquals1(
            Matrix3x2f().fill(1234).scale(1f, 2f),
            Matrix3x2d().fill(1234).scale(1.0, 2.0)
        )
        assertEquals1(
            Matrix3f().fill(1234).scale(Vector3f().fill(4567)),
            Matrix3d().fill(1234).scale(Vector3d().fill(4567))
        )
        assertEquals1(
            Matrix4x3f().fill(1234).scale(Vector3f().fill(4567)),
            Matrix4x3d().fill(1234).scale(Vector3d().fill(4567))
        )
        assertEquals1(
            Matrix4f().fill(1234).scale(Vector3f().fill(4567)),
            Matrix4d().fill(1234).scale(Vector3d().fill(4567))
        )
    }

    @Test
    fun testRotateLocalQuat() {
        assertEquals1(
            Matrix2f().fill(1234).rotateLocal(1f),
            Matrix2d().fill(1234).rotateLocal(1.0)
        )
        assertEquals1(
            Matrix3x2f().fill(1234).rotateLocal(1f),
            Matrix3x2d().fill(1234).rotateLocal(1.0)
        )
        assertEquals1(
            Matrix3f().fill(1234).rotateLocal(Quaternionf().fill(4567)),
            Matrix3d().fill(1234).rotateLocal(Quaterniond().fill(4567)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill(1234).rotateLocal(Quaternionf().fill(4567)),
            Matrix4x3d().fill(1234).rotateLocal(Quaterniond().fill(4567)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateLocal(Quaternionf().fill(4567)),
            Matrix4d().fill(1234).rotateLocal(Quaterniond().fill(4567)), 1e-6
        )
    }

    @Test
    fun testRotateLocalAxisAngle() {
        assertEquals1(
            Matrix3f().fill(1234).rotateLocal(1f, 2f, 3f, 4f),
            Matrix3d().fill(1234).rotateLocal(1.0, 2.0, 3.0, 4.0), 2e-6
        )
        assertEquals1(
            Matrix4x3f().fill(1234).rotateLocal(1f, 2f, 3f, 4f),
            Matrix4x3d().fill(1234).rotateLocal(1.0, 2.0, 3.0, 4.0), 2e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateLocal(1f, 2f, 3f, 4f),
            Matrix4d().fill(1234).rotateLocal(1.0, 2.0, 3.0, 4.0), 2e-6
        )
    }

    @Test
    fun testRotateLocalXYZ() {
        assertEquals1(
            Matrix3f().fill(1234).rotateLocalX(1f),
            Matrix3d().fill(1234).rotateLocalX(1.0), 1e-6
        )
        assertEquals1(
            Matrix3f().fill(1234).rotateLocalY(1f),
            Matrix3d().fill(1234).rotateLocalY(1.0), 1e-6
        )
        assertEquals1(
            Matrix3f().fill(1234).rotateLocalZ(1f),
            Matrix3d().fill(1234).rotateLocalZ(1.0), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill(1234).rotateLocalX(1f),
            Matrix4x3d().fill(1234).rotateLocalX(1.0), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill(1234).rotateLocalY(1f),
            Matrix4x3d().fill(1234).rotateLocalY(1.0), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill(1234).rotateLocalZ(1f),
            Matrix4x3d().fill(1234).rotateLocalZ(1.0), 1e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateLocalX(1f),
            Matrix4d().fill(1234).rotateLocalX(1.0), 1e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateLocalY(1f),
            Matrix4d().fill(1234).rotateLocalY(1.0), 1e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateLocalZ(1f),
            Matrix4d().fill(1234).rotateLocalZ(1.0), 1e-6
        )
    }

    @Test
    fun testScaleLocal() {
        assertEquals1(
            Matrix2f().fill(1234).scaleLocal(1f, 2f),
            Matrix2d().fill(1234).scaleLocal(1.0, 2.0), 1e-6
        )
        assertEquals1(
            Matrix3x2f().fill(1234).scaleLocal(1f, 2f),
            Matrix3x2d().fill(1234).scaleLocal(1.0, 2.0), 1e-6
        )
        assertEquals1(
            Matrix3f().fill(1234).scaleLocal(Vector3f().fill(4567)),
            Matrix3d().fill(1234).scaleLocal(Vector3d().fill(4567)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill(1234).scaleLocal(Vector3f().fill(4567)),
            Matrix4x3d().fill(1234).scaleLocal(Vector3d().fill(4567)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).scaleLocal(Vector3f().fill(4567)),
            Matrix4d().fill(1234).scaleLocal(Vector3d().fill(4567)), 1e-6
        )
    }

    @Test
    fun testRotateAround() {
        assertEquals1(
            Matrix4x3f().fill(1234).rotateAround(Quaternionf().fill(4567), 1f, 2f, 3f),
            Matrix4x3d().fill(1234).rotateAround(Quaterniond().fill(4567), 1.0, 2.0, 3.0),
            1e-6
        )
        assertEquals1(
            Matrix4f().fill(1234).rotateAround(Quaternionf().fill(4567), 1f, 2f, 3f),
            Matrix4d().fill(1234).rotateAround(Quaterniond().fill(4567), 1.0, 2.0, 3.0),
            1e-6
        )
    }

    @Test
    fun testRotateAroundAffine() {
        assertEquals1(
            Matrix4f().fill(1234).rotateAroundAffine(Quaternionf().fill(4567), 1f, 2f, 3f),
            Matrix4d().fill(1234).rotateAroundAffine(Quaterniond().fill(4567), 1.0, 2.0, 3.0),
            1e-6
        )
    }

    @Test
    fun testRotateAroundLocal() {
        assertEquals1(
            Matrix4f().fill(1234).rotateAroundLocal(Quaternionf().fill(4567), 1f, 2f, 3f),
            Matrix4d().fill(1234).rotateAroundLocal(Quaterniond().fill(4567), 1.0, 2.0, 3.0), 1e-6
        )
    }

    @Test
    fun testScaleAround() {
        assertEquals1(
            Matrix3x2f().fill(1234).scaleAround(4f, 5f, 6f, 1f),
            Matrix3x2d().fill(1234).scaleAround(4.0, 5.0, 6.0, 1.0), 1e-5
        )
        assertEquals1(
            Matrix4x3f().fill(1234).scaleAround(4f, 5f, 6f, 1f),
            Matrix4x3d().fill(1234).scaleAround(4.0, 5.0, 6.0, 1.0), 1e-5
        )
        assertEquals1(
            Matrix4f().fill(1234).scaleAround(4f, 5f, 6f, 1f, 2f, 3f),
            Matrix4d().fill(1234).scaleAround(4.0, 5.0, 6.0, 1.0, 2.0, 3.0), 1e-5
        )
    }

    @Test
    fun testScaleAroundLocal() {
        assertEquals1(
            Matrix4f().fill(1234).scaleAroundLocal(4f, 5f, 6f, 1f, 2f, 3f),
            Matrix4d().fill(1234).scaleAroundLocal(4.0, 5.0, 6.0, 1.0, 2.0, 3.0), 1e-6
        )
    }

    @Test
    fun testPerspective() {
        val rnd = Random(12345)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val e = rnd.nextDouble()
        val f = rnd.nextDouble()
        assertEquals1(
            Matrix4f().perspective(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().perspective(a, b, c, d), 1e-5
        )
        assertEquals1(
            Matrix4f().fill().perspective(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().fill().perspective(a, b, c, d), 1e-5
        )
        assertEquals1(
            Matrix4f().perspectiveRect(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().perspectiveRect(a, b, c, d), 1e-5
        )
        assertEquals1(
            Matrix4f().fill().perspectiveRect(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().fill().perspectiveRect(a, b, c, d), 1e-5
        )
        assertEquals1(
            Matrix4f().perspectiveOffCenter(
                a.toFloat(), b.toFloat(), c.toFloat(),
                d.toFloat(), e.toFloat(), f.toFloat()
            ),
            Matrix4d().perspectiveOffCenter(a, b, c, d, e, f), 1e-5
        )
        assertEquals1(
            Matrix4f().fill().perspectiveOffCenter(
                a.toFloat(), b.toFloat(), c.toFloat(),
                d.toFloat(), e.toFloat(), f.toFloat()
            ),
            Matrix4d().fill().perspectiveOffCenter(a, b, c, d, e, f), 1e-5
        )
        assertEquals1(
            Matrix4f().perspectiveOffCenterFov(
                a.toFloat(), b.toFloat(), c.toFloat(),
                d.toFloat(), e.toFloat(), f.toFloat()
            ),
            Matrix4d().perspectiveOffCenterFov(a, b, c, d, e, f), 1e-5
        )
        assertEquals1(
            Matrix4f().fill().perspectiveOffCenterFov(
                a.toFloat(), b.toFloat(), c.toFloat(),
                d.toFloat(), e.toFloat(), f.toFloat()
            ),
            Matrix4d().fill().perspectiveOffCenterFov(a, b, c, d, e, f), 1e-5
        )
        assertEquals1(
            Matrix4f().perspectiveOffCenterFovLH(
                a.toFloat(), b.toFloat(), c.toFloat(),
                d.toFloat(), e.toFloat(), f.toFloat()
            ),
            Matrix4d().perspectiveOffCenterFovLH(a, b, c, d, e, f), 1e-5
        )
        assertEquals1(
            Matrix4f().fill().perspectiveOffCenterFovLH(
                a.toFloat(), b.toFloat(), c.toFloat(),
                d.toFloat(), e.toFloat(), f.toFloat()
            ),
            Matrix4d().fill().perspectiveOffCenterFovLH(a, b, c, d, e, f), 1e-5
        )
    }

    @Test
    fun testReflect3() {
        val normal = Vector3f().fill().normalize()
        val dir = Quaternionf().fill()
        assertEquals1(
            Matrix3f().fill().reflect(normal),
            Matrix3d().fill().reflect(Vector3d(normal)), 1e-6
        )
        assertEquals1(
            Matrix3f().fill().reflect(dir),
            Matrix3d().fill().reflect(Quaterniond(dir)), 1e-6
        )
        assertEquals1(
            Matrix3f().reflection(normal),
            Matrix3d().reflection(Vector3d(normal)), 1e-6
        )
        assertEquals1(
            Matrix3f().reflection(dir),
            Matrix3d().reflection(Quaterniond(dir)), 1e-6
        )
    }

    @Test
    fun testReflect4() {
        val normal = Vector3f().fill().normalize()
        val pos = Vector3f().fill()
        val dir = Quaternionf().fill()
        assertEquals1(
            Matrix4x3f().fill().reflect(normal, pos),
            Matrix4x3d().fill().reflect(Vector3d(normal), Vector3d(pos)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill().reflect(dir, pos),
            Matrix4x3d().fill().reflect(Quaterniond(dir), Vector3d(pos)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().reflection(normal, pos),
            Matrix4x3d().reflection(Vector3d(normal), Vector3d(pos)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().reflection(dir, pos),
            Matrix4x3d().reflection(Quaterniond(dir), Vector3d(pos)), 1e-6
        )

        assertEquals1(
            Matrix4f().fill().reflect(normal, pos),
            Matrix4d().fill().reflect(Vector3d(normal), Vector3d(pos)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill().reflect(dir, pos),
            Matrix4d().fill().reflect(Quaterniond(dir), Vector3d(pos)), 1e-6
        )
        assertEquals1(
            Matrix4f().reflection(normal, pos),
            Matrix4d().reflection(Vector3d(normal), Vector3d(pos)), 1e-6
        )
        assertEquals1(
            Matrix4f().reflection(dir, pos),
            Matrix4d().reflection(Quaterniond(dir), Vector3d(pos)), 1e-6
        )
    }

    @Test
    fun testOrtho() {
        val rnd = Random(12345)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val e = rnd.nextDouble()
        val f = rnd.nextDouble()
        assertEquals1(
            Matrix4x3f().ortho(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4x3d().ortho(a, b, c, d, e, f), 2e-5
        )
        assertEquals1(
            Matrix4x3f().orthoLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4x3d().orthoLH(a, b, c, d, e, f), 2e-5
        )
        assertEquals1(
            Matrix4x3f().ortho2D(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4x3d().ortho2D(a, b, c, d), 2e-5
        )
        assertEquals1(
            Matrix4x3f().ortho2DLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4x3d().ortho2DLH(a, b, c, d), 2e-5
        )
        assertEquals1(
            Matrix4x3f().orthoSymmetric(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4x3d().orthoSymmetric(a, b, c, d), 2e-5
        )
        assertEquals1(
            Matrix4x3f().orthoSymmetricLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4x3d().orthoSymmetricLH(a, b, c, d), 2e-5
        )
        assertEquals1(
            Matrix4f().ortho(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4d().ortho(a, b, c, d, e, f), 2e-5
        )
        assertEquals1(
            Matrix4f().orthoLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4d().orthoLH(a, b, c, d, e, f), 2e-5
        )
        assertEquals1(
            Matrix4f().ortho2D(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().ortho2D(a, b, c, d), 2e-5
        )
        assertEquals1(
            Matrix4f().ortho2DLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().ortho2DLH(a, b, c, d), 2e-5
        )
        assertEquals1(
            Matrix4f().orthoCrop(Matrix4f().fill()),
            Matrix4d().orthoCrop(Matrix4d().fill()), 2e-5
        )
        assertEquals1(
            Matrix4f().orthoSymmetric(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().orthoSymmetric(a, b, c, d), 2e-5
        )
        assertEquals1(
            Matrix4f().orthoSymmetricLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix4d().orthoSymmetricLH(a, b, c, d), 2e-5
        )
    }

    @Test
    fun testLookAlong() {
        val dir = Vector3f().fill(213).normalize()
        val up = Vector3f().fill(456).normalize()
        assertEquals1(
            Matrix4x3f().lookAlong(dir, up),
            Matrix4x3d().lookAlong(Vector3d(dir), Vector3d(up))
        )
        assertEquals1(
            Matrix4x3f().fill().lookAlong(dir, up),
            Matrix4x3d().fill().lookAlong(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4f().lookAlong(dir, up),
            Matrix4d().lookAlong(Vector3d(dir), Vector3d(up))
        )
        assertEquals1(
            Matrix4f().fill().lookAlong(dir, up),
            Matrix4d().fill().lookAlong(Vector3d(dir), Vector3d(up)), 1e-6
        )
    }

    @Test
    fun testLookAt() {
        val eye = Vector3f().fill(213)
        val center = Vector3f().fill(561)
        val up = Vector3f().fill(456).normalize()
        assertEquals1(
            Matrix4x3f().lookAt(eye, center, up),
            Matrix4x3d().lookAt(Vector3d(eye), Vector3d(center), Vector3d(up))
        )
        assertEquals1(
            Matrix4x3f().fill().lookAt(eye, center, up),
            Matrix4x3d().fill().lookAt(Vector3d(eye), Vector3d(center), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().lookAtLH(eye, center, up),
            Matrix4x3d().lookAtLH(Vector3d(eye), Vector3d(center), Vector3d(up))
        )
        assertEquals1(
            Matrix4x3f().fill().lookAtLH(eye, center, up),
            Matrix4x3d().fill().lookAtLH(Vector3d(eye), Vector3d(center), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4f().lookAt(eye, center, up),
            Matrix4d().lookAt(Vector3d(eye), Vector3d(center), Vector3d(up))
        )
        assertEquals1(
            Matrix4f().fill().lookAt(eye, center, up),
            Matrix4d().fill().lookAt(Vector3d(eye), Vector3d(center), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4f().lookAtLH(eye, center, up),
            Matrix4d().lookAtLH(Vector3d(eye), Vector3d(center), Vector3d(up))
        )
        assertEquals1(
            Matrix4f().fill().lookAtLH(eye, center, up),
            Matrix4d().fill().lookAtLH(Vector3d(eye), Vector3d(center), Vector3d(up)), 1e-6
        )
    }

    @Test
    fun testTile() {
        assertEquals1(
            Matrix4f().tile(10, 8, 100, 200),
            Matrix4d().tile(10, 8, 100, 200)
        )
        assertEquals1(
            Matrix4f().fill().tile(10, 8, 100, 200),
            Matrix4d().fill().tile(10, 8, 100, 200), 1e-4
        )
    }

    @Test
    fun testFrustum() {
        val rnd = Random(12345)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val e = rnd.nextDouble()
        val f = rnd.nextDouble()
        assertEquals1(
            Matrix4f().frustum(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4d().frustum(a, b, c, d, e, f), 1e-4
        )
        assertEquals1(
            Matrix4f().fill().frustum(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4d().fill().frustum(a, b, c, d, e, f), 1e-4
        )
        assertEquals1(
            Matrix4f().frustumLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4d().frustumLH(a, b, c, d, e, f), 1e-4
        )
        assertEquals1(
            Matrix4f().fill().frustumLH(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4d().fill().frustumLH(a, b, c, d, e, f), 1e-4
        )
    }

    @Test
    fun testSetFromIntrinsics() {
        val rnd = Random(12345)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val e = rnd.nextDouble()
        val f = rnd.nextDouble()
        val g = rnd.nextDouble()
        val w = rnd.nextInt()
        val h = rnd.nextInt()
        assertEquals1(
            Matrix4f().setFromIntrinsic(
                a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(),
                w, h, f.toFloat(), g.toFloat()
            ),
            Matrix4d().setFromIntrinsic(a, b, c, d, e, w, h, f, g), 1e-4
        )
    }

    @Test
    fun testShadow() {
        val rnd = Random(12345)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val e = rnd.nextDouble()
        val f = rnd.nextDouble()
        val g = rnd.nextDouble()
        val h = rnd.nextDouble()
        assertEquals1(
            Matrix4x3f().shadow(
                a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(),
                e.toFloat(), f.toFloat(), g.toFloat(), h.toFloat()
            ),
            Matrix4x3d().shadow(a, b, c, d, e, f, g, h), 1e-6
        )
        assertEquals1(
            Matrix4f().shadow(
                a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(),
                e.toFloat(), f.toFloat(), g.toFloat(), h.toFloat()
            ),
            Matrix4d().shadow(a, b, c, d, e, f, g, h), 1e-6
        )
    }

    @Test
    fun testBillboard() {
        val pos1 = Vector3f().fill(123)
        val pos2 = Vector3f().fill(456)
        val up = Vector3f().fill(987).normalize()
        assertEquals1(
            Matrix4x3f().billboardCylindrical(pos1, pos2, up),
            Matrix4x3d().billboardCylindrical(Vector3d(pos1), Vector3d(pos2), Vector3d(up))
        )
        assertEquals1(
            Matrix4x3f().billboardSpherical(pos1, pos2),
            Matrix4x3d().billboardSpherical(Vector3d(pos1), Vector3d(pos2))
        )
        assertEquals1(
            Matrix4x3f().billboardSpherical(pos1, pos2, up),
            Matrix4x3d().billboardSpherical(Vector3d(pos1), Vector3d(pos2), Vector3d(up))
        )
        assertEquals1(
            Matrix4f().billboardCylindrical(pos1, pos2, up),
            Matrix4d().billboardCylindrical(Vector3d(pos1), Vector3d(pos2), Vector3d(up))
        )
        assertEquals1(
            Matrix4f().billboardSpherical(pos1, pos2),
            Matrix4d().billboardSpherical(Vector3d(pos1), Vector3d(pos2))
        )
        assertEquals1(
            Matrix4f().billboardSpherical(pos1, pos2, up),
            Matrix4d().billboardSpherical(Vector3d(pos1), Vector3d(pos2), Vector3d(up))
        )
    }

    @Test
    fun testCofactor3x3() {
        assertEquals1(
            Matrix3f().fill().cofactor(),
            Matrix3d().fill().cofactor()
        )
        assertEquals1(
            Matrix4x3f().fill().cofactor3x3(),
            Matrix4x3d().fill().cofactor3x3()
        )
        assertEquals1(
            Matrix4f().fill().cofactor3x3(),
            Matrix4d().fill().cofactor3x3()
        )
        assertEquals1(
            Matrix4f().fill().cofactor3x3(Matrix3f()),
            Matrix4d().fill().cofactor3x3(Matrix3d())
        )
    }

    @Test
    fun testNormal() {
        assertEquals1(
            Matrix3f().fill().normal(),
            Matrix3d().fill().normal(), 2e-6
        )
        assertEquals1(
            Matrix4x3f().fill().normal(),
            Matrix4x3d().fill().normal(), 2e-6
        )
        assertEquals1(
            Matrix4x3f().fill().normalize3x3(),
            Matrix4x3d().fill().normalize3x3(), 2e-6
        )
        assertEquals1(
            Matrix4x3f().fill().normalize3x3(Matrix3f()),
            Matrix4x3d().fill().normalize3x3(Matrix3d()), 2e-6
        )
        assertEquals1(
            Matrix4f().fill().normal(),
            Matrix4d().fill().normal(), 2e-6
        )
        assertEquals1(
            Matrix4f().fill().normalize3x3(),
            Matrix4d().fill().normalize3x3(), 2e-6
        )
        assertEquals1(
            Matrix4f().fill().normalize3x3(Matrix3f()),
            Matrix4d().fill().normalize3x3(Matrix3d()), 2e-6
        )
    }

    @Test
    fun testPick() {
        val rnd = Random(1235)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val x = rnd.nextInt(100)
        val y = rnd.nextInt(100)
        val w = rnd.nextInt(100)
        val h = rnd.nextInt(100)
        val vp = intArrayOf(x, y, x + w, y + h)
        assertEquals1(
            Matrix4x3f().pick(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), vp),
            Matrix4x3d().pick(a, b, c, d, vp), 1e-3
        )
        assertEquals1(
            Matrix4f().pick(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), vp),
            Matrix4d().pick(a, b, c, d, vp), 1e-3
        )
    }

    @Test
    @Suppress("SpellCheckingInspection") // if you're using that word, you're doing sth wrong 😉😂
    fun testArcball() { // camera controls from hell
        val rnd = Random(1235)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val e = rnd.nextDouble()
        val f = rnd.nextDouble()
        assertEquals1(
            Matrix4x3f().arcball(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4x3d().arcball(a, b, c, d, e, f)
        )
        assertEquals1(
            Matrix4f().arcball(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), e.toFloat(), f.toFloat()),
            Matrix4d().arcball(a, b, c, d, e, f)
        )
    }

    @Test
    fun testWithLookAtUp() {
        val up = Vector3f().fill().normalize()
        assertEquals1(
            Matrix4x3f().withLookAtUp(up),
            Matrix4x3d().withLookAtUp(Vector3d(up))
        )
        assertEquals1(
            Matrix4f().withLookAtUp(up),
            Matrix4d().withLookAtUp(Vector3d(up))
        )
    }

    @Test
    fun testObliqueZ() {
        assertEquals1(
            Matrix3f().obliqueZ(1.2f, 2f),
            Matrix3d().obliqueZ(1.2, 2.0)
        )
        assertEquals1(
            Matrix4x3f().obliqueZ(1.2f, 2f),
            Matrix4x3d().obliqueZ(1.2, 2.0)
        )
        assertEquals1(
            Matrix4f().obliqueZ(1.2f, 2f),
            Matrix4d().obliqueZ(1.2, 2.0)
        )
    }

    @Test
    fun testSpan() {
        val corner = Vector2f().fill(123)
        val directions = Matrix2f().fill(156)
        val xDir = directions.positiveX(Vector2f())
        val yDir = directions.positiveY(Vector2f())
        assertEquals1(
            Matrix3x2f().span(corner, xDir, yDir),
            Matrix3x2d().span(Vector2d(corner), Vector2d(xDir), Vector2d(yDir))
        )
    }

    @Test
    fun testAffineSpan() {
        val corner = Vector3f().fill(123)
        val directions = Quaternionf().fill(156).normalize()
        val xDir = directions.positiveX(Vector3f())
        val yDir = directions.positiveY(Vector3f())
        val zDir = directions.positiveZ(Vector3f())
        assertEquals1(
            Matrix4f().affineSpan(corner, xDir, yDir, zDir),
            Matrix4d().affineSpan(Vector3d(corner), Vector3d(xDir), Vector3d(yDir), Vector3d(zDir))
        )
    }

    @Test
    fun testGetEulerAngles() {
        val euler = Vector3d().fill(5113)
        assertEquals(euler, Matrix3f().rotateXYZ(Vector3f(euler)).getEulerAnglesXYZ(Vector3f()), 1e-7)
        assertEquals(euler, Matrix3d().rotateXYZ(euler).getEulerAnglesXYZ(Vector3d()), 1e-16)
        assertEquals(euler, Matrix4x3f().rotateXYZ(Vector3f(euler)).getEulerAnglesXYZ(Vector3f()), 1e-7)
        assertEquals(euler, Matrix4x3d().rotateXYZ(euler).getEulerAnglesXYZ(Vector3d()), 1e-16)
        assertEquals(euler, Matrix4f().rotateXYZ(Vector3f(euler)).getEulerAnglesXYZ(Vector3f()), 1e-7)
        assertEquals(euler, Matrix4d().rotateXYZ(euler).getEulerAnglesXYZ(Vector3d()), 1e-16)
    }

    @Test
    fun testRotatePosOptimization() {
        assertEquals1(
            Matrix4x3f().translate(1f, 2f, 3f).rotateX(1f),
            Matrix4x3d().translate(1.0, 2.0, 3.0).rotateX(1.0)
        )
        assertEquals1(
            Matrix4x3f().translate(1f, 2f, 3f).rotateY(1f),
            Matrix4x3d().translate(1.0, 2.0, 3.0).rotateY(1.0)
        )
        assertEquals1(
            Matrix4x3f().translate(1f, 2f, 3f).rotateZ(1f),
            Matrix4x3d().translate(1.0, 2.0, 3.0).rotateZ(1.0)
        )

        assertEquals1(
            Matrix4f().translate(1f, 2f, 3f).rotateX(1f),
            Matrix4d().translate(1.0, 2.0, 3.0).rotateX(1.0)
        )
        assertEquals1(
            Matrix4f().translate(1f, 2f, 3f).rotateY(1f),
            Matrix4d().translate(1.0, 2.0, 3.0).rotateY(1.0)
        )
        assertEquals1(
            Matrix4f().translate(1f, 2f, 3f).rotateZ(1f),
            Matrix4d().translate(1.0, 2.0, 3.0).rotateZ(1.0)
        )
    }

    @Test
    fun testSetRotation() {
        assertEquals1(
            Matrix4x3f().setRotationXYZ(1f, 2f, 3f),
            Matrix4x3d().setRotationXYZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4x3f().setRotationYXZ(1f, 2f, 3f),
            Matrix4x3d().setRotationYXZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4x3f().setRotationZYX(1f, 2f, 3f),
            Matrix4x3d().setRotationZYX(1.0, 2.0, 3.0)
        )

        assertEquals1(
            Matrix4f().setRotationXYZ(1f, 2f, 3f),
            Matrix4d().setRotationXYZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4f().setRotationYXZ(1f, 2f, 3f),
            Matrix4d().setRotationYXZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4f().setRotationZYX(1f, 2f, 3f),
            Matrix4d().setRotationZYX(1.0, 2.0, 3.0)
        )
    }

    @Test
    fun testGetAxis2() {
        assertEquals(
            Vector2f(1f, 0f).rotate(1f),
            Matrix2f().scale(2f).rotate(-1f).positiveX(Vector2f()), 1e-6
        )
        assertEquals(
            Vector2d(1f, 0f).rotate(1.0),
            Matrix2d().scale(2.0).rotate(-1.0).positiveX(Vector2d())
        )
        assertEquals(
            Vector2f(0f, 1f).rotate(1f),
            Matrix2f().scale(2f).rotate(-1f).positiveY(Vector2f()), 1e-6
        )
        assertEquals(
            Vector2d(0f, 1f).rotate(1.0),
            Matrix2d().scale(2.0).rotate(-1.0).positiveY(Vector2d())
        )
        assertEquals(
            Vector2f(2f, 0f).rotate(1f),
            Matrix2f().scale(2f).rotate(-1f).normalizedPositiveX(Vector2f()), 1e-6
        )
        assertEquals(
            Vector2d(2f, 0f).rotate(1.0),
            Matrix2d().scale(2.0).rotate(-1.0).normalizedPositiveX(Vector2d())
        )
        assertEquals(
            Vector2f(0f, 2f).rotate(1f),
            Matrix2f().scale(2f).rotate(-1f).normalizedPositiveY(Vector2f()), 1e-6
        )
        assertEquals(
            Vector2d(0f, 2f).rotate(1.0),
            Matrix2d().scale(2.0).rotate(-1.0).normalizedPositiveY(Vector2d())
        )
    }

    @Test
    fun testGetAxis3x2() {
        assertEquals(
            Vector2f(1f, 0f).rotate(1f),
            Matrix3x2f().scale(2f).rotate(-1f).positiveX(Vector2f())
        )
        assertEquals(
            Vector2d(1f, 0f).rotate(1.0),
            Matrix3x2d().scale(2.0).rotate(-1.0).positiveX(Vector2d())
        )
        assertEquals(
            Vector2f(0f, 1f).rotate(1f),
            Matrix3x2f().scale(2f).rotate(-1f).positiveY(Vector2f())
        )
        assertEquals(
            Vector2d(0f, 1f).rotate(1.0),
            Matrix3x2d().scale(2.0).rotate(-1.0).positiveY(Vector2d())
        )
        assertEquals(
            Vector2f(2f, 0f).rotate(1f),
            Matrix3x2f().scale(2f).rotate(-1f).normalizedPositiveX(Vector2f())
        )
        assertEquals(
            Vector2d(2f, 0f).rotate(1.0),
            Matrix3x2d().scale(2.0).rotate(-1.0).normalizedPositiveX(Vector2d())
        )
        assertEquals(
            Vector2f(0f, 2f).rotate(1f),
            Matrix3x2f().scale(2f).rotate(-1f).normalizedPositiveY(Vector2f())
        )
        assertEquals(
            Vector2d(0f, 2f).rotate(1.0),
            Matrix3x2d().scale(2.0).rotate(-1.0).normalizedPositiveY(Vector2d())
        )
    }

    @Test
    fun testOrigin() {
        assertEquals(
            Matrix3x2f().fill().origin(Vector2f()),
            Matrix3x2d().fill().origin(Vector2d()),
            1e-6
        )
    }

    @Test
    fun testViewArea() {
        assertEquals(
            Vector4d(Matrix3x2f().fill().viewArea(FloatArray(4)).map { it.toDouble() }.toDoubleArray()),
            Vector4d(Matrix3x2d().fill().viewArea(DoubleArray(4))), 1e-6
        )
    }

    @Test
    fun testView() {
        val rnd = Random(12344)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        assertEquals1(
            Matrix3x2f().setView(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix3x2d().setView(a, b, c, d)
        )
        assertEquals1(
            Matrix3x2f().fill().view(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat()),
            Matrix3x2d().fill().view(a, b, c, d), 1e-6
        )
    }

    @Test
    fun testRotation() {
        assertEquals1(
            Matrix2f().fill().rotation(1f),
            Matrix2d().fill().rotation(1.0)
        )
        assertEquals1(
            Matrix3x2f().fill().rotation(1f),
            Matrix3x2d().fill().rotation(1.0)
        )

        assertEquals1(
            Matrix3f().fill().rotation(AxisAngle4f().fill(123)),
            Matrix3d().fill().rotation(AxisAngle4d().fill(123))
        )
        assertEquals1(
            Matrix3f().fill().rotation(Quaternionf().fill(123)),
            Matrix3d().fill().rotation(Quaterniond().fill(123))
        )
        assertEquals1(
            Matrix3f().fill().rotationXYZ(1f, 2f, 3f),
            Matrix3d().fill().rotationXYZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix3f().fill().rotationYXZ(1f, 2f, 3f),
            Matrix3d().fill().rotationYXZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix3f().fill().rotationZYX(1f, 2f, 3f),
            Matrix3d().fill().rotationZYX(1.0, 2.0, 3.0)
        )

        assertEquals1(
            Matrix4x3f().fill().rotation(AxisAngle4f().fill(123)),
            Matrix4x3d().fill().rotation(AxisAngle4d().fill(123))
        )
        assertEquals1(
            Matrix4x3f().fill().rotation(Quaternionf().fill(123)),
            Matrix4x3d().fill().rotation(Quaterniond().fill(123))
        )
        assertEquals1(
            Matrix4x3f().fill().rotationXYZ(1f, 2f, 3f),
            Matrix4x3d().fill().rotationXYZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4x3f().fill().rotationYXZ(1f, 2f, 3f),
            Matrix4x3d().fill().rotationYXZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4x3f().fill().rotationZYX(1f, 2f, 3f),
            Matrix4x3d().fill().rotationZYX(1.0, 2.0, 3.0)
        )

        assertEquals1(
            Matrix4f().fill().rotation(AxisAngle4f().fill(123)),
            Matrix4d().fill().rotation(AxisAngle4d().fill(123))
        )
        assertEquals1(
            Matrix4f().fill().rotation(Quaternionf().fill(123)),
            Matrix4d().fill().rotation(Quaterniond().fill(123))
        )
        assertEquals1(
            Matrix4f().fill().rotationXYZ(1f, 2f, 3f),
            Matrix4d().fill().rotationXYZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4f().fill().rotationYXZ(1f, 2f, 3f),
            Matrix4d().fill().rotationYXZ(1.0, 2.0, 3.0)
        )
        assertEquals1(
            Matrix4f().fill().rotationZYX(1f, 2f, 3f),
            Matrix4d().fill().rotationZYX(1.0, 2.0, 3.0)
        )
    }

    @Test
    fun testRotationTo() {
        val from = Vector2f().fill(56).normalize()
        val to = Vector2f().fill(156).normalize()
        assertEquals1(
            Matrix3x2f().rotateTo(from, to),
            Matrix3x2d().rotateTo(Vector2d(from), Vector2d(to))
        )
    }

    @Test
    fun testRotationTowards() {
        val dir = Vector3f().fill(56).normalize()
        val up = Vector3f().fill(156).normalize()
        assertEquals1(
            Matrix3f().rotateTowards(dir, up),
            Matrix3d().rotateTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix3f().fill().rotateTowards(dir, up),
            Matrix3d().fill().rotateTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix3f().fill().rotationTowards(dir, up),
            Matrix3d().fill().rotationTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )

        assertEquals1(
            Matrix4x3f().rotateTowards(dir, up),
            Matrix4x3d().rotateTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill().rotateTowards(dir, up),
            Matrix4x3d().fill().rotateTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4x3f().fill().rotationTowards(dir, up),
            Matrix4x3d().fill().rotationTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )

        assertEquals1(
            Matrix4f().rotateTowards(dir, up),
            Matrix4d().rotateTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill().rotateTowards(dir, up),
            Matrix4d().fill().rotateTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill().rotationTowards(dir, up),
            Matrix4d().fill().rotationTowards(Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill().rotationTowardsXY(1f, 2f),
            Matrix4d().fill().rotationTowardsXY(1.0, 2.0),
        )
    }

    @Test
    fun testInvert() {
        assertEquals1(
            Matrix2f().fill().invert(),
            Matrix2d().fill().invert(), 5e-7
        )
        assertEquals1(
            Matrix3x2f().fill().invert(),
            Matrix3x2d().fill().invert(), 5e-7
        )
        assertEquals1(
            Matrix3f().fill().invert(),
            Matrix3d().fill().invert(), 3e-6
        )
        assertEquals1(
            Matrix4x3f().fill().invert(),
            Matrix4x3d().fill().invert(), 3e-6
        )
        assertEquals1(
            Matrix4f().fill().invert(),
            Matrix4d().fill().invert(), 5e-4
        )
    }

    @Test
    fun testInvertOrtho() {
        assertEquals1(
            Matrix4x3f().fill().invertOrtho(),
            Matrix4x3d().fill().invertOrtho(), 5e-7
        )
        assertEquals1(
            Matrix4f().fill().invertOrtho(),
            Matrix4d().fill().invertOrtho(), 5e-7
        )
    }

    @Test
    fun testInvertAffine() {
        assertEquals1(
            Matrix4f().fill().invertAffine(),
            Matrix4d().fill().invertAffine(), 5e-7
        )
    }

    @Test
    fun testInvertPerspective() {
        assertEquals1(
            Matrix4f().fill().invertPerspective(),
            Matrix4d().fill().invertPerspective(), 1e-6
        )
    }

    @Test
    fun testInvertPerspectiveView() {
        assertEquals1(
            Matrix4f().fill().invertPerspectiveView(Matrix4f().fill(546)),
            Matrix4d().fill().invertPerspectiveView(Matrix4d().fill(546)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill().invertPerspectiveView(Matrix4x3f().fill(546)),
            Matrix4d().fill().invertPerspectiveView(Matrix4x3d().fill(546)), 1e-6
        )
    }

    @Test
    fun testInvertFrustum() {
        assertEquals1(
            Matrix4f().fill().invertFrustum(),
            Matrix4d().fill().invertFrustum(), 1e-6
        )
    }

    @Test
    fun testSet3x3() {
        assertEquals1(
            Matrix4f().fill().set3x3(Matrix3f().fill(123)),
            Matrix4d().fill().set3x3(Matrix3d().fill(123))
        )
    }

    @Test
    fun testSet4x3() {
        assertEquals1(
            Matrix4f().fill().set4x3(Matrix4x3f().fill(123)),
            Matrix4d().fill().set4x3(Matrix4x3f().fill(123))
        )
        assertEquals1(
            Matrix4f().fill().set4x3(Matrix4x3f().fill(123)),
            Matrix4d().fill().set4x3(Matrix4x3d().fill(123))
        )
    }

    @Test
    fun testSetTransposed() {
        assertEquals1(
            Matrix3f().fill().setTransposed(Matrix3f().fill(123)),
            Matrix3d().fill().setTransposed(Matrix3d().fill(123))
        )
        assertEquals1(
            Matrix4f().fill().setTransposed(Matrix4f().fill(123)),
            Matrix4d().fill().setTransposed(Matrix4d().fill(123))
        )
    }

    @Test
    fun testRotationTranslateTowards() {
        val pos = Vector3f().fill(123)
        val dir = Vector3f().fill(56).normalize()
        val up = Vector3f().fill(156).normalize()
        assertEquals1(
            Matrix4x3f().translationRotateTowards(pos, dir, up),
            Matrix4x3d().translationRotateTowards(Vector3d(pos), Vector3d(dir), Vector3d(up)), 1e-6
        )
        assertEquals1(
            Matrix4f().translationRotateTowards(pos, dir, up),
            Matrix4d().translationRotateTowards(Vector3d(pos), Vector3d(dir), Vector3d(up)), 1e-6
        )
    }

    @Test
    fun testLerp() {
        assertEquals1(
            Matrix4f().fill(123).lerp(Matrix4f().fill(234), 0.3f),
            Matrix4d().fill(123).lerp(Matrix4d().fill(234), 0.3)
        )
    }

    @Test
    fun testTransformAab4x3() {
        val vec0 = Vector3d().fill(123)
        val vec1 = Vector3d().fill(456)
        val min0 = vec0.min(vec1, Vector3d())
        val max0 = vec0.max(vec1, Vector3d())

        val aabb0 = AABBd().setMin(min0).setMax(max0)
        aabb0.transform(Matrix4x3d().fill(123))

        val min1f = Vector3f()
        val max1f = Vector3f()
        Matrix4x3f().fill(123).transformAab(Vector3f(min0), Vector3f(max0), min1f, max1f)
        assertEquals(aabb0.getMin(Vector3f()), min1f, 2e-7)
        assertEquals(aabb0.getMax(Vector3f()), max1f, 2e-7)

        val min1d = Vector3d()
        val max1d = Vector3d()
        Matrix4x3d().fill(123).transformAab(min0, max0, min1d, max1d)
        assertEquals(aabb0.getMin(Vector3d()), min1d)
        assertEquals(aabb0.getMax(Vector3d()), max1d)
    }

    @Test
    fun testTransformAab4() {
        val vec0 = Vector3d().fill(123)
        val vec1 = Vector3d().fill(456)
        val min0 = vec0.min(vec1, Vector3d())
        val max0 = vec0.max(vec1, Vector3d())

        val aabb0 = AABBd().setMin(min0).setMax(max0)
        aabb0.transform(Matrix4d().fill(123))

        val min1f = Vector3f()
        val max1f = Vector3f()
        Matrix4f().fill(123).transformAab(Vector3f(min0), Vector3f(max0), min1f, max1f)
        assertEquals(aabb0.getMin(Vector3f()), min1f, 2e-7)
        assertEquals(aabb0.getMax(Vector3f()), max1f, 2e-7)

        val min1d = Vector3d()
        val max1d = Vector3d()
        Matrix4d().fill(123).transformAab(min0, max0, min1d, max1d)
        assertEquals(aabb0.getMin(Vector3d()), min1d)
        assertEquals(aabb0.getMax(Vector3d()), max1d)
    }

    @Test
    fun testTrapezoidCrop() {
        val rnd = Random(1324)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        val d = rnd.nextDouble()
        val e = rnd.nextDouble()
        val f = rnd.nextDouble()
        val g = rnd.nextDouble()
        val h = rnd.nextDouble()
        assertEquals1(
            Matrix4f().trapezoidCrop(
                a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(),
                e.toFloat(), f.toFloat(), g.toFloat(), h.toFloat()
            ), Matrix4d().trapezoidCrop(a, b, c, d, e, f, g, h), 1e-4
        )
    }

    @Test
    fun testOrthoCrop() {
        assertEquals1(
            Matrix4f().fill(234).orthoCrop(Matrix4f().fill(456)),
            Matrix4d().fill(234).orthoCrop(Matrix4d().fill(456)), 1e-6
        )
    }

    @Test
    fun testPerspectiveFrustumSlice() {
        assertEquals1(
            Matrix4f().fill(234).perspectiveFrustumSlice(1e-3f, 1e3f),
            Matrix4d().fill(234).perspectiveFrustumSlice(1e-3, 1e3), 1e-6
        )
    }

    @Test
    fun testFrustumAABB() {
        val min0 = Vector3f()
        val max0 = Vector3f()
        val min1 = Vector3d()
        val max1 = Vector3d()
        Matrix4f().fill().frustumAabb(min0, max0)
        Matrix4d().fill().frustumAabb(min1, max1)
        assertEquals(Vector3d(min0), min1, 1e-3)
        assertEquals(Vector3d(max0), max1, 1e-3)
    }

    @Test
    fun testSetSkewSymmetric() {
        val rnd = Random(1324)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        val c = rnd.nextDouble()
        assertEquals1(
            Matrix3f().fill().setSkewSymmetric(a.toFloat(), b.toFloat(), c.toFloat()),
            Matrix3d().fill().setSkewSymmetric(a, b, c)
        )
    }

    @Test
    fun testSkew() {
        val rnd = Random(1324)
        val a = rnd.nextDouble()
        val b = rnd.nextDouble()
        assertEquals1(
            Matrix4f().fill().skew(a.toFloat(), b.toFloat()),
            Matrix4d().fill().skew(a, b)
        )
    }

    @Test
    fun testMul() {
        assertEquals1(
            Matrix4f().fill().mul(Matrix4x3f().fill(123)),
            Matrix4d().fill().mul(Matrix4x3d().fill(123)), 1e-6
        )
        assertEquals1(
            Matrix4f().fill().mul(Matrix4x3d().fill(123)),
            Matrix4d().fill().mul(Matrix4x3d().fill(123)), 1e-6
        )
    }

    @Test
    fun testMulLocal() {
        assertEquals1(
            Matrix2f().fill().mulLocal(Matrix2f().fill(123)),
            Matrix2d().fill().mulLocal(Matrix2d().fill(123))
        )
        assertEquals1(
            Matrix3x2f().fill().mulLocal(Matrix3x2f().fill(123)),
            Matrix3x2d().fill().mulLocal(Matrix3x2d().fill(123))
        )
        assertEquals1(
            Matrix3f().fill().mulLocal(Matrix3f().fill(123)),
            Matrix3d().fill().mulLocal(Matrix3d().fill(123))
        )
        assertEquals1(
            Matrix4f().fill().mulLocal(Matrix4f().fill(123)),
            Matrix4d().fill().mulLocal(Matrix4d().fill(123)), 1e-6
        )
    }

    @Test
    fun testMulLocalAffine() {
        assertEquals1(
            Matrix4f().fill().mulLocalAffine(Matrix4f().fill(123)),
            Matrix4d().fill().mulLocalAffine(Matrix4d().fill(123))
        )
    }

    @Test
    fun testMulOrthoAffine() {
        assertEquals1(
            Matrix4f().fill().mulOrthoAffine(Matrix4f().fill(123)),
            Matrix4d().fill().mulOrthoAffine(Matrix4d().fill(123))
        )
    }

    @Test
    fun testMulTranslationAffine() {
        assertEquals1(
            Matrix4f().fill().mulTranslationAffine(Matrix4f().fill(123)),
            Matrix4d().fill().mulTranslationAffine(Matrix4d().fill(123))
        )
    }

    fun assertEquals1(a: Matrix<*, *, *>, b: Matrix<*, *, *>, threshold: Double = 1e-7) {
        assertEquals(a.numCols, b.numCols)
        assertEquals(a.numRows, b.numRows)
        for (col in 0 until a.numCols) {
            for (row in 0 until a.numRows) {
                assertEquals(a[col, row], b[col, row], threshold) {
                    "|\n$a - \n$b\n| = ${abs(a[col, row] - b[col, row])} > $threshold, m$col$row"
                }
            }
        }
    }
}