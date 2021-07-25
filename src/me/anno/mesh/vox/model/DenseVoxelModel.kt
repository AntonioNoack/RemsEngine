package me.anno.mesh.vox.model

// a different implementation could use oct-trees
class DenseVoxelModel(sizeX: Int, sizeY: Int, sizeZ: Int, val data: ByteArray) : VoxelModel(sizeX, sizeY, sizeZ) {

    override fun fill(dst: ByteArray) {
        System.arraycopy(data, 0, dst, 0, data.size)
    }

    override fun getBlock(x: Int, y: Int, z: Int): Byte {
        // for easy border access
        if (x !in 0 until sizeX || y !in 0 until sizeY || z !in 0 until sizeZ) return 0
        return data[getIndex(x, y, z)]
    }
}