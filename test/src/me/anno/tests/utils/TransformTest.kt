package me.anno.tests.utils

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Matrix4x3
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TransformTest {

    val pos get() = Vector3d(1.0, 2.0, 3.0)
    val rot get() = Quaternionf().rotateX(10f)
    val sca get() = Vector3f(1f, 1.5f, 2f)

    val pos2 get() = Vector3d(5.0, -2.0, 3.5)
    val rot2 get() = Quaternionf().rotateY(10f).rotateX(5f)
    val sca2 get() = Vector3f(1.5f, 1.1f, 0.5f)

    @Test
    fun testTransformValidation() {

        val e = Entity()
        val t = e.transform

        t.localPosition = pos
        t.localRotation = rot
        t.localScale = sca

        t.validate()

        assertEquals(t.globalTransform, t.getLocalTransform(Matrix4x3()))

        assertEquals(pos, t.localPosition)
        assertEquals(pos, t.globalPosition)
        assertTrue(rot.equals(t.localRotation, 1e-7f))
        assertTrue(rot.equals(t.globalRotation, 1e-7f))
        assertTrue(sca.equals(t.localScale, 1e-7f))
        assertTrue(sca.equals(t.globalScale, 1e-6f)) { "$sca != ${t.globalScale}" }
    }

    // -q = q in 3d
    @Test
    fun testQuaternionNegation() {
        val rnd = Random(12234)
        for (i in 0 until 100) {
            val x = rnd.nextDouble() * 2 - 1
            val y = rnd.nextDouble() * 2 - 1
            val z = rnd.nextDouble() * 2 - 1
            val w = rnd.nextDouble() * 2 - 1
            val pos = Quaterniond(x, y, z, w).normalize()
            val neg = Quaterniond(-x, -y, -z, -w).normalize()
            assertEquals(
                pos.transform(Vector3d(1.0, 0.0, 0.0)),
                neg.transform(Vector3d(1.0, 0.0, 0.0))
            )
            assertEquals(
                pos.transform(Vector3d(0.0, 1.0, 0.0)),
                neg.transform(Vector3d(0.0, 1.0, 0.0))
            )
            assertEquals(
                pos.transform(Vector3d(0.0, 0.0, 1.0)),
                neg.transform(Vector3d(0.0, 0.0, 1.0))
            )
        }
    }

    @Test
    fun testWithHierarchy() {
        // setup: parent and child with local transforms
        val parent = Entity("Parent")
        val child = Entity("Child", parent)

        parent.position = pos
        parent.rotation = rot
        parent.scale = sca

        child.position = pos2
        child.rotation = rot2
        child.scale = sca2

        parent.validateTransform()

        // tests
        val parentMatrix = Matrix4x3().translationRotateScale(pos, rot, sca)
        assertTrue(parentMatrix.equals(parent.transform.getLocalTransform(Matrix4x3()), 1e-15))
        assertTrue(parentMatrix.equals(parent.transform.globalTransform, 1e-15))

        assertTrue(
            parentMatrix.transformPosition(pos2)
                .equals(child.transform.globalPosition, 1e-15)
        )
        val childLocal = Matrix4x3().translationRotateScale(pos2, rot2, sca2)
        val childGlobal = parentMatrix.mul(childLocal, Matrix4x3())
        assertTrue(childLocal.equals(child.transform.getLocalTransform(Matrix4x3()), 1e-15))
        assertTrue(childGlobal.equals(child.transform.globalTransform, 1e-15))

        assertTrue(childGlobal.getTranslation(Vector3d()).equals(child.transform.globalPosition, 1e-15))
        assertTrue(childGlobal.getUnnormalizedRotation(Quaternionf()).equals(child.transform.globalRotation, 1e-15f))
        assertTrue(childGlobal.getScale(Vector3f()).equals(child.transform.globalScale, 1e-15f))
    }

    @Test
    fun testInverseTransformToParent() {
        val parent = Entity()
        val child = Entity(parent)

        parent.position = pos
        parent.rotation = rot

        parent.validateTransform()
        child.transform.globalPosition = Vector3d()
        parent.validateTransform()
        child.transform.globalRotation = Quaternionf()
        parent.validateTransform()

        checkIdentityTransform(child.transform)
    }

    private fun checkIdentityTransform(transform: Transform) {
        assertTrue(transform.globalTransform.equals(Matrix4x3(), 1e-4)) {
            "${transform.globalTransform} isn't identity"
        }
        assertTrue(transform.globalPosition.equals(Vector3d(), 1e-6)) {
            "${transform.globalPosition} isn't identity"
        }
        assertTrue(transform.globalRotation.equals(Quaternionf(), 1e-7f)) {
            "${transform.globalRotation} isn't identity"
        }
        assertTrue(transform.globalScale.equals(Vector3f(1f), 1e-6f)) {
            "${transform.globalScale} isn't identity"
        }
    }

    @Test
    fun testInverseTransformToParent2() {
        val parent = Entity()
        val child = Entity(parent)

        parent.position = pos
        parent.rotation = rot

        parent.validateTransform()
        child.transform.setGlobal(Matrix4x3())
        parent.validateTransform()

        checkIdentityTransform(child.transform)
    }
}