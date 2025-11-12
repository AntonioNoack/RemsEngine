package me.anno.tests.physics.bullet

import com.bulletphysics.linearmath.Transform
import me.anno.bullet.BulletPhysics.Companion.mat4x3ToTransform
import me.anno.bullet.BulletPhysics.Companion.transformToMat4x3
import me.anno.utils.assertions.assertEquals
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class CenterOfMassTests {
    @Test
    fun testCenterOfMassIsInvertible() {

        val rotation = Quaternionf()
            .rotateX(0.1f)
            .rotateY(0.2f)
            .rotateZ(0.3f)

        val scale = Vector3f(1.0, 2.0, 3.0)
        val position = Vector3d(-5.0, 3.0, 1.0)
        val centerOfMass = Vector3d(7.0, 2.0, 13.0)

        val basis = Matrix4x3()
            .translationRotateScale(position, rotation, Vector3f(scale))

        val asTransform = mat4x3ToTransform(basis, scale, centerOfMass, Transform())
        val convertedBack = transformToMat4x3(asTransform, scale, centerOfMass, Matrix4x3())

        assertEquals(basis, convertedBack, 1e-15)
    }
}