package me.anno.utils.test

import me.anno.utils.Vectors.print
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Vector4f

fun main(){

    val logger = LogManager.getLogger()

    val m = Matrix4f().perspective(1f, 1f, 0.5f, 10f)

    logger.info(m.toString())

    logger.info(m.transformProject(Vector4f(0f, 0f, -1f, 1f)).print())

    logger.info(m.transform(Vector4f(0f, 0f, -1f, 1f)).print())



}