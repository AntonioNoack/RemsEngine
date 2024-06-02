package me.anno.tests.rtrt.engine

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.Clock

fun main() {
    val size = 512
    val mesh = Mesh()
    val clock = Clock("BLASBench")
    TerrainUtils.generateRegularQuadHeightMesh(size, size, false, 1f, mesh, true)
    BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
    clock.stop("BLAS Generation")
}