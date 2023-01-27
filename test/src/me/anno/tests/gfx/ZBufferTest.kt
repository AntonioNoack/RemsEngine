package me.anno.tests.gfx

import me.anno.gpu.drawing.Perspective.perspective2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Vector4f

fun main() {

    val logger = LogManager.getLogger("ZBufferTest")

    val m = Matrix4f().perspective2(1f, 1f, 0.5f, 10f, 0f, 0f)

    logger.info(m.toString())
    logger.info(m.transformProject(Vector4f(0f, 0f, -1f, 1f)))
    logger.info(m.transform(Vector4f(0f, 0f, -1f, 1f)))

}