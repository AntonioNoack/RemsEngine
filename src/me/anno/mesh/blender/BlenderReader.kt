package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.fonts.mesh.Triangulation
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerPrefabFile
import me.anno.mesh.blender.impl.*
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Matrices.getScale2
import me.anno.utils.types.Matrices.getTranslation2
import org.apache.logging.log4j.LogManager
import org.joml.*
import java.nio.ByteBuffer


// extract the relevant information from a blender file:
// done meshes
// todo skeletons
// todo animations
// done materials
// todo scene hierarchy
//     create a test scene with different layouts, and check that everything is in the right place
object BlenderReader {

    private val LOGGER = LogManager.getLogger(BlenderReader::class)

    // todo the materials have their specific order
    // todo the material index of a triangle is the real material
    // todo find all present materials, and compact them

    private fun mapMaterials(materials: List<BMaterial>, polygons: BInstantList<MPoly>)
            : Pair<IntArray, Array<FileReference>> {
        val usedMaterials = BooleanArray(materials.size)
        for (i in 0 until polygons.size) {
            usedMaterials[polygons[i].materialIndex.toUShort().toInt()] = true
        }
        val numUsedMaterials = usedMaterials.count { it }
        val indexMapping = IntArray(materials.size) { -1 }
        val materialList = Array<FileReference>(numUsedMaterials) { InvalidRef }
        var j = 0
        val awkwardOffset = 1 // maybe 0 = undefined?
        for (i in awkwardOffset until materials.size) {
            if (usedMaterials[i - awkwardOffset]) {
                indexMapping[i] = j
                materialList[j] = materials[i].fileRef
                j++
            }
        }
        return indexMapping to materialList
    }

    fun addTriangle(
        positions: FloatArray, positions2: ExpandingFloatArray,
        normals: FloatArray, normals2: ExpandingFloatArray,
        uvs2: ExpandingFloatArray,
        v0: Int, v1: Int, v2: Int,
        uvs: BInstantList<MLoopUV>,
        uv0: Int, uv1: Int, uv2: Int
    ) {
        positions2.addUnsafe(positions, v0 * 3, 3)
        positions2.addUnsafe(positions, v1 * 3, 3)
        positions2.addUnsafe(positions, v2 * 3, 3)
        normals2.addUnsafe(normals, v0 * 3, 3)
        normals2.addUnsafe(normals, v1 * 3, 3)
        normals2.addUnsafe(normals, v2 * 3, 3)
        val uv0x = uvs[uv0]
        uvs2.addUnsafe(uv0x.u); uvs2.addUnsafe(uv0x.v)
        val uv1x = uvs[uv1]
        uvs2.addUnsafe(uv1x.u); uvs2.addUnsafe(uv1x.v)
        val uv2x = uvs[uv2]
        uvs2.addUnsafe(uv2x.u); uvs2.addUnsafe(uv2x.v)
    }

