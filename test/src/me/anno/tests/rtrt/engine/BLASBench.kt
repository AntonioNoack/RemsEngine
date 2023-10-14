package me.anno.tests.rtrt.engine

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.Clock

fun main() {
    val size = 4096
    Thread.sleep(10_000) // time for VisualVM to attach
    val mesh = Mesh()
    val clock = Clock()
    TerrainUtils.generateRegularQuadHeightMesh(size, size, false, 1f, mesh)
    BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
    clock.stop("BLAS Generation")
}