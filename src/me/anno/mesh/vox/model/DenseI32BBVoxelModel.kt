package me.anno.mesh.vox.model

import java.nio.ByteBuffer

// a different implementation could use oct-trees
@Suppress("unused")
open class DenseI32BBVoxelModel(sizeX: Int, sizeY: Int, sizeZ: Int, val data: ByteBuffer) : VoxelModel(sizeX, sizeY, sizeZ) {

    override fun fill(dst: IntArray) {
        data.asIntBuffer().get(dst)
            .position(0)
    }

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        // for easy border access
        if (x !in 0 until sizeX || y !in 0 until sizeY || z !in 0 until sizeZ) return 0
        return data.getInt(getIndex(x, y, z) shl 2)
    }

}