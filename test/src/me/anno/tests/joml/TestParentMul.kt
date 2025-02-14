package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.random.Random

class TRS(random: Random) {
    val pos = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
    val rot = Quaternionf().rotateYXZ(random.nextFloat(), random.nextFloat(), random.nextFloat())
    val sca = Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())

    val M = Matrix4x3f()
        .translate(pos)
        .rotate(rot)
        .scale(sca)
}

fun main() {
    val rnd = Random(1365)
    val parent = TRS(rnd)
    val child = TRS(rnd)

    assertEquals(
        parent.M.mul(child.M, Matrix4x3f()),
        Matrix4x3f(parent.M)
            .translate(child.pos)
            .rotate(child.rot)
            .scale(child.sca),
        1e-5
    )
}