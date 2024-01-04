package me.anno.tests.maths

import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import kotlin.math.atan2

fun main() {
    val rnd = Random()
    val quat = Quaternionf()
    val test = Vector3f()
    val normal = Vector3f()
    for (i in 0 until 100) {
        normal.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).normalize()
            .normalToQuaternionY(quat)
            .transform(test.set(1f, 0f, 0f))
        for (j in 0 until 10) {
            val angle = atan2(test.z, test.x)
            quat.rotateY(angle)
                .transform(test.set(1f, 0f, 0f))
        }
        val angle2 = atan2(test.z, test.x)
        println(angle2)
    }
}