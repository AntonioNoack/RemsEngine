package me.anno.tests.utils

import me.anno.maths.Maths.mix
import me.anno.utils.types.Floats.toRadians
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.random.Random

fun main() {
    val workQuat = Quaterniond()
    val testQuat = Quaterniond()
    val tmpQuat = Quaterniond()
    val srcVec = Vector3d()
    val dstVec = Vector3d()
    val rand = Random(1234L)
    val maxAllowedError = 1e-7
    for (i in 0 until 1_000_000) {
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
        if (workQuat.difference(testQuat, tmpQuat).angle() > maxAllowedError)
            throw IllegalStateException("1/[$i] $workQuat != $testQuat")
        workQuat.toEulerAnglesDegrees(dstVec)
        if (dstVec.distanceSquared(srcVec) > maxAllowedError)
            throw IllegalStateException("2/[$i] $srcVec -> $dstVec via $workQuat")
    }
}