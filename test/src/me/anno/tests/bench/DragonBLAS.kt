package me.anno.tests.bench

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.BVHBuilder.centroidTime
import me.anno.maths.bvh.BVHBuilder.splitTime
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.types.Floats.formatPercent
import org.apache.logging.log4j.LogManager

fun main() {
    val logger = LogManager.getLogger("DragonBLAS")
    val clock = Clock(logger)
    OfficialExtensions.initForTests()
    clock.stop("Loading Extensions")
    val mesh = MeshCache[downloads.getChild("3d/dragon.obj")]!!
    clock.stop("Loading Mesh")
    // 180ms/e, 230ms first time
    // optimized using multi-threading and pivot-reusing down to 42ms/e
    logger.info("Num Primitives: ${mesh.numPrimitives}")
    clock.benchmark(3, 10, "Building BLAS") {
        val node = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)!!
        if (it == 0) logger.info("Max-depth: ${node.maxDepth()}")
    }
    val ratio = (centroidTime.get().toFloat() / (centroidTime.get() + splitTime.get()))
    logger.info("Centroid-Finding: ${ratio.formatPercent()}%")
    Engine.requestShutdown()
}