    fun joinPositionsAndUVs(
        vertexCount: Int,
        positions: FloatArray,
        normals: FloatArray,
        polygons: BInstantList<MPoly>,
        loopData: BInstantList<MLoop>,
        uvs: BInstantList<MLoopUV>,
        materialIndices: IntArray?,
        prefab: Prefab
    ) {
        val positions2 = ExpandingFloatArray(vertexCount * 3)
        val normals2 = ExpandingFloatArray(vertexCount * 3)
        val uvs2 = ExpandingFloatArray(vertexCount * 2)
        positions2.ensure()
        normals2.ensure()
        uvs2.ensure()
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
                        uvs2,
                        v, v, v,
                        uvs,
                        uv, uv, uv
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
                        uvs2,
                        v0, v1, v2,
                        uvs,
                        uv0, uv1, uv2
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
                        uvs2,
                        v0, v1, v2,
                        uvs,
                        uv0, uv1, uv2
                    )
                    addTriangle(
                        positions, positions2,
                        normals, normals2,
                        uvs2,
                        v2, v3, v0,
                        uvs,
                        uv2, uv3, uv0
                    )
                    materialIndices?.set(matIndex++, materialIndex)
                    materialIndices?.set(matIndex++, materialIndex)
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
                        materialIndices?.set(matIndex++, materialIndex)
                    }
                }
            }
        }
        prefab.setProperty("positions", positions2.toFloatArray())
        prefab.setProperty("normals", normals2.toFloatArray())
        prefab.setProperty("uvs", uvs2.toFloatArray())
        /*if (complexCtr > 0) {
            LOGGER.info("Mesh had $complexCtr complex polygons")
        }*/
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
                    for (j in triangles.indices step 3) {
                        materialIndices?.set(matIndex++, materialIndex)
                    }
                }
            }
        }
        prefab.setProperty("indices", indices.toIntArray())
    }

    fun readAsFolder(ref: FileReference): InnerFolder {

        // todo 1: the normals are often awkward
        // todo 2: find equivalent meshes, and replace them for speed
        // todo 3: read positions, indices, and normals without instantiation

        // transform: +x, +z, -y
        // because we want y up, and Blender has z up

        val clock = Clock()
        val bytes = ref.readBytes()
        clock.stop("read bytes")
        val nio = ByteBuffer.allocate(bytes.size)
        nio.put(bytes)
        nio.flip()
        clock.stop("put into other array")
        val binaryFile = BinaryFile(nio)
        val folder = InnerFolder(ref)
        val file = BlenderFile(binaryFile)
        clock.stop("read blender file")
        // data.printTypes()

        @Suppress("UNCHECKED_CAST")
        val materialsInFile = file.instances["Material"] as? List<BMaterial> ?: emptyList()

        // find where the materials are referenced
        /*file.searchReferencesByStructsAtPositions(
            materialsInFile.map { it.position },
            materialsInFile.map { it.id.name.substring(2) }
        )

        val meshes = file.instances["Mesh"] as? List<BMesh> ?: emptyList()
        meshes.forEach { mesh ->
            println(mesh.id.name)
            println(mesh.materials?.joinToString())
        }*/

        // return folder


        if ("Material" in file.instances) {
            val matFolder = folder.createChild("materials", null) as InnerFolder
            for (i in materialsInFile.indices) {
                val mat = materialsInFile[i]
                val name = mat.id.name.substring(2)
                // println("material $name")
                val prefab = Prefab("Material")
                prefab.setProperty("diffuseBase", Vector4f(mat.r, mat.g, mat.b, mat.a))
                prefab.setProperty("metallicMinMax", Vector2f(0f, mat.metallic))
                prefab.setProperty("roughnessMinMax", Vector2f(0f, mat.roughness))
                prefab.sealFromModifications()
                mat.fileRef = matFolder.createPrefabChild("$name.json", prefab)
                // println("prev ${mat.id.prev}, next: ${mat.id.next}")
            }
        }
        clock.stop("read ${file.instances["Material"]?.size} materials")
        if ("Mesh" in file.instances) {
            val meshFolder = folder.createChild("meshes", null) as InnerFolder
            for (mesh in file.instances["Mesh"]!!) {
                mesh as BMesh
                var name = mesh.id.name
                if (name.startsWith("ME") && name.length > 2) name = name.substring(2)
                val prefab = Prefab("Mesh")
                val vertices = mesh.vertices ?: continue // how can there be meshes without vertices?
                val positions = FloatArray(vertices.size * 3)
                val normals = FloatArray(vertices.size * 3)
                val materials = mesh.materials ?: emptyArray()
                val polygons = mesh.polygons ?: BInstantList.emptyList()
                val loopData = mesh.loops ?: BInstantList.emptyList()

                // todo if there are multiple materials, collect the indices

                // println("mesh materials: $materials")
                // println("mesh $name: ${mesh.numVertices} vertices, ${mesh.numPolygons} polys with ${mesh.polygons?.sumOf { it.loopSize }} vertices")
                // val mapping = mapMaterials(materialsInFile, polygons)
                // println("mat-mapping: [${mapping.first.joinToString()}], [${mapping.second.joinToString()}]")
                // prefab.setProperty("materials", mapping.second)
                prefab.setProperty("materials", materials.map { it as BMaterial?; it?.fileRef ?: InvalidRef })
                // todo bone hierarchy,
                // todo bone animations
                // todo bone indices & weights (vertex groups)
                // val layers = mesh.lData.layers ?: emptyArray()
                // val uvLayers = layers.firstOrNull { it as BCustomDataLayer; it.type == 16 } as? BCustomDataLayer
                // val weights = layers.firstOrNull { it as BCustomDataLayer; it.type == 17 } as? BCustomDataLayer
                /*
                * var layers = data.getLdata().getLayers();
                var uvs = layers.filter(map => map.getType() == 16)[0];
                if(uvs) uvs = uvs.getData();
                var wei = layers.filter(map => map.getType() == 17)[0];
                if(wei) wei = wei.getData();
                * */
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
                prefab.setProperty("positions", positions)
                prefab.setProperty("normals", normals)
                // println("loop uvs: " + mesh.loopUVs?.size)
                val uvs = mesh.loopUVs ?: BInstantList.emptyList()
                // todo vertex colors
                val hasUVs = uvs.isNotEmpty() && uvs.any { it.u != 0f || it.v != 0f }
                // println("loop cols: " + mesh.loopColor?.size)
                if (hasUVs) {// non-indexed, because we don't support separate uv and position indices
                    val triCount = polygons.sumOf {
                        when (val size = it.loopSize) {
                            0 -> 0
                            1, 2, 3 -> 1
                            else -> size - 2
                        }
                    }
                    val materialIndices = if (materials.size > 1) IntArray(triCount) else null
                    joinPositionsAndUVs(
                        triCount * 3,
                        positions,
                        normals,
                        polygons,
                        loopData,
                        uvs,
                        materialIndices,
                        prefab
                    )
                    if (materialIndices != null) prefab.setProperty("materialIndices", materialIndices)
                } else {
                    val materialIndices = if (materials.size > 1) IntArray(positions.size / 9) else null
                    collectIndices(positions, polygons, loopData, materialIndices, prefab)
                    if (materialIndices != null) prefab.setProperty("materialIndices", materialIndices)
                }
                prefab.sealFromModifications()
                mesh.fileRef = meshFolder.createPrefabChild("$name.json", prefab)
            }
        }
        clock.stop("read meshes")

        // extract the hierarchy, and create a Scene.json somehow
        val prefab = Prefab("Entity")
        if ("Object" in file.instances) {

            val objects = file.instances["Object"]!!
            val roots = ArrayList<BObject>()

            for (obj in objects) {
                obj as BObject
                val parent = obj.parent
                if (parent == null) roots.add(obj)
            }

            val paths = HashMap<BObject, Path>()
            if (roots.size > 1) {
                // add a pseudo root
                roots.forEachIndexed { index, bObject ->
                    val name = bObject.id.name.substring(2)
                    val path = Path(Path.ROOT_PATH, name, index, 'e')
                    paths[bObject] = path
                    createObject(prefab, bObject, path)
                }
            } else {
                // there must be a root
                paths[roots.first()] = Path.ROOT_PATH
                createObject(prefab, roots.first(), Path.ROOT_PATH)
            }

            for (obj in objects) {
                obj as BObject
                makeObject(prefab, obj, paths)
            }

        }
        prefab.sealFromModifications()
        folder.createPrefabChild("Scene.json", prefab)
        clock.stop("read hierarchy")
        return folder
    }

    fun makeObject(prefab: Prefab, obj: BObject, paths: HashMap<BObject, Path>): Path {
        return paths.getOrPut(obj) {
            val name = obj.id.name.substring(2)
            val parent = makeObject(prefab, obj.parent!!, paths)
            val childIndex = prefab.adds.count { it.path == parent && it.type == 'e' }
            val path = Path(parent, name, childIndex, 'e')
            createObject(prefab, obj, path)
            path
        }
    }

    fun createObject(prefab: Prefab, obj: BObject, path: Path) {
        if (path != Path.ROOT_PATH) {
            prefab.add(path.parent ?: Path.ROOT_PATH, 'e', "Entity")
        }
        // add position relative to parent
        // par * self = ws
        // -> (par)-1 * (par * self) = self
        val parentMatrix = obj.parent?.finalWSMatrix ?: Matrix4f()
        val localMatrix = Matrix4f(parentMatrix).invert().mul(obj.finalWSMatrix)
        // if(path == Path.ROOT_PATH) localMatrix.rotateX(-Math.PI.toFloat() * 0.5f)
        val translation = localMatrix.getTranslation2().switchYZ()
        if (translation.x != 0.0 || translation.y != 0.0 || translation.z != 0.0)
            prefab.setUnsafe(path, "position", translation)
        val rotation = localMatrix.getUnnormalizedRotation(Quaterniond()).switchYZ()
        if (rotation.w != 1.0)
            prefab.setUnsafe(path, "rotation", rotation)
        val scale = localMatrix.getScale2().switchYZ()
        if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0)
            prefab.setUnsafe(path, "scale", scale)
        // todo get names...
        when (BObject.objectTypeById[obj.type.toInt()]) {
            BObject.BObjectType.OB_EMPTY -> { // done
            }
            BObject.BObjectType.OB_MESH -> {
                // add mesh component
                val c = prefab.add(path, 'c', "MeshComponent", 0)
                prefab.setUnsafe(c, "mesh", (obj.data as BMesh).fileRef)
                // materials would be nice... but somehow they are always null
            }
            BObject.BObjectType.OB_CAMERA -> {
                val cam = obj.data as? BCamera
                if (cam != null) {
                    val c = prefab.add(path, 'c', "CameraComponent", 0)
                    prefab.setUnsafe(c, "near", cam.near.toDouble())
                    prefab.setUnsafe(c, "far", cam.far.toDouble())
                }
            }
            BObject.BObjectType.OB_LAMP -> {
                val light = obj.data as? BLamp
                if (light != null) {
                    val clazzName = when (light.type) {
                        0 -> "PointLight"
                        1 -> "DirectionalLight" // sun
                        2 -> "SpotLight"
                        4 -> "SpotLight" // todo area light
                        else -> null // deprecated or not supported
                    }
                    if (clazzName != null) {
                        // additional scale by brightness? probably would be a good idea
                        val c = prefab.add(path, 'c', clazzName, 0)
                        val e = light.energy * 0.01f // 100 W is ~ our brightness
                        prefab.setUnsafe(c, "color", Vector3f(light.r, light.g, light.b).mul(e))
                        prefab.setUnsafe(c, "shadowMapCascades", light.cascadeCount)
                        prefab.setUnsafe(c, "shadowMapPower", light.cascadeExponent)
                    }
                } else LOGGER.warn("obj.data of a lamp was not a lamp: ${obj.data?.javaClass}")
            }
            // todo armatures...
            // todo volumes?
            // todo curves?
            else -> {
                // nothing to do
            }
        }
    }

    fun Vector3d.switchYZ(): Vector3d {
        return set(x, +z, -y)
    }

    fun Quaterniond.switchYZ(): Quaterniond {
        return set(x, +z, -y, w)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // Thread.sleep(10000) // time for the debugger to attach
        val clock = Clock()
        // val ref = getReference(documents, "Blender/Bedroom.blend")
        val ref = getReference(documents, "Blender/MaterialTest.blend")
        // val ref = getReference("E:/Documents/Blender/Aerial Aircraft Carrier (CVNA-82)II.blend")
        val folder = readAsFolder(ref)
        clock.stop("read file")
        ECSRegistry.initWithGFX(512)
        clock.stop("inited opengl")
        val scene = folder.getChild("Scene.json") as InnerPrefabFile
        Thumbs.useCacheFolder = true
        Thumbs.generateSomething(scene.prefab, ref, getReference(desktop, "test.png"), 512) {}
        clock.stop("rendered & saved image")
    }

}