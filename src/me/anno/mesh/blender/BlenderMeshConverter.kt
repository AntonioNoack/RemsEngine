package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.fonts.mesh.Triangulation
import me.anno.gpu.CullMode
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.max
import me.anno.mesh.blender.impl.*
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f

object BlenderMeshConverter {

    private val LOGGER = LogManager.getLogger(BlenderMeshConverter::class)

    fun convertBMesh(src: BMesh): Prefab? {

        val vertices = src.vertices ?: return null // how can there be meshes without vertices?
        val positions = FloatArray(vertices.size * 3)
        val materials = src.materials ?: emptyArray()
        val polygons = src.polygons ?: BInstantList.emptyList()
        val loopData = src.loops ?: BInstantList.emptyList()

        val prefab = Prefab("Mesh")
        prefab["materials"] = materials.map { it as BMaterial?; it?.fileRef ?: InvalidRef }
        prefab["cullMode"] = CullMode.BOTH

        val hasNormals = vertices.size > 0 && vertices[0].noOffset >= 0
        val normals: FloatArray? = if (BlenderReader.postTransform) {
            if (hasNormals) {
                val normals = FloatArray(vertices.size * 3)
                for (i in 0 until vertices.size) {
                    val v = vertices[i]
                    val i3 = i * 3
                    positions[i3] = v.x
                    positions[i3 + 1] = v.y
                    positions[i3 + 2] = v.z
                    normals[i3] = v.nx
                    normals[i3 + 1] = v.ny
                    normals[i3 + 2] = v.nz
                }
                normals
            } else {
                for (i in 0 until vertices.size) {
                    val v = vertices[i]
                    val i3 = i * 3
                    positions[i3] = v.x
                    positions[i3 + 1] = v.y
                    positions[i3 + 2] = v.z
                }
                null
            }
        } else {
            if (hasNormals) {
                val normals = FloatArray(vertices.size * 3)
                for (i in 0 until vertices.size) {
                    val v = vertices[i]
                    val i3 = i * 3
                    positions[i3] = v.x
                    positions[i3 + 1] = +v.z
                    positions[i3 + 2] = -v.y
                    normals[i3] = v.nx
                    normals[i3 + 1] = +v.nz
                    normals[i3 + 2] = -v.ny
                }
                normals
            } else {
                for (i in 0 until vertices.size) {
                    val v = vertices[i]
                    val i3 = i * 3
                    positions[i3] = v.x
                    positions[i3 + 1] = +v.z
                    positions[i3 + 2] = -v.y
                }
                null
            }
        }

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug(src.fData.layers)
            LOGGER.debug(src.eData.layers)
            LOGGER.debug(src.vData.layers)
            LOGGER.debug(src.pData.layers)
            LOGGER.debug(src.lData.layers)
        }

        val newUVs0 = src.lData.layers
            .firstOrNull { it.data.firstOrNull() is MLoopUV }

        // todo find normals for newer files; no[3] is extinct

        @Suppress("UNCHECKED_CAST")
        val newUVs = newUVs0?.data as? BInstantList<MLoopUV>

        val uvs = src.loopUVs ?: newUVs ?: BInstantList.emptyList()
        // LOGGER.info("UVs: ${src.loopUVs} ?: ${src.lData.layers.map { it.name }} ?: empty")

        // todo vertex colors
        val hasUVs = uvs.any { it.u != 0f || it.v != 0f }
        val triCount = polygons.sumOf {
            when (val size = it.loopSize) {
                0 -> 0
                1, 2 -> 1
                else -> size - 2
            }
        }

