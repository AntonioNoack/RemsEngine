package me.anno.tests.physics.bullet

import me.anno.ecs.components.physics.Physics.Companion.convertEntityToPhysicsI
import me.anno.ecs.components.physics.Physics.Companion.convertPhysicsToEntityII
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

        val original = Matrix4x3()
            .translationRotateScale(position, rotation, scale)

        val tmpPos = Vector3d()
        val tmpRot = Quaternionf()
        val convertedBack = Matrix4x3()
        convertEntityToPhysicsI(original, tmpPos, tmpRot, scale, centerOfMass)
        convertPhysicsToEntityII(tmpPos, tmpRot, convertedBack, scale, centerOfMass)

        assertEquals(original, convertedBack, 1e-6)
    }
}