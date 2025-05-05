package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.utils.structures.arrays.ByteArrayList
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

open class MeshBuilder(flags: Int) {

    companion object {
        const val WITH_NORMALS = 1
        const val WITH_TANGENTS = 2
        const val WITH_COLORS = 4
        const val WITH_UVS = 8
        const val WITH_INDICES = 16
        const val WITH_BONES = 32
    }

    constructor(vc: Mesh) : this(
        (vc.normals != null).toInt(WITH_NORMALS) or
                (vc.tangents != null).toInt(WITH_TANGENTS) or
                (vc.color0 != null).toInt(WITH_COLORS) or
                (vc.uvs != null).toInt(WITH_UVS) or
                (vc.boneIndices != null).toInt(WITH_BONES)
        // indices are excluded on purpose!
    )

    val positions = FloatArrayList(64)
    val normals = if (flags.hasFlag(WITH_NORMALS)) FloatArrayList(64) else null
    val tangents = if (flags.hasFlag(WITH_TANGENTS)) FloatArrayList(64) else null
    val colors = if (flags.hasFlag(WITH_COLORS)) IntArrayList(64) else null
    val uvs = if (flags.hasFlag(WITH_UVS)) FloatArrayList(64) else null
    val indices = if (flags.hasFlag(WITH_INDICES)) IntArrayList(64) else null

    val boneWeights = if (flags.hasFlag(WITH_BONES)) FloatArrayList(64) else null
    val boneIndices = if (flags.hasFlag(WITH_BONES)) ByteArrayList(64) else null

    fun add(mesh: Mesh, vertexIndex: Int) {
        positions.addAll(mesh.positions!!, vertexIndex * 3, 3)
        normals?.addAll(mesh.normals!!, vertexIndex * 3, 3)
        tangents?.addAll(mesh.tangents!!, vertexIndex * 4, 4)
        colors?.add(mesh.color0!![vertexIndex])
        uvs?.addAll(mesh.uvs!!, vertexIndex * 2, 2)

        boneIndices?.addAll(mesh.boneIndices!!, vertexIndex * 4, 4)
        boneWeights?.addAll(mesh.boneWeights!!, vertexIndex * 4, 4)
    }

    fun build(dst: Mesh = Mesh()): Mesh {
        dst.positions = positions.toFloatArray()
        dst.normals = normals?.toFloatArray()
        dst.uvs = uvs?.toFloatArray()
        dst.color0 = colors?.toIntArray()
        dst.tangents = tangents?.toFloatArray()

        dst.boneWeights = boneWeights?.toFloatArray()
        dst.boneIndices = boneIndices?.toByteArray()
        dst.indices = indices?.toIntArray()
        dst.invalidateGeometry()
        return dst
    }
}