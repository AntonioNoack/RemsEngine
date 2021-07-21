package me.anno.mesh.vox.meshing

import me.anno.mesh.vox.VoxelModel
import me.anno.mesh.vox.meshing.BlockBuffer.addQuad
import me.anno.mesh.vox.meshing.MergeBlocks.mergeBlocks

object BakeMesh {

    fun bakeMesh(
        model: VoxelModel,
        side: BlockSide,
        dst: VoxelMeshBuildInfo
    ) {

        val colors = ByteArray(model.size)
        model.fill(colors)

        val isSolid = BooleanArray(model.size)
        for (i in isSolid.indices) isSolid[i] = colors[i] != 0.toByte()
        removeSolidInnerBlocks(model, side, colors, isSolid)

        lateinit var blockSizes: IntArray

        val zero = 0.toByte()
        val size111 = getSizeInfo(1, 1, 1, model)

        val size000 = 0
        blockSizes = IntArray(colors.size) {
            val isEmpty = colors[it] == zero
            if (isEmpty) size000 else size111
        }

        mergeBlocks(blockSizes, colors, model)

        val dz = model.getIndex(0, 0, 1)

        for (y in 0 until model.sizeY) {
            for (x in 0 until model.sizeX) {
                var index = model.getIndex(x, y, 0)
                for (z in 0 until model.sizeZ) {
                    val blockSize = blockSizes[index]
                    if (blockSize > 0) {
                        addFaces(side, dst, x, y, z, blockSize, colors[index], model)
                    }
                    index += dz
                }
            }
        }

    }

    fun getSizeInfo(x: Int, y: Int, z: Int, model: VoxelModel): Int {
        val sx = model.sizeX + 1
        val sz = model.sizeZ + 1
        return ((y * sx) + x) * sz + z
    }

    fun addFaces(
        side: BlockSide, base: VoxelMeshBuildInfo,
        x: Int, y: Int, z: Int, size: Int, color: Byte, model: VoxelModel
    ) {
        // must be the inverse of size info
        val sx0 = model.sizeX + 1
        val sz0 = model.sizeZ + 1
        val sz = size % sz0
        val sxy = size / sz0
        val sx = sxy % sx0
        val sy = sxy / sx0
        base.setOffset(x - model.centerX, y - model.centerY, z - model.centerZ)
        base.setColor(color)
        addQuad(base, side, sx, sy, sz)
    }

    fun removeSolidInnerBlocks(
        model: VoxelModel,
        blockSide: BlockSide,
        colors: ByteArray,
        isSolid: BooleanArray
    ) {
        val dx = model.getIndex(1, 0, 0)
        val dy = model.getIndex(0, 1, 0)
        val dz = model.getIndex(0, 0, 1)
        if (dx == 0 || dy == 0 || dz == 0) throw IllegalStateException()
        val sideOffset = blockSide.x * dx + blockSide.y * dy + blockSide.z * dz
        if (sideOffset == 0) throw IllegalStateException()
        val zero = 0.toByte()
        for (y in 1 until model.sizeY - 1) {
            for (x in 1 until model.sizeX - 1) {
                var index = model.getIndex(x, y, 1)
                for (z in 1 until model.sizeZ - 1) {
                    if (colors[index] != zero && !isSolid[index]) {
                        if ( // completely covered &&
                            isSolid[index - dz] && isSolid[index + dz] &&
                            isSolid[index + dx] && isSolid[index - dx] &&
                            isSolid[index - dy] && isSolid[index + dy]
                        ) {
                            colors[index] = 0
                        }
                    }
                    index += dz
                }
            }
        }
    }

}