package me.anno.gpu.pipeline

import me.anno.gpu.ShaderLib.pbrModelShader
import me.anno.gpu.shader.BaseShader
import me.anno.utils.image.ImageWriter.writeImageInt
import org.joml.AABBd
import org.joml.Math.toRadians
import org.joml.Quaterniond
import org.joml.Vector3d

fun main() {

    pbrModelShader = BaseShader("", "", "", "")

    val pipeline = Pipeline()
    imageTest(pipeline)
    simpleTest(pipeline)

}

fun imageTest(pipeline: Pipeline) {

    val w = 256
    val h = 256

    pipeline.calculatePlanes(
        0.001, 100.0, toRadians(20.0), 1.0,
        Quaterniond().rotateX(0.3),
        Vector3d(0.0, 0.0, 0.0)
    )

    for (z in -5 until 0) {
        writeImageInt(w, h, false, "cull$z.png", 1024) { x, y, i ->
            val aabb = AABBd()
            val xf = (x - w / 2) * 5.0 / w
            val yf = (y - h / 2) * 5.0 / h
            val zf = z * 1.0
            aabb.union(Vector3d(xf, yf, zf))
            val d = 0.5
            aabb.union(Vector3d(xf + d, yf + d, zf + d))
            if (pipeline.contains(aabb)) 0 else 0xffffff
        }
    }

}

fun simpleTest(pipeline: Pipeline) {

    pipeline.calculatePlanes(0.001, 100.0, toRadians(90.0), 1.0, Quaterniond(), Vector3d(0.0, 0.0, -1.0))

    val aabb1 = AABBd()
    aabb1.union(0.0, 0.0, 0.0)

    val aabb2 = AABBd()
    aabb2.union(0.0, 0.0, 0.9)

    println("shall be false: ${pipeline.contains(aabb1)}")

    println("shall be false: ${pipeline.contains(aabb2)}")

    pipeline.calculatePlanes(0.001, 100.0, toRadians(90.0), 1.0, Quaterniond(), Vector3d(0.0, 0.0, 1.0))

    println("shall be true: ${pipeline.contains(aabb1)}")

    println("shall be true: ${pipeline.contains(aabb2)}")

}