package me.anno.mesh.vox.meshing

import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3f

class VoxelMeshBuilder(
    // input
    val palette: IntArray?,
    // output
    val vertices: FloatArrayList,
    val colors: IntArrayList?,
    val normals: FloatArrayList?
) {

    var side: BlockSide = BlockSide.NX

    var color = 0

    var ox = 0f
    var oy = 0f
    var oz = 0f

    fun setOffset(x: Float, y: Float, z: Float) {
        ox = x
        oy = y
        oz = z
    }

    fun add(vx: Int, vy: Int, vz: Int) {
        vertices += ox + vx
        vertices += oy + vy
        vertices += oz + vz
        if (normals != null) {
            normals.add(side.x.toFloat())
            normals.add(side.y.toFloat())
            normals.add(side.z.toFloat())
        }
        colors?.add(color)
    }

    fun add(v: Vector3f) {
        vertices += ox + v.x
        vertices += oy + v.y
        vertices += oz + v.z
        if (normals != null) {
            normals.add(side.x.toFloat())
            normals.add(side.y.toFloat())
            normals.add(side.z.toFloat())
        }
        colors?.add(color)
    }

    fun add(x: Float, y: Float, z: Float) {
        vertices += ox + x
        vertices += oy + y
        vertices += oz + z
        if (normals != null) {
            normals.add(side.x.toFloat())
            normals.add(side.y.toFloat())
            normals.add(side.z.toFloat())
        }
        colors?.add(color)
    }


}