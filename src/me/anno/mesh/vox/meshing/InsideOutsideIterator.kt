package me.anno.mesh.vox.meshing

import me.anno.mesh.vox.model.VoxelModel

object InsideOutsideIterator {
    @JvmStatic
    fun forAllBlocks(
        model: VoxelModel,
        blockSide: BlockSide,
        inside: BlockCallback?,
        outside: BlockCallback?
    ) {
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
        // handle inside
        if (inside != null) {
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    for (z in z0 until z1) {
                        inside.process(x, y, z)
                    }
                }
            }
        }
        // handle outside
        if (outside != null) {
            when (blockSide) {
                BlockSide.NX, BlockSide.PX -> {
                    val negative = blockSide == BlockSide.NX
                    val x = if (negative) 0 else model.sizeX - 1
                    for (y in y0 until y1) {
                        for (z in z0 until z1) {
                            outside.process(x, y, z)
                        }
                    }
                }
                BlockSide.NY, BlockSide.PY -> {
                    val negative = blockSide == BlockSide.NY
                    val y = if (negative) 0 else model.sizeY - 1
                    for (x in x0 until x1) {
                        for (z in z0 until z1) {
                            outside.process(x, y, z)
                        }
                    }
                }
                BlockSide.NZ, BlockSide.PZ -> {
                    val negative = blockSide == BlockSide.NZ
                    val z = if (negative) 0 else model.sizeZ - 1
                    for (y in y0 until y1) {
                        for (x in x0 until x1) {
                            outside.process(x, y, z)
                        }
                    }
                }
            }
        }
    }
}