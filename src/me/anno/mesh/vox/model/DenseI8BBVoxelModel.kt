package me.anno.mesh.vox.model

import java.nio.ByteBuffer

// a different implementation could use oct-trees
@Suppress("unused")
open class DenseI8BBVoxelModel(sizeX: Int, sizeY: Int, sizeZ: Int, val data: ByteBuffer) : VoxelModel(sizeX, sizeY, sizeZ) {

    override fun fill(dst: IntArray) {
        for (i in 0 until data.capacity()) {
            dst[i] = data[i].toInt().and(255)
        }
    }

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        // for easy border access
        if (x !in 0 until sizeX || y !in 0 until sizeY || z !in 0 until sizeZ) return 0
        return data[getIndex(x, y, z)].toInt().and(255)
    }
}