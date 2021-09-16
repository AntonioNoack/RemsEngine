package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.fonts.mesh.Triangulation
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.zip.InnerFolder
import me.anno.mesh.blender.impl.*
import me.anno.utils.OS
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer

// todo extract the relevant information from a blender file:
// todo meshes
// todo skeletons
// todo animations
// todo materials
// todo scene hierarchy
object BlenderReader {

    fun addTriangle(
        positions: FloatArray, positions2: ExpandingFloatArray,
        normals: FloatArray, normals2: ExpandingFloatArray,
        uvs2: ExpandingFloatArray,
        v0: Int, v1: Int, v2: Int,
        uv0: MLoopUV, uv1: MLoopUV, uv2: MLoopUV
    ) {
        positions2.add(positions, v0 * 3, 3)
        positions2.add(positions, v1 * 3, 3)
        positions2.add(positions, v2 * 3, 3)
        normals2.add(normals, v0 * 3, 3)
        normals2.add(normals, v1 * 3, 3)
        normals2.add(normals, v2 * 3, 3)
        uvs2.add(uv0.u); uvs2.add(uv0.v)
        uvs2.add(uv1.u); uvs2.add(uv1.v)
        uvs2.add(uv2.u); uvs2.add(uv2.v)
    }

    fun joinPositionsAndUVs(
        positions: FloatArray,
        normals: FloatArray,
        polygons: Array<BlendData?>,
        loopData: Array<BlendData?>,
        uvs: Array<BlendData?>,
        prefab: Prefab
    ) {
        val positions2 = ExpandingFloatArray(positions.size)
        val normals2 = ExpandingFloatArray(normals.size)
        val uvs2 = ExpandingFloatArray(positions.size * 2 / 3)
        var uvIndex = 0
        for (i in polygons.indices) {
            val polygon = polygons[i] as MPoly
            val loopStart = polygon.loopStart
            when (val loopSize = polygon.loopSize) {
                0 -> {
                }
                1 -> {// point
                    val v = (loopData[loopStart] as MLoop).v
                    val uv = uvs[uvIndex++] as MLoopUV
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2,
                        v, v, v,
                        uv, uv, uv
                    )
                }
                2 -> {// line
                    val v0 = (loopData[loopStart] as MLoop).v
                    val v1 = (loopData[loopStart + 1] as MLoop).v
                    val uv0 = uvs[uvIndex++] as MLoopUV
                    val uv1 = uvs[uvIndex++] as MLoopUV
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2,
                        v0, v1, v1,
                        uv0, uv1, uv1
                    )
                }
                3 -> {// triangle
                    val v0 = (loopData[loopStart] as MLoop).v
                    val v1 = (loopData[loopStart + 1] as MLoop).v
                    val v2 = (loopData[loopStart + 2] as MLoop).v
                    val uv0 = uvs[uvIndex++] as MLoopUV
                    val uv1 = uvs[uvIndex++] as MLoopUV
                    val uv2 = uvs[uvIndex++] as MLoopUV
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2,
                        v0, v1, v2,
                        uv0, uv1, uv2
                    )
                }
                4 -> {// quad, simple
                    val v0 = (loopData[loopStart] as MLoop).v
                    val v1 = (loopData[loopStart + 1] as MLoop).v
                    val v2 = (loopData[loopStart + 2] as MLoop).v
                    val v3 = (loopData[loopStart + 3] as MLoop).v
                    val uv0 = uvs[uvIndex++] as MLoopUV
                    val uv1 = uvs[uvIndex++] as MLoopUV
                    val uv2 = uvs[uvIndex++] as MLoopUV
                    val uv3 = uvs[uvIndex++] as MLoopUV
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2,
                        v0, v1, v2,
                        uv0, uv1, uv2
                    )
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2,
                        v2, v3, v0,
                        uv2, uv3, uv0
                    )
                }
                else -> {
                    // complex triangulation, because it may be more complicated than it seems, and
                    // we have to be correct
                    val vec2Index = HashMap<Vector3f, Int>()
                    val vectors = Array(loopSize) {
                        val index = (loopData[loopStart + it] as MLoop).v
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
                        val v0 = (loopData[loopStart + i0] as MLoop).v
                        val v1 = (loopData[loopStart + i1] as MLoop).v
                        val v2 = (loopData[loopStart + i2] as MLoop).v
                        val uv0 = uvs[uvIndex0 + i0] as MLoopUV
                        val uv1 = uvs[uvIndex0 + i1] as MLoopUV
                        val uv2 = uvs[uvIndex0 + i2] as MLoopUV
                        addTriangle(
                            positions, positions2,
                            normals, normals2,
                            uvs2,
                            v0, v1, v2,
                            uv0, uv1, uv2
                        )
                    }
                }
            }
        }
        prefab.setProperty("positions", positions2.toFloatArray())
        prefab.setProperty("normals", normals2.toFloatArray())
        prefab.setProperty("uvs", uvs2.toFloatArray())
    }

    fun collectIndices(
        positions: FloatArray,
        polygons: Array<BlendData?>,
        loopData: Array<BlendData?>,
        prefab: Prefab
    ) {
        // indexed, simple
        val indices = ExpandingIntArray(polygons.size * 3)
        for (i in polygons.indices) {
            // todo the uvs may be incompatible, and lay in a different list ->
            // todo when this is so, we cannot use the indexed buffer, or would need to cache values
            val polygon = polygons[i] as MPoly
            val loopStart = polygon.loopStart
            when (val loopSize = polygon.loopSize) {
                0 -> {
                }
                1 -> {// point
                    val v = (loopData[loopStart] as MLoop).v
                    indices.add(v)
                    indices.add(v)
                    indices.add(v)
                }
                2 -> {// line
                    val v0 = (loopData[loopStart] as MLoop).v
                    val v1 = (loopData[loopStart + 1] as MLoop).v
                    indices.add(v0)
                    indices.add(v1)
                    indices.add(v1)
                }
                3 -> {// triangle
                    indices.add((loopData[loopStart] as MLoop).v)
                    indices.add((loopData[loopStart + 1] as MLoop).v)
                    indices.add((loopData[loopStart + 2] as MLoop).v)
                }
                4 -> {// quad, simple
                    val v0 = (loopData[loopStart] as MLoop).v
                    val v1 = (loopData[loopStart + 1] as MLoop).v
                    val v2 = (loopData[loopStart + 2] as MLoop).v
                    val v3 = (loopData[loopStart + 3] as MLoop).v
                    indices.add(v0)
                    indices.add(v1)
                    indices.add(v2)
                    indices.add(v2)
                    indices.add(v3)
                    indices.add(v0)
                }
                else -> {
                    // complex triangulation, because it may be more complicated than it seems, and
                    // we have to be correct
                    val vec2Index = HashMap<Vector3f, Int>()
                    val vectors = Array(loopSize) {
                        val index = (loopData[loopStart + it] as MLoop).v
                        val vec = Vector3f(
                            positions[index * 3],
                            positions[index * 3 + 1],
                            positions[index * 3 + 2]
                        )
                        vec2Index[vec] = index
                        vec
                    }
                    val triangles = Triangulation.ringToTrianglesVec3f(vectors.toList())
                    for (tri in triangles) {
                        indices.add(vec2Index[tri]!!)
                    }
                }
            }
        }
        prefab.setProperty("indices", indices.toIntArray())
    }

    fun readAsFolder(ref: FileReference): InnerFolder {
        val bytes = ref.readBytes()
        val nio = ByteBuffer.allocate(bytes.size)
        nio.put(bytes)
        nio.flip()
        val file = BinaryFile(nio)
        val folder = InnerFolder(ref)
        val data = BlenderFile(file)
        // data.printTypes()
        if ("Material" in data.instances) {
            val matFolder = folder.createChild("materials", null) as InnerFolder
            for (mat in data.instances["Material"]!!) {
                mat as BMaterial
                var name = mat.id!!.name
                if (name.startsWith("MA") && name.length > 2) name = name.substring(2)
                println("material $name")
                val prefab = Prefab("Material")
                prefab.setProperty("diffuseBase", Vector4f(mat.r, mat.g, mat.b, mat.a))
                prefab.setProperty("metallicMinMax", Vector2f(0f, mat.metallic))
                prefab.setProperty("roughnessMinMax", Vector2f(0f, mat.roughness))
                matFolder.createPrefabChild("$name.json", prefab)
            }
        }
        if ("Mesh" in data.instances) {
            val meshFolder = folder.createChild("meshes", null) as InnerFolder
            for (mesh in data.instances["Mesh"]!!) {
                mesh as BMesh
                var name = mesh.id!!.name
                if (name.startsWith("ME") && name.length > 2) name = name.substring(2)
                val prefab = Prefab("Mesh")
                val vertices = mesh.vertices!!
                val positions = FloatArray(vertices.size * 3)
                val normals = FloatArray(vertices.size * 3)
                val layers = mesh.lData.layers ?: emptyArray()
                println("mesh $name: ${mesh.numVertices} vertices, ${mesh.numPolygons} polys with ${mesh.polygons?.sumOf { it as MPoly; it.loopSize }} vertices")
                val uvLayers = layers.firstOrNull { it as BCustomDataLayer; it.typeInt == 16 } as? BCustomDataLayer
                val weights = layers.firstOrNull { it as BCustomDataLayer; it.typeInt == 17 } as? BCustomDataLayer
                /*
                * var layers = data.getLdata().getLayers();
                var uvs = layers.filter(map => map.getType() == 16)[0];
                if(uvs) uvs = uvs.getData();
                var wei = layers.filter(map => map.getType() == 17)[0];
                if(wei) wei = wei.getData();
                * */
                for (i in vertices.indices) {
                    val v = vertices[i] as MVert
                    val i3 = i * 3
                    val pos = v.pos
                    // we could transform z<->y here
                    positions[i3] = pos.x
                    positions[i3 + 1] = pos.y
                    positions[i3 + 2] = pos.z
                    val nor = v.normal
                    normals[i3] = nor.x
                    normals[i3 + 1] = nor.y
                    normals[i3 + 2] = nor.z
                }
                prefab.setProperty("positions", positions)
                prefab.setProperty("normals", normals)
                val polygons = mesh.polygons ?: emptyArray()
                val loopData = mesh.loops ?: emptyArray()
                println("loop uvs: " + mesh.loopUVs?.size)
                val uvs = mesh.loopUVs ?: emptyArray() // todo when this contains valuable data, then use it
                val hasUVs = uvs.isNotEmpty() && uvs.any { it as MLoopUV; it.u != 0f || it.v != 0f }
                println("loop cols: " + mesh.loopColor?.size)
                if (hasUVs) {// non-indexed, because we don't support separate uv and position indices
                    joinPositionsAndUVs(positions, normals, polygons, loopData, uvs, prefab)
                } else {
                    collectIndices(positions, polygons, loopData, prefab)
                }
                meshFolder.createPrefabChild("$name.json", prefab)
            }
        }
        // todo extract the hierarchy, and create a Scene.json somehow

        return folder
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val ref = getReference(OS.documents, "Blender/Beaker.blend")
        readAsFolder(ref)
    }

}