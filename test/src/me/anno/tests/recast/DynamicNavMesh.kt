package me.anno.tests.recast

import org.recast4j.dynamic.DynamicNavMesh
import org.recast4j.dynamic.io.VoxelFile

// todo:
//  - agent walking left and right
//  - colliders obstructing pieces of the path by moving up/down
fun main() {
    val voxelFile = VoxelFile()
    val navMesh = DynamicNavMesh(voxelFile)

}