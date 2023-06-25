package me.anno.mesh.vox.model

// a different implementation could use oct-trees
@Suppress("unused")
open class DenseI16VoxelModel(sizeX: Int, sizeY: Int, sizeZ: Int, val data: ShortArray) : VoxelModel(sizeX, sizeY, sizeZ) {

    override fun fill(dst: IntArray) {
        for(i in data.indices){
            dst[i] = data[i].toInt().and(0xffff)
        }
    }

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        // for easy border access
        if (x !in 0 until sizeX || y !in 0 until sizeY || z !in 0 until sizeZ) return 0
        return data[getIndex(x, y, z)].toInt().and(0xffff)
    }
}