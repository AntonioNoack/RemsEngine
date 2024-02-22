package me.anno.mesh.vox.meshing

import me.anno.mesh.vox.meshing.BlockBuffer.addQuad
import me.anno.mesh.vox.meshing.MergeBlocks.mergeBlocks
import me.anno.mesh.vox.model.VoxelModel
import kotlin.math.max

object BakeMesh {

    fun bakeMesh(
        model: VoxelModel,
        side: BlockSide,
        dst: VoxelMeshBuilder,
        insideIsSolid: IsSolid?,
        outsideIsSolid: IsSolid?
    ): Float {

        val colors = IntArray(model.size)
        model.fill(dst.palette, colors)

        val isSolid = BooleanArray(model.size)
        val insideIsSolid1 = insideIsSolid
            ?: IsSolid { x, y, z -> colors[model.getIndex(x, y, z)] != 0 }

        val dz = model.getIndex(0, 0, 1)
        for (y in 0 until model.sizeY) {
            for (x in 0 until model.sizeX) {
                var index = model.getIndex(x, y, 0)
                for (z in 0 until model.sizeZ) {
                    isSolid[index] = insideIsSolid1.test(x, y, z)
                    index += dz
                }
            }
        }

        val removed = removeSolidInnerBlocks(model, side, colors, isSolid, outsideIsSolid)

        val size000 = 0
        val size111 = getSizeInfo(1, 1, 1, model)

        val blockSizes = IntArray(colors.size) {
            val isEmpty = colors[it] == 0
            if (isEmpty) size000 else size111
        }

        mergeBlocks(blockSizes, colors, model)

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

        return removed
    }

    fun getSizeInfo(x: Int, y: Int, z: Int, model: VoxelModel): Int {
        val sx = model.sizeX + 1
        val sz = model.sizeZ + 1
        return ((y * sx) + x) * sz + z
    }

    fun addFaces(
        side: BlockSide, base: VoxelMeshBuilder,
        x: Int, y: Int, z: Int, size: Int, color: Int, model: VoxelModel
    ) {
        // must be the inverse of size info
        val sx0 = model.sizeX + 1
        val sz0 = model.sizeZ + 1
        val sz = size % sz0
        val sxy = size / sz0
        val sx = sxy % sx0
        val sy = sxy / sx0
        base.setOffset(x - model.centerX, y - model.centerY, z - model.centerZ)
        base.color = color
        addQuad(base, side, sx, sy, sz)
    }

    fun removeSolidInnerBlocks(
        model: VoxelModel,
        blockSide: BlockSide,
        colors: IntArray,
        isSolid: BooleanArray,
        outsideIsSolid: IsSolid?
    ): Float {
        val dx = model.getIndex(1, 0, 0)
        val dy = model.getIndex(0, 1, 0)
        val dz = model.getIndex(0, 0, 1)
        val sideOffset = blockSide.x * dx + blockSide.y * dy + blockSide.z * dz
        if (sideOffset == 0) throw IllegalStateException()
        var ctr = 0
        var x0 = 0
        var x1 = model.sizeX
        var y0 = 0
        var y1 = model.sizeY
        var z0 = 0
        var z1 = model.sizeZ
        when (blockSide) {
            BlockSide.NX -> x0++
            BlockSide.NY -> y0++
            BlockSide.NZ -> z0++
            BlockSide.PX -> x1--
            BlockSide.PY -> y1--
            BlockSide.PZ -> z1--
        }
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                var index = model.getIndex(x, y, z0)
                for (z in z0 until z1) {
                    if (colors[index] != 0) {
                        // covered
                        if (isSolid[index + sideOffset]) {
                            colors[index] = 0
                            ctr++
                        }
                    }
                    index += dz
                }
            }
        }
        // yxz
        if (outsideIsSolid != null) {
            when (blockSide) {
                BlockSide.NX, BlockSide.PX -> {
                    val negative = blockSide == BlockSide.NX
                    val i0 = if (negative) 0 else model.sizeX - 1
                    val i1 = if (negative) -1 else model.sizeX
                    for (y in y0 until y1) {
                        for (z in z0 until z1) {
                            val index = model.getIndex(i0, y, z)
                            if (colors[index] != 0 && outsideIsSolid.test(i1, y, z)) {
                                colors[index] = 0
                            }
                        }
                    }
                }
                BlockSide.NY, BlockSide.PY -> {
                    val negative = blockSide == BlockSide.NY
                    val i0 = if (negative) 0 else model.sizeY - 1
                    val i1 = if (negative) -1 else model.sizeY
                    for (x in x0 until x1) {
                        for (z in z0 until z1) {
                            val index = model.getIndex(x, i0, z)
                            if (colors[index] != 0 && outsideIsSolid.test(x, i1, z)) {
                                colors[index] = 0
                            }
                        }
                    }
                }
                BlockSide.NZ, BlockSide.PZ -> {
                    val negative = blockSide == BlockSide.NZ
                    val i0 = if (negative) 0 else model.sizeZ - 1
                    val i1 = if (negative) -1 else model.sizeZ
                    for (y in y0 until y1) {
                        for (x in x0 until x1) {
                            val index = model.getIndex(x, y, i0)
                            if (colors[index] != 0 && outsideIsSolid.test(x, y, i1)) {
                                colors[index] = 0
                            }
                        }
                    }
                }
            }
        }
        val wasSolid = isSolid.count { it }
        // LOGGER.info("Removed ${(ctr*100f/wasSolid).f1()}% of $wasSolid blocks")
        return ctr.toFloat() / max(1, wasSolid)
    }
}