package me.anno.mesh.vox.meshing

import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.addUnsafe
import me.anno.utils.structures.arrays.IntArrayList

class VoxelMeshBuilder(
    // input
    val palette: IntArray?,
    // output
    val vertices: FloatArrayList,
    val colors: IntArrayList?,
    val normals: FloatArrayList?
) {

    fun finishSide(side: BlockSide) {
        val normals = normals ?: return
        val sx = side.x.toFloat()
        val sy = side.y.toFloat()
        val sz = side.z.toFloat()
        val dstSize = vertices.size
        normals.ensureCapacity(dstSize)
        val dst = normals.values
        var i = normals.size
        while (i < dstSize) {
            dst[i++] = sx
            dst[i++] = sy
            dst[i++] = sz
        }
        normals.size = i
    }

    fun addColor(color: Int, times: Int) {
        val colors = colors ?: return
        val oldSize = colors.size
        val newSize = oldSize + times
        colors.ensureCapacity(newSize)
        colors.values.fill(color, oldSize, newSize)
        colors.size = newSize
    }

    fun addQuad(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float
    ) {
        val vertices = vertices
        vertices.ensureExtra(6 * 3)
        vertices.addUnsafe(ax, ay, az)
        vertices.addUnsafe(cx, cy, cz)
        vertices.addUnsafe(bx, by, bz)
        vertices.addUnsafe(ax, ay, az)
        vertices.addUnsafe(dx, dy, dz)
        vertices.addUnsafe(cx, cy, cz)
    }

    fun addQuad(
        side: BlockSide,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float
    ) {
        when (side) {
            BlockSide.NX -> addQuad(
                x0, y0, z0,
                x0, y1, z0,
                x0, y1, z1,
                x0, y0, z1
            )
            BlockSide.PX -> addQuad(
                x1, y1, z0,
                x1, y0, z0,
                x1, y0, z1,
                x1, y1, z1
            )
            BlockSide.NY -> addQuad(
                x0, y0, z0,
                x0, y0, z1,
                x1, y0, z1,
                x1, y0, z0
            )
            BlockSide.PY -> addQuad(
                x0, y1, z1,
                x0, y1, z0,
                x1, y1, z0,
                x1, y1, z1
            )
            BlockSide.NZ -> addQuad(
                x0, y1, z0,
                x0, y0, z0,
                x1, y0, z0,
                x1, y1, z0
            )
            BlockSide.PZ -> addQuad(
                x0, y0, z1,
                x0, y1, z1,
                x1, y1, z1,
                x1, y0, z1
            )
        }
    }
}