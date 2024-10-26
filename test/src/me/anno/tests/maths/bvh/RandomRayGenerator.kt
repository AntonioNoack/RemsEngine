package me.anno.tests.maths.bvh

import me.anno.maths.Maths.TAUf
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class RandomRayGenerator {

    val random = Random(1234)
    val pos = Vector3f()
    val dir = Vector3f()
    val rot = Quaternionf()

    fun next(): Boolean {

        val angle = random.nextFloat() * TAUf
        val distanceFromCenter = random.nextFloat() * 2f
        val px = cos(angle) * distanceFromCenter
        val py = sin(angle) * distanceFromCenter

        // we could reset the quaternion here, but it doesn't matter
        rot.rotateX(random.nextFloat() * TAUf)
        rot.rotateY(random.nextFloat() * TAUf)
        rot.rotateZ(random.nextFloat() * TAUf)

        pos.set(px, py, 2f).rotate(rot)
        dir.set(0f, 0f, -1f).rotate(rot)

        return distanceFromCenter < 1f
    }
}