package me.anno.tests.rtrt.engine

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.Clock

fun main() {
    val size = 512
    val mesh = Mesh()
    val clock = Clock("BLASBench")
    RectangleTerrainModel.generateRegularQuadHeightMesh(size, size, false, 1f, mesh, true)
    clock.stop("Mesh Generation")
    BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
    clock.stop("BLAS Generation")
    Engine.requestShutdown()
}