package me.anno.mesh.vox.model

// a different implementation could use oct-trees
@Suppress("unused")
class DenseI32VoxelModel(sizeX: Int, sizeY: Int, sizeZ: Int, val data: IntArray) : VoxelModel(sizeX, sizeY, sizeZ) {

    override fun fill(dst: IntArray) {
        for(i in data.indices){
            dst[i] = data[i]
        }
    }

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        // for easy border access
        if (x !in 0 until sizeX || y !in 0 until sizeY || z !in 0 until sizeZ) return 0
        return data[getIndex(x, y, z)]
    }
}