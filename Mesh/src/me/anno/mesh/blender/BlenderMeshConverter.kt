package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.gpu.CullMode
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.max
import me.anno.mesh.Triangulation
import me.anno.mesh.blender.impl.BCustomLayerType
import me.anno.mesh.blender.impl.BInstantList
import me.anno.mesh.blender.impl.BMaterial
import me.anno.mesh.blender.impl.BMesh
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.helper.IAPolyList
import me.anno.mesh.blender.impl.helper.VEJoinList
import me.anno.mesh.blender.impl.interfaces.LoopLike
import me.anno.mesh.blender.impl.interfaces.PolyLike
import me.anno.mesh.blender.impl.interfaces.UVLike
import me.anno.mesh.blender.impl.mesh.MDeformVert
import me.anno.mesh.blender.impl.mesh.MLoopUV
import me.anno.mesh.blender.impl.primitives.BVector1i
import me.anno.mesh.blender.impl.primitives.BVector2f
import me.anno.mesh.blender.impl.primitives.BVector3f
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.createArrayList
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.joml.Vector3f

object BlenderMeshConverter {

    private val LOGGER = LogManager.getLogger(BlenderMeshConverter::class)
    var maxNumTriangles = 100_000_000

    private fun flipVertices(positions: FloatArray) {
        for (i in 0 until positions.size / 3) {
            val i3 = i * 3
            val tmp = positions[i3 + 1]
            positions[i3 + 1] = positions[i3 + 2]
            positions[i3 + 2] = -tmp
        }
    }

    private fun getNewPolygons(src: BMesh): List<PolyLike> {
        val polygons0 = src.polyOffsetIndices

        @Suppress("UNCHECKED_CAST")
        val materialIndices0 = src.pData.layers
            .firstOrNull { it.name == "material_index" && it.type == BCustomLayerType.PROP_INT.id }
            ?.data as? List<BVector1i> ?: emptyList()

        return if (polygons0 != null) IAPolyList(polygons0, materialIndices0) else emptyList()
    }

    fun convertBMesh(src: BMesh): Prefab? {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("fdata: {}", src.fData)
            LOGGER.debug("edata: {}", src.eData)
            LOGGER.debug("vdata: {}", src.vData)
            LOGGER.debug("pdata: {}", src.pData)
            LOGGER.debug("ldata: {}", src.lData)
        }

        val vertices = src.vertices

        val newVertices = if (vertices == null) {
            @Suppress("UNCHECKED_CAST")
            src.vData.layers
                .firstOrNull { it.name == "position" }
                ?.data as? BInstantList<BVector3f>
        } else null
        if (vertices == null && newVertices == null) {
            LOGGER.warn("${src.id.realName} has no vertices")
            // how can there be meshes without vertices?
            // because newer versions save the data in different places
            return null
        }

        val numVertices = vertices?.size ?: newVertices!!.size
        val positions = FloatArray(numVertices * 3)
        val materials: List<BlendData?> = src.materials ?: emptyList()
        val polygons: List<PolyLike> = src.polygons ?: getNewPolygons(src)

        var loopData: List<LoopLike>? = src.loops
        if (loopData == null) {
            val lData = src.lData

            @Suppress("UNCHECKED_CAST")
            val vs = lData.layers.firstOrNull { it.name == ".corner_vert" && it.type == BCustomLayerType.PROP_INT.id }
                ?.data as? BInstantList<BVector1i>

            @Suppress("UNCHECKED_CAST")
            val es = lData.layers.firstOrNull { it.name == ".corner_edge" && it.type == BCustomLayerType.PROP_INT.id }
                ?.data as? BInstantList<BVector1i>
            loopData = if (vs != null && es != null) {
                VEJoinList(vs, es)
            } else emptyList()
        }

        val prefab = Prefab("Mesh")
        prefab["materials"] = materials.map { (it as? BMaterial)?.fileRef ?: InvalidRef }
        prefab["cullMode"] = CullMode.BOTH

        val hasNormals = vertices != null && vertices.size > 0 && vertices[0].noOffset >= 0

