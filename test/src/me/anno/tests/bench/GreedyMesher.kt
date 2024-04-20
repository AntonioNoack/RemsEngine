package me.anno.tests.bench

import me.anno.ecs.components.mesh.Mesh
import me.anno.mesh.vox.model.VoxelModel
import me.anno.tests.utils.TestWorld
import me.anno.utils.Clock

fun main() {
    val clock = Clock()
    val world = TestWorld()
    val mesh = Mesh()
    val chunk = world.getChunk(0, 0, 0, true)!!
    val model = object : VoxelModel(world.sizeX, world.sizeY, world.sizeZ) {
        override fun getBlock(x: Int, y: Int, z: Int) =
            chunk[getIndex(x, y, z)].toInt()
    }
    clock.benchmark(100, 1000, "Greedy Meshing") { // 1.7ms on Ryzen 5 2600 -> ok-ish :)
        model.createMesh(null, null, null, mesh)
    }
}