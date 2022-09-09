package me.anno.mesh.vox.meshing

import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.joml.Vector3f
import org.joml.Vector3i

class VoxelMeshBuildInfo(
    // input
    val palette: IntArray?,
    // output
    val vertices: ExpandingFloatArray,
    val colors: ExpandingIntArray?,
    val normals: ExpandingFloatArray?
) {

    var nx = 0
    var ny = 0
    var nz = 0

    var color = 0

    var ox = 0f
    var oy = 0f
    var oz = 0f

    fun setOffset(x: Float, y: Float, z: Float) {
        ox = x
        oy = y
        oz = z
    }

    fun setNormal(side: BlockSide) {
        nx = side.x
        ny = side.y
        nz = side.z
    }

    fun add(vx: Int, vy: Int, vz: Int) {
        vertices += ox + vx
        vertices += oy + vy
        vertices += oz + vz
        if (normals != null) {
            normals.add(nx.toFloat())
            normals.add(ny.toFloat())
            normals.add(nz.toFloat())
        }
        colors?.add(color)
    }

    fun add(v: Vector3f) {
        vertices += ox + v.x
        vertices += oy + v.y
        vertices += oz + v.z
        if (normals != null) {
            normals.add(nx.toFloat())
            normals.add(ny.toFloat())
            normals.add(nz.toFloat())
        }
        colors?.add(color)
    }

    fun add(x: Float, y: Float, z: Float) {
        vertices += ox + x
        vertices += oy + y
        vertices += oz + z
        if (normals != null) {
            normals.add(nx.toFloat())
            normals.add(ny.toFloat())
            normals.add(nz.toFloat())
        }
        colors?.add(color)
    }


}