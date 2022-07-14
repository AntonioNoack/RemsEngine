package me.anno.tests

import me.anno.ecs.components.chunks.spherical.SphereTriangle
import me.anno.utils.types.Vectors.print
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

fun main() {
    val a = Vector3d(-1.0, -1.0, 1.0)
    val b = Vector3d(-1.0, +1.0, 1.0)
    val c = Vector3d(+2.0, +0.0, 1.0)
    val tri = SphereTriangle(null, 0, a, b, c)
    val logger = LogManager.getLogger(tri.javaClass)
    logger.info(tri.baseAB.print())
    logger.info(tri.baseUp.print())
    logger.info(tri.baseAC.print())
    logger.info(tri.globalToLocal)
    logger.info(tri.localToGlobal)
    logger.info(
        tri.globalToLocal.transformPosition(Vector3d(0.0, 0.0, 1.1)).print()
    ) // shall become (0,0.1,0)
    logger.info(
        tri.globalToLocal.transformPosition(Vector3d(0.0, 1.0, 1.1)).print()
    ) // shall become (1,0.1,0)
    logger.info(tri.localA.print())
    logger.info(tri.localB.print())
    logger.info(tri.localC.print())
}