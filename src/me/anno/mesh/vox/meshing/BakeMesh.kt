package me.anno.mesh.vox.meshing

import me.anno.mesh.vox.meshing.MergeBlocks.mergeBlocks
import me.anno.mesh.vox.model.VoxelModel

object BakeMesh {

    fun getColors(model: VoxelModel, dst: VoxelMeshBuilder): IntArray {
        val colors = IntArray(model.size)
        model.fill(dst.palette, colors)
        return colors
    }

    fun getIsSolid(
        model: VoxelModel, blockSide: BlockSide,
        insideIsSolid: GetBlockId, outsideIsSolid: GetBlockId?,
        needsFace: NeedsFace?
    ): BooleanArray {
        val isSolid = BooleanArray(model.size)
        val outsideIsSolid1 = outsideIsSolid ?: GetBlockId { _, _, _ -> 0 }
        val needsFace1 = needsFace ?: NeedsFace { inside, outside -> inside != 0 && outside == 0 }
        InsideOutsideIterator.forAllBlocks(
            model, blockSide,
            fillSolid(model, blockSide, insideIsSolid, insideIsSolid, needsFace1, isSolid),
            fillSolid(model, blockSide, insideIsSolid, outsideIsSolid1, needsFace1, isSolid)
        )
        return isSolid
    }

    private fun fillSolid(
        model: VoxelModel, blockSide: BlockSide,
        insideBlocks: GetBlockId, outsideBlocks: GetBlockId,
        needsFace: NeedsFace, dstIsSolid: BooleanArray,
    ): BlockCallback {
        val dx = blockSide.x
        val dy = blockSide.y
        val dz = blockSide.z
        return BlockCallback { x, y, z ->
            dstIsSolid[model.getIndex(x, y, z)] = needsFace.needsFace(
                insideBlocks.getBlockId(x, y, z),
                outsideBlocks.getBlockId(x + dx, y + dy, z + dz)
            )
        }
    }

    private fun getInitialBlockSizes(model: VoxelModel, isSolid: BooleanArray): IntArray {
        val size000 = 0
        val size111 = getSizeInfo(1, 1, 1, model)
        return IntArray(isSolid.size) {
            if (isSolid[it]) size111 else size000
        }
    }

    private fun addFaces(
        model: VoxelModel,
        blockSizes: IntArray,
        side: BlockSide,
        dst: VoxelMeshBuilder,
        colors: IntArray
    ) {
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

    fun bakeMesh(
        model: VoxelModel,
        blockSide: BlockSide,
        dst: VoxelMeshBuilder,
        insideIsSolid: GetBlockId,
        outsideIsSolid: GetBlockId?,
        needsFace: NeedsFace?,
        colors: IntArray,
    ) {

        val isSolid = getIsSolid(model, blockSide, insideIsSolid, outsideIsSolid, needsFace)
        val blockSizes = getInitialBlockSizes(model, isSolid)

        mergeBlocks(blockSizes, colors, model)
        addFaces(model, blockSizes, blockSide, dst, colors)
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
        val ox = x - model.centerX
        val oy = y - model.centerY
        val oz = z - model.centerZ
        base.addQuad(side, ox, oy, oz, ox + sx, oy + sy, oz + sz)
        base.addColor(color, 6)
    }
}