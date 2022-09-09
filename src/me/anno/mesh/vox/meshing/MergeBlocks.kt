package me.anno.mesh.vox.meshing

import me.anno.mesh.vox.model.VoxelModel
import me.anno.mesh.vox.meshing.BakeMesh.getSizeInfo

object MergeBlocks {

    fun mergeBlocks(
        blocks: IntArray,
        colors: IntArray,
        model: VoxelModel
    ) {

        val sx = model.sizeX
        val sy = model.sizeY
        val sz = model.sizeZ

        // checking lights is too complicated, except they are in full darkness,
        // which would be caves; but caves are always irregular anyway ->
        // this functionality is useless for this use-case and realization
        // we need to do this differently

        // join z
        for (y in 0 until sy) {
            for (x in 0 until sx) {
                var z = -1
                while (++z < sz - 1) {
                    val index0 = model.getIndex(x, y, z)
                    if (blocks[index0] != 0) {
                        val hereType = colors[index0]
                        val hereBlock = blocks[index0]
                        val startZ = z
                        while (++z < sz) {
                            val indexI = model.getIndex(x, y, z)
                            if (colors[indexI] != hereType || blocks[indexI] != hereBlock) break
                        }
                        if (z > startZ + 1) {
                            blocks[index0] = getSizeInfo(1, 1, z - startZ, model)
                            for (zi in startZ + 1 until z) {
                                blocks[model.getIndex(x, y, zi)] = 0
                            }
                        }
                    }
                }
            }
        }

        // join x
        for (y in 0 until sy) {
            for (z in 0 until sz) {
                var x = -1
                while (++x < sx - 1) {
                    val index0 = model.getIndex(x, y, z)
                    if (blocks[index0] != 0) {
                        val hereType = colors[index0]
                        val hereBlock = blocks[index0]
                        val startX = x
                        while (++x < sx) {
                            val indexI = model.getIndex(x, y, z)
                            if (colors[indexI] != hereType || blocks[indexI] != hereBlock) break
                        }
                        if (x > startX + 1) {
                            val oldSZ = hereBlock % (sz + 1)
                            blocks[index0] = getSizeInfo(x - startX, 1, oldSZ, model)
                            for (xi in startX + 1 until x) {
                                blocks[model.getIndex(xi, y, z)] = 0
                            }
                        }
                    }
                }
            }
        }

        // join y
        for (x in 0 until sx) {
            for (z in 0 until sz) {
                var y = -1
                while (++y < sy - 1) {
                    val index0 = model.getIndex(x, y, z)
                    if (blocks[index0] != 0) {
                        val hereType = colors[index0]
                        val hereBlock = blocks[index0]
                        val startY = y
                        while (++y < sy) {
                            val indexI = model.getIndex(x, y, z)
                            if (colors[indexI] != hereType || blocks[indexI] != hereBlock) break
                        }
                        if (y > startY + 1) {
                            val oldSX = (hereBlock / (sz + 1)) % (sx + 1)
                            val oldSZ = hereBlock % (sz + 1)
                            blocks[index0] = getSizeInfo(oldSX, y - startY, oldSZ, model)
                            for (yi in startY + 1 until y) {
                                blocks[model.getIndex(x, yi, z)] = 0
                            }
                        }
                    }
                }
            }
        }

    }

}