package me.anno.mesh.vox.model

import kotlin.math.min

// a different implementation could use oct-trees
@Suppress("unused")
open class DenseI32VoxelModel(sizeX: Int, sizeY: Int, sizeZ: Int, val data: IntArray) : VoxelModel(sizeX, sizeY, sizeZ) {

    override fun fill(dst: IntArray) {
        data.copyInto(dst, 0, 0, min(data.size, dst.size))
    }

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        // for easy border access
        if (x !in 0 until sizeX || y !in 0 until sizeY || z !in 0 until sizeZ) return 0
        return data[getIndex(x, y, z)]
    }

}