        val boneWeights = src.vertexGroups
        val materialIndices = if (materials.size > 1) IntArray(triCount) else null
        val numVertexGroups = boneWeights?.size ?: 0
        if (hasUVs) {// non-indexed, because we don't support separate uv and position indices
            joinPositionsAndUVs(
                triCount * 3,
                positions, normals,
                polygons, loopData, uvs,
                boneWeights, numVertexGroups,
                materialIndices, prefab
            )
        } else {
            collectIndices(
                positions, normals,
                polygons, loopData,
                boneWeights, numVertexGroups,
                materialIndices, prefab
            )
        }
        if (materialIndices != null) prefab["materialIds"] = materialIndices
        prefab.sealFromModifications()
        return prefab
    }

    fun addBoneWeight(gi: Int, gw: Float, bestBones: IntArray, bestWeights: FloatArray) {
        for (i in 0 until 4) {
            if (gw > bestWeights[i]) {
                // move all other weights back
                for (j in 3 downTo i + 1) {
                    bestWeights[j] = bestWeights[j - 1]
                    bestBones[j] = bestBones[j - 1]
                }
                bestBones[i] = gi
                bestWeights[i] = gw
                return
            }
        }
    }

    fun addBoneWeights(
        boneWeights: BInstantList<MDeformVert>, vi: Int,
        bestBones: IntArray, bestWeights: FloatArray,
        numVertexGroups: Int
    ) {
        for (w in boneWeights[vi].weights) {
            val gi = w.vertexGroupIndex
            val gw = w.weight
            if (gi in 0 until numVertexGroups) {
                addBoneWeight(gi, gw, bestBones, bestWeights)
            }
        }
    }

    fun fillInBones(
        boneWeights: BInstantList<MDeformVert>, vi: Int,
        bestBones: IntArray, bestWeights: FloatArray,
        boneIndices2: ExpandingIntArray,
        boneWeights2: ExpandingFloatArray,
        numVertexGroups: Int
    ) {
        addBoneWeights(boneWeights, vi, bestBones, bestWeights, numVertexGroups)

        // then assign their weights
        val weightSum = 1f / max(1e-38f, (bestWeights[0] + bestWeights[1]) + (bestWeights[2] + bestWeights[3]))
        for (i in 0 until 4) {
            boneIndices2.addUnsafe(bestBones[i])
            boneWeights2.addUnsafe(bestWeights[i] * weightSum)
        }

        bestWeights.fill(0f)
        bestBones.fill(0)
    }

    fun joinPositionsAndUVs(
        vertexCount: Int,
        positions: FloatArray,
        normals: FloatArray?,
        polygons: BInstantList<MPoly>,
        loopData: BInstantList<MLoop>,
        uvs: BInstantList<MLoopUV>,
        boneWeights: BInstantList<MDeformVert>?,
        numVertexGroups: Int,
        materialIndices: IntArray?,
        prefab: Prefab,
    ) {

        val positions2 = ExpandingFloatArray(vertexCount * 3)
        val normals2 = if (normals != null) ExpandingFloatArray(vertexCount * 3) else null
        val uvs2 = ExpandingFloatArray(vertexCount * 2)
        val boneIndices2 = if (boneWeights != null) ExpandingIntArray(vertexCount * 4) else null
        val boneWeights2 = if (boneWeights != null) ExpandingFloatArray(vertexCount * 4) else null

        var uvIndex = 0
        var matIndex = 0

        val bestBones = IntArray(4)
        val bestWeights = FloatArray(4)
        val numUvs = uvs.size

        fun addTriangle(
            v0: Int, v1: Int, v2: Int,
            uv0: Int, uv1: Int, uv2: Int,
        ) {

            val v03 = v0 * 3
            val v13 = v1 * 3
            val v23 = v2 * 3

            // positions
            positions2.addUnsafe(positions, v03, 3)
            positions2.addUnsafe(positions, v13, 3)
            positions2.addUnsafe(positions, v23, 3)

            // normals
            if (normals != null && normals2 != null) {
                normals2.addUnsafe(normals, v03, 3)
                normals2.addUnsafe(normals, v13, 3)
                normals2.addUnsafe(normals, v23, 3)
            }

            // uvs
            if (uv0 < numUvs && uv1 < numUvs && uv2 < numUvs) {
                val uv0x = uvs[uv0]
                uvs2.addUnsafe(uv0x.u)
                uvs2.addUnsafe(uv0x.v)
                val uv1x = uvs[uv1]
                uvs2.addUnsafe(uv1x.u)
                uvs2.addUnsafe(uv1x.v)
                val uv2x = uvs[uv2]
                uvs2.addUnsafe(uv2x.u)
                uvs2.addUnsafe(uv2x.v)
            } else {
                uvs2.skip(6)
            }

            // bone weights / indices
            if (boneWeights != null &&
                boneWeights2 != null &&
                boneIndices2 != null
            ) {
                boneIndices2.ensureExtra(4 * 3)
                fillInBones(boneWeights, v0, bestBones, bestWeights, boneIndices2, boneWeights2, numVertexGroups)
                fillInBones(boneWeights, v1, bestBones, bestWeights, boneIndices2, boneWeights2, numVertexGroups)
                fillInBones(boneWeights, v2, bestBones, bestWeights, boneIndices2, boneWeights2, numVertexGroups)
            }
        }

        for (i in polygons.indices) {
            val polygon = polygons[i]
            val loopStart = polygon.loopStart
            val materialIndex = polygon.materialIndex.toUShort().toInt()
            when (val loopSize = polygon.loopSize) {
                0 -> {
                }
                1 -> {// point
                    val v = loopData[loopStart].v
                    val uv = uvIndex++
                    addTriangle(v, v, v, uv, uv, uv)
                    materialIndices?.set(matIndex++, materialIndex)
                }
                2 -> {// line
                    val v0 = loopData[loopStart].v
                    val v1 = loopData[loopStart + 1].v
                    val uv0 = uvIndex++
                    val uv1 = uvIndex++
                    addTriangle(v0, v1, v1, uv0, uv1, uv1)
                    materialIndices?.set(matIndex++, materialIndex)
                }
                3 -> {// triangle
                    val v0 = loopData[loopStart].v
                    val v1 = loopData[loopStart + 1].v
                    val v2 = loopData[loopStart + 2].v
                    val uv0 = uvIndex++
                    val uv1 = uvIndex++
                    val uv2 = uvIndex++
                    addTriangle(v0, v1, v2, uv0, uv1, uv2)
                    materialIndices?.set(matIndex++, materialIndex)
                }
                4 -> {// quad, simple
                    val v0 = loopData[loopStart].v
                    val v1 = loopData[loopStart + 1].v
                    val v2 = loopData[loopStart + 2].v
                    val v3 = loopData[loopStart + 3].v
                    val uv0 = uvIndex++
                    val uv1 = uvIndex++
                    val uv2 = uvIndex++
                    val uv3 = uvIndex++
                    addTriangle(v0, v1, v2, uv0, uv1, uv2)
                    addTriangle(v2, v3, v0, uv2, uv3, uv0)
                    if (materialIndices != null) {
                        materialIndices[matIndex++] = materialIndex
                        materialIndices[matIndex++] = materialIndex
                    }
                }
                else -> {
                    //complexCtr++
                    // complex triangulation, because it may be more complicated than it seems, and
                    // we have to be correct
                    val vec2Index = HashMap<Vector3f, Int>()
                    val vectors = Array(loopSize) {
                        val index = (loopData[loopStart + it]).v
                        val vec = Vector3f(
                            positions[index * 3],
                            positions[index * 3 + 1],
                            positions[index * 3 + 2]
                        )
                        vec2Index[vec] = it
                        vec
                    }
                    val uvIndex0 = uvIndex
                    uvIndex += loopSize
                    val triangles = Triangulation.ringToTrianglesVec3f(vectors.toList())
                    for (idx0 in triangles.indices step 3) {
                        val i0 = vec2Index[triangles[idx0]]!!
                        val i1 = vec2Index[triangles[idx0 + 1]]!!
                        val i2 = vec2Index[triangles[idx0 + 2]]!!
                        val v0 = (loopData[loopStart + i0]).v
                        val v1 = (loopData[loopStart + i1]).v
                        val v2 = (loopData[loopStart + i2]).v
                        val uv0 = uvIndex0 + i0
                        val uv1 = uvIndex0 + i1
                        val uv2 = uvIndex0 + i2
                        addTriangle(v0, v1, v2, uv0, uv1, uv2)
                    }
                    if (materialIndices != null) {
                        for (idx0 in triangles.indices step 3) {
                            materialIndices[matIndex++] = materialIndex
                        }
                    }
                }
            }
        }
        prefab["positions"] = positions2.toFloatArray()
        prefab["uvs"] = uvs2.toFloatArray()
        if (normals2 != null) {
            prefab["normals"] = normals2.toFloatArray()
            LOGGER.debug("Normals: ${normals2.toFloatArray().joinToString()}")
        }
        if (boneWeights2 != null && boneIndices2 != null) {
            prefab["boneWeights"] = boneWeights2.toFloatArray()
            prefab["boneIndices"] = boneIndices2.toIntArray()
        }
    }

    fun collectIndices(
        positions: FloatArray,
        normals: FloatArray?,
        polygons: BInstantList<MPoly>,
        loopData: BInstantList<MLoop>,
        boneWeights: BInstantList<MDeformVert>?,
        numVertexGroups: Int,
        materialIndices: IntArray?,
        prefab: Prefab
    ) {
        // indexed, simple
        val indices = ExpandingIntArray(polygons.size * 3)
        var matIndex = 0
        for (i in polygons.indices) {
            val polygon = polygons[i]
            val loopStart = polygon.loopStart
            val materialIndex = polygon.materialIndex.toUShort().toInt()
            when (val loopSize = polygon.loopSize) {
                0 -> {
                }
                1 -> {// point
                    val v = loopData[loopStart].v
                    indices.add(v)
                    indices.add(v)
                    indices.add(v)
                    materialIndices?.set(matIndex++, materialIndex)
                }
                2 -> {// line
                    val v0 = loopData[loopStart].v
                    val v1 = loopData[loopStart + 1].v
                    indices.add(v0)
                    indices.add(v1)
                    indices.add(v1)
                    materialIndices?.set(matIndex++, materialIndex)
                }
                3 -> {// triangle
                    indices.add(loopData[loopStart].v)
                    indices.add(loopData[loopStart + 1].v)
                    indices.add(loopData[loopStart + 2].v)
                    materialIndices?.set(matIndex++, materialIndex)
                }
                4 -> {// quad, simple
                    val v0 = loopData[loopStart].v
                    val v1 = loopData[loopStart + 1].v
                    val v2 = loopData[loopStart + 2].v
                    val v3 = loopData[loopStart + 3].v
                    indices.add(v0)
                    indices.add(v1)
                    indices.add(v2)
                    indices.add(v2)
                    indices.add(v3)
                    indices.add(v0)
                    materialIndices?.set(matIndex++, materialIndex)
                    materialIndices?.set(matIndex++, materialIndex)
                }
                else -> {
                    // complex triangulation, because it may be more complicated than it seems, and
                    // we have to be correct
                    val vec2Index = HashMap<Vector3f, Int>()
                    val vectors = Array(loopSize) {
                        val index = loopData[loopStart + it].v
                        val vec = Vector3f().set(positions, index * 3)
                        vec2Index[vec] = index
                        vec
                    }
                    val triangles = Triangulation.ringToTrianglesVec3f(vectors.toList())
                    for (tri in triangles) {
                        indices.add(vec2Index[tri]!!)
                    }
                    for (j in triangles.indices step 3) {
                        materialIndices?.set(matIndex++, materialIndex)
                    }
                }
            }
        }
        prefab["positions"] = positions
        if (normals != null) {
            prefab["normals"] = normals
            LOGGER.debug("Normals: ${normals.joinToString()}")
        }
        prefab["indices"] = indices.toIntArray()
        if (boneWeights != null) {
            val vertexCount = positions.size / 3
            val boneIndices2 = ExpandingIntArray(vertexCount * 4)
            val boneWeights2 = ExpandingFloatArray(vertexCount * 4)
            val bestBones = IntArray(4)
            val bestWeights = FloatArray(4)
            for (i in 0 until vertexCount) {
                fillInBones(
                    boneWeights, i, bestBones, bestWeights,
                    boneIndices2, boneWeights2, numVertexGroups
                )
            }
            prefab["boneWeights"] = boneWeights2.toFloatArray()
            prefab["boneIndices"] = boneIndices2.toIntArray()
        }
    }
}