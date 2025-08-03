package me.anno.tests.utils

import me.anno.maths.Maths.mix
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.toRadians
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.random.Random

class QuaternionTest {
    @Test
    fun eulerAnglesToQuaternionToEulerAngles() {
        val workQuat = Quaterniond()
        val testQuat = Quaterniond()
        val tmpQuat = Quaterniond()
        val srcVec = Vector3d()
        val dstVec = Vector3d()
        val rand = Random(1234L)
        val maxAllowedError = 1e-7
        for (i in 0 until 10_000) {
            srcVec.set(
                mix(-90.0, 90.0, rand.nextDouble()),
                mix(-180.0, 180.0, rand.nextDouble()),
                mix(-90.0, 90.0, rand.nextDouble())
            )
            srcVec.toQuaternionDegrees(workQuat)
            testQuat.identity()
                .rotateY(srcVec.y.toRadians())
                .rotateX(srcVec.x.toRadians())
                .rotateZ(srcVec.z.toRadians())
            assertEquals(0.0, workQuat.difference(testQuat, tmpQuat).angle(), maxAllowedError)
            workQuat.toEulerAnglesDegrees(dstVec)
            assertEquals(0.0, dstVec.distanceSquared(srcVec), maxAllowedError)
        }
    }

    @Test
    fun testRotationToParallel() {
        val qf = Quaternionf().rotationTo(0f, 1f, 0f, 0f, 1f, 0f)
        assertEquals(Quaternionf(), qf)

        qf.rotationTo(0f, 1f, 0f, 0f, -1f, 0f)
        assertEquals(Quaternionf(1f, 0f, 0f, 0f), qf)

        val qd = Quaterniond().rotationTo(0.0, 1.0, 0.0, 0.0, 1.0, 0.0)
        assertEquals(Quaterniond(), qd)

        qd.rotationTo(0.0, 1.0, 0.0, 0.0, -1.0, 0.0)
        assertEquals(Quaterniond(1.0, 0.0, 0.0, 0.0), qd)
    }
}