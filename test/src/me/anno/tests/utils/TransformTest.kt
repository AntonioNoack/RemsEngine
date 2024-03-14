package me.anno.tests.utils

import me.anno.ecs.Entity
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransformTest {

    val pos get() = Vector3d(1.0, 2.0, 3.0)
    val rot get() = Quaterniond().rotateX(10.0)
    val sca get() = Vector3d(1.0, 1.5, 2.0)

    val pos2 get() = Vector3d(5.0, -2.0, 3.5)
    val rot2 get() = Quaterniond().rotateY(10.0).rotateX(5.0)
    val sca2 get() = Vector3d(1.5, 1.1, 0.5)

    @Test
    fun testTransformValidation() {

        val e = Entity()
        val t = e.transform

        t.localPosition = pos
        t.localRotation = rot
        t.localScale = sca

        t.validate()

        assertEquals(t.localTransform, t.globalTransform)

        assertEquals(pos, t.localPosition)
        assertEquals(pos, t.globalPosition)
        assertTrue(rot.equals(t.localRotation, 1e-15))
        assertTrue(rot.equals(t.globalRotation, 1e-15))
        assertTrue(sca.equals(t.localScale, 1e-15))
        assertTrue(sca.equals(t.globalScale, 1e-15))
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
        val parent = Entity()
        val child = Entity(parent)

        parent.position = pos
        parent.rotation = rot
        parent.scale = sca

        child.position = pos2
        child.rotation = rot2
        child.scale = sca2

        parent.validateTransform()

        // tests
        val parentMatrix = Matrix4x3d().translationRotateScale(pos, rot, sca)
        assertTrue(parentMatrix.equals(parent.transform.localTransform, 1e-15))
        assertTrue(parentMatrix.equals(parent.transform.globalTransform, 1e-15))

        assertTrue(
            parentMatrix.transformPosition(pos2)
                .equals(child.transform.globalPosition, 1e-15)
        )
        val childLocal = Matrix4x3d().translationRotateScale(pos2, rot2, sca2)
        val childGlobal = parentMatrix.mul(childLocal, Matrix4x3d())
        assertTrue(childLocal.equals(child.transform.localTransform, 1e-15))
        assertTrue(childGlobal.equals(child.transform.globalTransform, 1e-15))

        assertTrue(childGlobal.getTranslation(Vector3d()).equals(child.transform.globalPosition, 1e-15))
        assertTrue(childGlobal.getUnnormalizedRotation(Quaterniond()).equals(child.transform.globalRotation, 1e-15))
        assertTrue(childGlobal.getScale(Vector3d()).equals(child.transform.globalScale, 1e-15))
    }

    // todo test setting global transform onto child
    @Test
    fun testInverseTransformToParent() {
        val parent = Entity()
        val child = Entity(parent)

        parent.position = pos
        parent.rotation = rot

        parent.validateTransform()
        child.transform.globalPosition = Vector3d()
        parent.validateTransform()
        child.transform.globalRotation = Quaterniond()
        parent.validateTransform()

        assertTrue(child.transform.globalTransform.equals(Matrix4x3d(), 1e-15))
        assertTrue(child.transform.globalPosition.equals(Vector3d(), 1e-15))
        assertTrue(child.transform.globalRotation.equals(Quaterniond(), 1e-15))
        assertTrue(child.transform.globalScale.equals(Vector3d(1.0), 1e-15))
    }
}