        var normals: FloatArray? = null
        if (hasNormals) {
            normals = FloatArray(numVertices * 3)
            for (i in 0 until numVertices) {
                val v = vertices!![i]
                val i3 = i * 3
                positions[i3] = v.x
                positions[i3 + 1] = v.y
                positions[i3 + 2] = v.z
                normals[i3] = v.nx
                normals[i3 + 1] = v.ny
                normals[i3 + 2] = v.nz
            }
        } else if (vertices != null) {
            for (i in 0 until numVertices) {
                val v = vertices[i]
                val i3 = i * 3
                positions[i3] = v.x
                positions[i3 + 1] = v.y
                positions[i3 + 2] = v.z
            }
        } else {
            newVertices!! // memcpy would work here, too
            for (i in 0 until numVertices) {
                val v = newVertices[i]
                val i3 = i * 3
                positions[i3] = v.x
                positions[i3 + 1] = v.y
                positions[i3 + 2] = v.z
            }
        }

        if (!BlenderReader.postTransform) {
            flipVertices(positions)
            if (normals != null) {
                flipVertices(normals)
            }
        }

        @Suppress("UNCHECKED_CAST")
        val newUVs0 = src.lData.layers
            .firstOrNull { it.data.firstOrNull() is MLoopUV }
            ?.data as? BInstantList<MLoopUV>

        @Suppress("UNCHECKED_CAST")
        val newUVs1 = src.lData.layers
            .firstOrNull { it.name == "UVMap" && it.data.firstOrNull() is BVector2f }
            ?.data as? BInstantList<BVector2f>

        // todo find normals for newer files; no[3] is extinct

        val uvs = src.loopUVs ?: newUVs0 ?: newUVs1 ?: emptyList()

        // todo vertex colors
        val hasUVs = uvs.any2 { it.u != 0f || it.v != 0f }
        val triCount0 = polygons.sumOf {
            when (val size = it.loopSize) {
                0 -> 0
                1, 2 -> 1
                else -> size - 2
            }.toLong()
        }

        if (triCount0 < 0 || triCount0 > maxNumTriangles) {
            LOGGER.warn("Invalid number of triangles in ${src.id.realName}: $triCount0")
            return null
        }

        val triCount = triCount0.toInt()

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
        prefab["materialIds"] = materialIndices
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
        boneIndices2: IntArrayList,
        boneWeights2: FloatArrayList,
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
        polygons: List<PolyLike>,
        loopData: List<LoopLike>,
        uvs: List<UVLike>,
        boneWeights: BInstantList<MDeformVert>?,
        numVertexGroups: Int,
        materialIndices: IntArray?,
        prefab: Prefab,
    ) {

        val positions2 = FloatArrayList(vertexCount * 3)
        val normals2 = if (normals != null) FloatArrayList(vertexCount * 3) else null
        val uvs2 = FloatArrayList(vertexCount * 2)
        val boneIndices2 = if (boneWeights != null) IntArrayList(vertexCount * 4) else null
        val boneWeights2 = if (boneWeights != null) FloatArrayList(vertexCount * 4) else null

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
            val materialIndex = polygon.materialIndex
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
                    val vectors = createArrayList(loopSize) {
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
                    val triangles = Triangulation.ringToTrianglesVec3f(vectors)
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
            prefab["rawBoneIndices"] = boneIndices2.toIntArray() // these need to be mapped
        }
    }

    fun collectIndices(
        positions: FloatArray,
        normals: FloatArray?,
        polygons: List<PolyLike>,
        loopData: List<LoopLike>,
        boneWeights: BInstantList<MDeformVert>?,
        numVertexGroups: Int,
        materialIndices: IntArray?,
        prefab: Prefab
    ) {
        // indexed, simple
        val indices = IntArrayList(polygons.size * 3)
        var matIndex = 0
        for (i in polygons.indices) {
            val polygon = polygons[i]
            val loopStart = polygon.loopStart
            val materialIndex = polygon.materialIndex
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
                    val vec2Index = HashMap<Vector3d, Int>()
                    val vectors = createArrayList(loopSize) {
                        val index = loopData[loopStart + it].v
                        val vec = Vector3d(positions, index * 3)
                        vec2Index[vec] = index
                        vec
                    }
                    val triangles = Triangulation.ringToTrianglesVec3d(vectors)
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
            val boneIndices2 = IntArrayList(vertexCount * 4)
            val boneWeights2 = FloatArrayList(vertexCount * 4)
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