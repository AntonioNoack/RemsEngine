package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.structures.arrays.ByteArrayList
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList

open class MeshBuilder(vc: Mesh) {

    val positions = FloatArrayList(64)
    val normals = if (vc.normals != null) FloatArrayList(64) else null
    val tangents = if (vc.tangents != null) FloatArrayList(64) else null
    val colors = if (vc.color0 != null) IntArrayList(64) else null
    val uvs = if (vc.uvs != null) FloatArrayList(64) else null

    val boneWeights = if (vc.boneWeights != null) FloatArrayList(64) else null
    val boneIndices = if (vc.boneIndices != null) ByteArrayList(64) else null

    fun add(mesh: Mesh, i: Int) {
        positions.add(mesh.positions!!, i * 3, 3)
        normals?.add(mesh.normals!!, i * 3, 3)
        tangents?.add(mesh.tangents!!, i * 4, 4)
        colors?.add(mesh.color0!![i])
        uvs?.add(mesh.uvs!!, i * 2, 2)

        boneIndices?.add(mesh.boneIndices!!, i * 4, 4)
        boneWeights?.add(mesh.boneWeights!!, i * 4, 4)
    }

    fun build(mesh: Mesh = Mesh()): Mesh {
        mesh.positions = positions.toFloatArray()
        mesh.normals = normals?.toFloatArray()
        mesh.uvs = uvs?.toFloatArray()
        mesh.color0 = colors?.toIntArray()
        mesh.tangents = tangents?.toFloatArray()

        mesh.boneWeights = boneWeights?.toFloatArray()
        mesh.boneIndices = boneIndices?.toByteArray()
        mesh.indices = null
        mesh.invalidateGeometry()
        return mesh
    }
}