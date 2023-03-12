package me.anno.ecs.components.mesh

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.all2
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Arrays.resize
import org.joml.Matrix4x3f
import kotlin.math.abs
import kotlin.math.min

/**
 * joins multiple meshes into one;
 * like static meshes in Unity / Unreal Engine
 * */
@Suppress("unused")
abstract class MeshJoiner<V>(
    private val hasColors: Boolean,
    private val hasBones: Boolean,
    private val mayHaveUVs: Boolean
) {

    abstract fun getMesh(element: V): Mesh

    abstract fun getTransform(element: V, dst: Matrix4x3f)

    open fun getVertexColor(element: V): Int = -1

    open fun getMaterial(element: V): FileReference = InvalidRef

    open fun getBoneId(element: V): Byte = 0

    private fun is3x3Identity(t: Matrix4x3f): Boolean {
        return abs(t.m00 - 1f) + abs(t.m10) + abs(t.m20) +
                abs(t.m01) + abs(t.m11 - 1f) + abs(t.m21) +
                abs(t.m02) + abs(t.m12) + abs(t.m22 - 1f) < 1e-7f
    }

    private fun alloc(needed: Boolean, size: Int, old: FloatArray?) = if (needed) old.resize(size) else null
    private fun alloc(needed: Boolean, size: Int, old: IntArray?) = if (needed) old.resize(size) else null
    private fun alloc(needed: Boolean, size: Int, old: ByteArray?) = if (needed) old.resize(size) else null

    fun join(dstMesh: Mesh, elements: List<V>): Mesh {

        if (elements.isEmpty()) {
            dstMesh.positions = FloatArray(0)
            dstMesh.indices = null
            dstMesh.materialIds = null
            return dstMesh
        }

        var numPositions = 0
        var numTriangles = 0

        val firstMaterial = getMaterial(elements[0])
        val hasUniqueMaterial = elements.all2 { getMaterial(it) == firstMaterial }
        val materialToId: Map<FileReference, Int>?

        if (hasUniqueMaterial) {
            materialToId = null
            dstMesh.materials = listOf(firstMaterial)
        } else {
            val uniqueMaterials = elements
                .map { getMaterial(it) }
                .toHashSet().toList()
            materialToId = uniqueMaterials.withIndex().associate { it.value to it.index }
            dstMesh.materials = uniqueMaterials
        }

        for (element in elements) {
            val model = getMesh(element)
            numPositions += model.positions!!.size
            numTriangles += model.numPrimitives.toInt()
        }

        val numIndices = numTriangles * 3
        val numPositionCoords = numPositions * 3
        val numUVsCoords = numPositions * 2

        val dstPositions = alloc(true, numPositionCoords, dstMesh.positions)!!
        val dstNormals = alloc(true, numPositionCoords, dstMesh.normals)!!
        val hasUVs = mayHaveUVs && elements.any2 { getMesh(it).uvs != null }
        val dstUVs = alloc(hasUVs, numUVsCoords, dstMesh.uvs)
        val dstTangents = alloc(hasUVs, numPositions * 4, dstMesh.tangents)
        val dstColors = alloc(hasColors, numPositions, dstMesh.color0)
        val dstIndices = alloc(true, numIndices, dstMesh.indices)!!
        val dstMaterialIds = alloc(!hasUniqueMaterial, numTriangles, dstMesh.materialIds)
        val numBoneIndices = numPositions * 4
        val dstBoneIndices = alloc(hasBones, numBoneIndices, dstMesh.boneIndices)
        val dstBoneWeights = if (dstBoneIndices != null) {
            val w = alloc(true, numBoneIndices, dstMesh.boneWeights)!!
            // set every 4th value to 1
            for (i in w.indices step 4) {
                w[i] = 1f
            }
            w
        } else null

        var i = 0
        var j = 0

        val tmp = JomlPools.vec3f.create()
        val localToGlobal = JomlPools.mat4x3f.create()

        for (element in elements) {

            val srcMesh = getMesh(element)
            srcMesh.ensureBuffer() // ensure normals, tangents and such have been initialized

            val srcPositions = srcMesh.positions!!
            val srcNormals = srcMesh.normals!!
            getTransform(element, localToGlobal)
            val i0 = i

            val srcTangents = srcMesh.tangents
            if (is3x3Identity(localToGlobal)) { // fast path with translation only
                val px = localToGlobal.m30
                val py = localToGlobal.m31
                val pz = localToGlobal.m32
                for (k in srcPositions.indices step 3) {
                    dstPositions[i++] = px + srcPositions[k]
                    dstPositions[i++] = py + srcPositions[k + 1]
                    dstPositions[i++] = pz + srcPositions[k + 2]
                }
                System.arraycopy(srcNormals, 0, dstNormals, i0, srcNormals.size)
                if (dstTangents != null && srcTangents != null) {
                    val i4 = i0 / 3 * 4
                    val j4 = min(i4 + srcPositions.size / 3 * 4, dstTangents.size)
                    if (j4 > i4) System.arraycopy(srcTangents, 0, dstTangents, i4, j4 - i4)
                }
            } else { // slow path
                var i4 = i0 / 3 * 4
                var j4 = 0
                for (k in srcPositions.indices step 3) {
                    tmp.set(
                        srcPositions[k],
                        srcPositions[k + 1],
                        srcPositions[k + 2]
                    )
                    localToGlobal.transformPosition(tmp)
                    dstPositions[i++] = tmp.x
                    dstPositions[i++] = tmp.y
                    dstPositions[i++] = tmp.z
                    tmp.set(srcNormals[k], srcNormals[k + 1], srcNormals[k + 2])
                    localToGlobal.transformDirection(tmp)
                    dstNormals[i - 3] = tmp.x
                    dstNormals[i - 2] = tmp.y
                    dstNormals[i - 1] = tmp.z
                    if (dstTangents != null && srcTangents != null) {
                        tmp.set(srcTangents[j4++], srcTangents[j4++], srcTangents[j4++])
                        localToGlobal.transformDirection(tmp)
                        dstTangents[i4++] = tmp.x
                        dstTangents[i4++] = tmp.y
                        dstTangents[i4++] = tmp.z
                        dstTangents[i4++] = srcTangents[j4++]
                    }
                }
            }
            val meshUVs = srcMesh.uvs
            if (dstUVs != null && meshUVs != null) {
                val i2 = i0 / 3 * 2
                val j2 = min(i2 + meshUVs.size, dstUVs.size)
                if (j2 > i2) System.arraycopy(meshUVs, 0, dstUVs, i2, j2 - i2)
            }
            if (dstBoneIndices != null) {
                val j0 = i0 / 3 * 4
                val dataSize = srcPositions.size / 3 * 4
                val srcWeights = srcMesh.boneWeights
                val srcIndices = srcMesh.boneIndices
                if (srcWeights != null || srcIndices != null) {
                    if (srcWeights != null)
                        System.arraycopy(srcWeights, 0, dstBoneWeights!!, j0, min(dataSize, srcWeights.size))
                    if (srcIndices != null)
                        System.arraycopy(srcIndices, 0, dstBoneIndices, j0, min(dataSize, srcIndices.size))
                } else {
                    val boneId = getBoneId(element)
                    for (k in 0 until dataSize step 4) {
                        dstBoneIndices[j0 + k] = boneId
                    }
                }
            }
            dstColors?.fill(getVertexColor(element), i0 / 3, (i0 + srcNormals.size) / 3)
            val indices2 = srcMesh.indices
            val baseIndex = i0 / 3
            val j0 = j
            if (indices2 != null) {
                for (k in indices2.indices) {
                    dstIndices[j++] = baseIndex + indices2[k]
                }
            } else {
                for (k in 0 until srcPositions.size / 3) {
                    dstIndices[j++] = baseIndex + k
                }
            }
            if (dstMaterialIds != null) {
                val materialId = materialToId!![getMaterial(element)] ?: 0
                dstMaterialIds.fill(materialId, j0 / 3, j / 3)
            }
        }

        JomlPools.mat4x3f.sub(1)
        JomlPools.vec3f.sub(1)

        dstMesh.positions = dstPositions
        dstMesh.indices = dstIndices
        dstMesh.normals = dstNormals
        dstMesh.uvs = dstUVs
        dstMesh.tangents = dstTangents
        dstMesh.color0 = dstColors
        dstMesh.boneWeights = dstBoneWeights
        dstMesh.boneIndices = dstBoneIndices
        dstMesh.materialIds = dstMaterialIds

        return dstMesh

    }

}