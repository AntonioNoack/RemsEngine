package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.fonts.mesh.Triangulation
import me.anno.gpu.CullMode
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.impl.*
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f

object BlenderMeshConverter {

    private val LOGGER = LogManager.getLogger(BlenderMeshConverter::class)

    fun convertBMesh(src: BMesh): Prefab? {

        if (LOGGER.isDebugEnabled) {
            // todo find the assigned Armature modifier to extract the bone hierarchy, so we can sort the bones properly
            //  (in case they are not; haven't found confirmation for them being in the correct order yet)
            println("[BlenderMeshConverter] Converting Mesh")
            println(src.vertexGroupNames) // = bone names
            println(src.oldVertexGroups) // = bone indices and bone weights :3
        }

        val vertices = src.vertices ?: return null // how can there be meshes without vertices?
        val positions = FloatArray(vertices.size * 3)
        val materials = src.materials ?: emptyArray()
        val polygons = src.polygons ?: BInstantList.emptyList()
        val loopData = src.loops ?: BInstantList.emptyList()

        val prefab = Prefab("Mesh")
        prefab.setProperty("materials", materials.map { it as BMaterial?; it?.fileRef ?: InvalidRef })
        prefab.setProperty("cullMode", CullMode.BOTH)

        // todo bone hierarchy,
        // todo bone animations
        // todo bone indices & weights (vertex groups)
        // val layers = mesh.lData.layers ?: emptyArray()
        // val uvLayers = layers.firstOrNull { it as BCustomDataLayer; it.type == 16 } as? BCustomDataLayer
        // val weights = layers.firstOrNull { it as BCustomDataLayer; it.type == 17 } as? BCustomDataLayer
        @Suppress("SpellCheckingInspection")
                /*
                * var layers = data.getLdata().getLayers();
                var uvs = layers.filter(map => map.getType() == 16)[0];
                if(uvs) uvs = uvs.getData();
                var wei = layers.filter(map => map.getType() == 17)[0];
                if(wei) wei = wei.getData();
                * */
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
        prefab.setProperty("positions", positions)
        prefab.setProperty("normals", normals)

        val uvs = src.loopUVs ?: BInstantList.emptyList()
        // todo vertex colors
        val hasUVs = uvs.any { it.u != 0f || it.v != 0f }
        val triCount = polygons.sumOf {
            when (val size = it.loopSize) {
                0 -> 0
                1, 2 -> 1
                else -> size - 2
            }
        }
        val materialIndices = if (materials.size > 1) IntArray(triCount) else null
        if (hasUVs) {// non-indexed, because we don't support separate uv and position indices
            joinPositionsAndUVs(
                triCount * 3,
                positions, normals,
                polygons, loopData, uvs,
                materialIndices, prefab
            )
            if (materialIndices != null) prefab.setProperty("materialIds", materialIndices)
        } else {
            collectIndices(positions, polygons, loopData, materialIndices, prefab)
            if (materialIndices != null) prefab.setProperty("materialIds", materialIndices)
        }
        prefab.sealFromModifications()
        return prefab
    }

    private fun addTriangle(
        positions: FloatArray, positions2: ExpandingFloatArray,
        normals: FloatArray?, normals2: ExpandingFloatArray?,
        uvs2: ExpandingFloatArray,
        v0: Int, v1: Int, v2: Int,
        uvs: BInstantList<MLoopUV>,
        uv0: Int, uv1: Int, uv2: Int
    ) {
        val v03 = v0 * 3
        val v13 = v1 * 3
        val v23 = v2 * 3
        positions2.addUnsafe(positions, v03, 3)
        positions2.addUnsafe(positions, v13, 3)
        positions2.addUnsafe(positions, v23, 3)
        if (normals != null && normals2 != null) {
            normals2.addUnsafe(normals, v03, 3)
            normals2.addUnsafe(normals, v13, 3)
            normals2.addUnsafe(normals, v23, 3)
        }
        // println("$v0 $v1 $v2 $uv0 $uv1 $uv2 ${positions[v03]} ${normals[v03]}")
        val uv0x = uvs[uv0]
        uvs2.addUnsafe(uv0x.u)
        uvs2.addUnsafe(uv0x.v)
        val uv1x = uvs[uv1]
        uvs2.addUnsafe(uv1x.u)
        uvs2.addUnsafe(uv1x.v)
        val uv2x = uvs[uv2]
        uvs2.addUnsafe(uv2x.u)
        uvs2.addUnsafe(uv2x.v)
    }

    fun joinPositionsAndUVs(
        vertexCount: Int,
        positions: FloatArray,
        normals: FloatArray?,
        polygons: BInstantList<MPoly>,
        loopData: BInstantList<MLoop>,
        uvs: BInstantList<MLoopUV>,
        materialIndices: IntArray?,
        prefab: Prefab
    ) {
        val positions2 = ExpandingFloatArray(vertexCount * 3)
        val normals2 = if (normals != null) ExpandingFloatArray(vertexCount * 3) else null
        val uvs2 = ExpandingFloatArray(vertexCount * 2)
        var uvIndex = 0
        var matIndex = 0
        //var complexCtr = 0
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
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2, v, v, v,
                        uvs, uv, uv, uv
                    )
                    materialIndices?.set(matIndex++, materialIndex)
                }
                2 -> {// line
                    val v0 = loopData[loopStart].v
                    val v1 = loopData[loopStart + 1].v
                    val uv0 = uvIndex++
                    val uv1 = uvIndex++
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2,
                        v0, v1, v1,
                        uvs,
                        uv0, uv1, uv1
                    )
                    materialIndices?.set(matIndex++, materialIndex)
                }
                3 -> {// triangle
                    val v0 = loopData[loopStart].v
                    val v1 = loopData[loopStart + 1].v
                    val v2 = loopData[loopStart + 2].v
                    val uv0 = uvIndex++
                    val uv1 = uvIndex++
                    val uv2 = uvIndex++
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2, v0, v1, v2,
                        uvs, uv0, uv1, uv2
                    )
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
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2, v0, v1, v2,
                        uvs, uv0, uv1, uv2
                    )
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2, v2, v3, v0,
                        uvs, uv2, uv3, uv0
                    )
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
                        addTriangle(
                            positions, positions2,
                            normals, normals2,
                            uvs2,
                            v0, v1, v2,
                            uvs,
                            uv0, uv1, uv2
                        )
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
        prefab["normals"] = normals2?.toFloatArray()
        prefab["uvs"] = uvs2.toFloatArray()
    }

    fun collectIndices(
        positions: FloatArray,
        polygons: BInstantList<MPoly>,
        loopData: BInstantList<MLoop>,
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
        prefab["indices"] = indices.toIntArray()
    }
}