package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.fonts.mesh.Triangulation
import me.anno.gpu.CullMode
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerFolderCallback
import me.anno.maths.Maths.sq
import me.anno.mesh.blender.impl.*
import me.anno.utils.Clock
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.max

/**
 * extract the relevant information from a blender file:
 *  - done meshes
 *  - todo skeletons
 *  - todo animations
 *  - todo embedded textures
 *  - done materials
 *  - done scene hierarchy
 * create a test scene with different layouts, and check that everything is in the right place
 * */
object BlenderReader {

    // postTransform=false may not be setting positions / translations correctly
    private const val postTransform = true

    private val LOGGER = LogManager.getLogger(BlenderReader::class)

    // todo the materials have their specific order
    // todo the material index of a triangle is the real material
    // todo find all present materials, and compact them

    private fun mapMaterials(materials: List<BMaterial>, polygons: BInstantList<MPoly>)
            : Pair<IntArray, Array<FileReference>> {
        val usedMaterials = BooleanArray(materials.size)
        for (i in 0 until polygons.size) {
            usedMaterials[polygons[i].materialIndex.toInt() and 0xffff] = true
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

    fun readAsFolder(ref: FileReference, callback: InnerFolderCallback) {
        ref.readByteBuffer(false) { it, exc ->
            if (it != null) {
                callback(readAsFolder(ref, it), null)
            } else callback(null, exc)
        }
    }

    private fun readImages(file: BlenderFile, folder: InnerFolder, clock: Clock) {
        @Suppress("unchecked_cast")
        val imagesInFile = file.instances["Image"] as? List<BImage> ?: return
        val texFolder = folder.createChild("textures", null) as InnerFolder
        for (image in imagesInFile) {
            LOGGER.debug(image)
            val rawImageData = image.packedFiles.first?.packedFile?.data
            if (rawImageData != null) {
                val newName = "${image.id.realName}.png"
                image.reference = texFolder.createByteChild(newName, rawImageData)
            } else if (image.name.isNotBlank()) {
                // prefer external files, if they exist?
                image.reference = file.folder.getChild(image.name)
            }
        }
        clock.stop("reading images")
    }

    private fun readMaterials(file: BlenderFile, folder: InnerFolder, clock: Clock) {
        @Suppress("unchecked_cast")
        val materialsInFile = file.instances["Material"] as? List<BMaterial> ?: return
        val matFolder = folder.createChild("materials", null) as InnerFolder
        for (i in materialsInFile.indices) {
            val mat = materialsInFile[i]
            val prefab = Prefab("Material")
            BlenderShaderTree.defineDefaultMaterial(prefab, mat)
            val name = mat.id.realName
            prefab.sealFromModifications()
            mat.fileRef = matFolder.createPrefabChild("$name.json", prefab)
        }
        clock.stop("read ${file.instances["Material"]?.size} materials")
    }

    private fun readMeshes(file: BlenderFile, folder: InnerFolder, clock: Clock) {
        if ("Mesh" !in file.instances) return
        val meshFolder = folder.createChild("meshes", null) as InnerFolder
        for (mesh in file.instances["Mesh"]!!) {
            mesh as BMesh
            var name = mesh.id.name
            if (name.startsWith("ME") && name.length > 2) name = name.substring(2)
            val prefab = Prefab("Mesh")
            val vertices = mesh.vertices ?: continue // how can there be meshes without vertices?
            val positions = FloatArray(vertices.size * 3)
            val materials = mesh.materials ?: emptyArray()
            val polygons = mesh.polygons ?: BInstantList.emptyList()
            val loopData = mesh.loops ?: BInstantList.emptyList()

            // todo if there are multiple materials, collect the indices

            // LOGGER.debug("mesh materials: $materials")
            // LOGGER.debug("mesh $name: ${mesh.numVertices} vertices, ${mesh.numPolygons} polys with ${mesh.polygons?.sumOf { it.loopSize }} vertices")
            // val mapping = mapMaterials(materialsInFile, polygons)
            // LOGGER.debug("mat-mapping: [${mapping.first.joinToString()}], [${mapping.second.joinToString()}]")
            // prefab.setProperty("materials", mapping.second)
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
            val normals: FloatArray? = if (postTransform) {
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
                    prefab["normals"] = normals
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
                    prefab.setProperty("normals", normals)
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
            // LOGGER.debug("loop uvs: " + mesh.loopUVs?.size)
            val uvs = mesh.loopUVs ?: BInstantList.emptyList()
            // todo vertex colors
            val hasUVs = uvs.any { it.u != 0f || it.v != 0f }
            // LOGGER.debug("loop cols: " + mesh.loopColor?.size)
            val triCount = polygons.sumOf {
                when (val size = it.loopSize) {
                    0 -> 0
                    1, 2 -> 1
                    else -> size - 2
                }
            }
            if (hasUVs) {// non-indexed, because we don't support separate uv and position indices
                val materialIndices = if (materials.size > 1) IntArray(triCount) else null
                joinPositionsAndUVs(
                    triCount * 3,
                    positions, normals,
                    polygons, loopData, uvs,
                    materialIndices, prefab
                )
                if (materialIndices != null) prefab.setProperty("materialIds", materialIndices)
            } else {
                val materialIndices = if (materials.size > 1) IntArray(triCount) else null
                collectIndices(positions, polygons, loopData, materialIndices, prefab)
                if (materialIndices != null) prefab.setProperty("materialIds", materialIndices)
            }
            prefab.sealFromModifications()
            mesh.fileRef = meshFolder.createPrefabChild("$name.json", prefab)
        }
        clock.stop("read meshes")
    }

    private fun extractHierarchy(file: BlenderFile): Prefab {
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
                for (index in roots.indices) {
                    val bObject = roots[index]
                    val name = bObject.id.realName
                    val path = Path(Path.ROOT_PATH, name, index, 'e')
                    paths[bObject] = path
                    createObject(prefab, bObject, path, false)
                }
                if (postTransform) {
                    prefab[Path.ROOT_PATH, "rotation"] = Quaterniond().rotateX(-PI / 2)
                }
            } else {
                // there must be a root
                paths[roots.first()] = Path.ROOT_PATH
                createObject(prefab, roots.first(), Path.ROOT_PATH, true)
            }

            for (obj in objects) {
                obj as BObject
                makeObject(prefab, obj, paths)
            }
        }
        return prefab
    }

    fun readAsFolder(ref: FileReference, nio: ByteBuffer): InnerFolder {

        // todo 1: find equivalent meshes, and replace them for speed
        // todo 2: read positions, indices, and normals without instantiation

        // transform: +x, +z, -y
        // because we want y up, and Blender has z up

        val clock = Clock()
        clock.stop("read bytes")

        clock.stop("put into other array")
        val binaryFile = BinaryFile(nio)
        val folder = InnerFolder(ref)
        val file = BlenderFile(binaryFile, ref.getParent() ?: InvalidRef)
        clock.stop("read blender file")
        // data.printTypes()

        readImages(file, folder, clock)
        readMaterials(file, folder, clock)
        readMeshes(file, folder, clock)

        val prefab = extractHierarchy(file)
        prefab.sealFromModifications()
        folder.createPrefabChild("Scene.json", prefab)
        clock.stop("read hierarchy")
        return folder
    }

    fun makeObject(prefab: Prefab, obj: BObject, paths: HashMap<BObject, Path>): Path {
        return paths.getOrPut(obj) {
            val name = obj.id.realName
            val parent = makeObject(prefab, obj.parent!!, paths)
            val childIndex = prefab.adds.count { it.path == parent && it.type == 'e' }
            val path = Path(parent, name, childIndex, 'e')
            createObject(prefab, obj, path, false)
            path
        }
    }

    fun createObject(prefab: Prefab, obj: BObject, path: Path, isRoot: Boolean) {
        if (path != Path.ROOT_PATH) {
            prefab.add(
                path.parent ?: Path.ROOT_PATH,
                path.type, "Entity", path.nameId
            )
        }
        // add position relative to parent
        // par * self = ws
        // -> (par)-1 * (par * self) = self
        val parentMatrix = obj.parent?.finalWSMatrix ?: Matrix4f()
        val localMatrix = Matrix4f(parentMatrix).invert().mul(obj.finalWSMatrix)
        // if(path == Path.ROOT_PATH) localMatrix.rotateX(-PI.toFloat() * 0.5f)
        val translation = localMatrix.getTranslation(Vector3d())
        if (!postTransform) translation.set(translation.x, translation.z, -translation.y)
        if (translation.x != 0.0 || translation.y != 0.0 || translation.z != 0.0)
            prefab.setUnsafe(path, "position", translation)
        val rotation = localMatrix.getUnnormalizedRotation(Quaterniond())
        if (!postTransform) rotation.set(rotation.x, rotation.z, -rotation.y, rotation.w)
        if (isRoot && postTransform) rotation.rotateLocalX(-PI / 2)
        if (rotation.w != 1.0)
            prefab.setUnsafe(path, "rotation", rotation)
        val scale = localMatrix.getScale(Vector3d())
        if (!postTransform) scale.set(scale.x, scale.z, -scale.y)
        if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0)
            prefab.setUnsafe(path, "scale", scale)
        when (BObject.objectTypeById[obj.type.toInt()]) {
            BObject.BObjectType.OB_EMPTY -> { // done
            }
            BObject.BObjectType.OB_MESH -> {
                // add mesh component
                val c = prefab.add(path, 'c', "MeshComponent", obj.id.realName)
                prefab.setUnsafe(c, "meshFile", (obj.data as BMesh).fileRef)
                // materials would be nice... but somehow they are always null
            }
            BObject.BObjectType.OB_CAMERA -> {
                val cam = obj.data as? BCamera
                if (cam != null) {
                    val c = prefab.add(path, 'c', "Camera", obj.id.realName)
                    prefab.setUnsafe(c, "near", cam.near.toDouble())
                    prefab.setUnsafe(c, "far", cam.far.toDouble())
                }
            }
            BObject.BObjectType.OB_LAMP -> {
                val light = obj.data as? BLamp
                if (light != null) {
                    val name = obj.id.realName
                    val path1 = prefab.add(path, 'e', "Entity", name)
                    val extraSize = Vector3d(1.0)
                    val c = when (light.type) {
                        0 -> {
                            extraSize.set(light.pointRadius.toDouble())
                            prefab.add(path1, 'c', "PointLight", name)
                        }
                        1 -> { // sun
                            prefab.add(path1, 'c', "DirectionalLight", name)
                        }
                        2 -> {
                            extraSize.set(light.spotRadius.toDouble())
                            prefab.add(path1, 'c', "SpotLight", name)
                        }
                        // AreaLight
                        4 -> {
                            when (light.areaShape.toInt()) {
                                0 -> {
                                    // square
                                    extraSize.set(light.areaSizeX * 10.0)
                                    prefab.add(path1, 'c', "RectangleLight", name).apply {
                                        prefab.setUnsafe(this, "width", 0.1f)
                                        prefab.setUnsafe(this, "height", 0.1f)
                                    }
                                }
                                1 -> {
                                    // rectangle
                                    val w = light.areaSizeX
                                    val h = light.areaSizeY
                                    extraSize.set(max(w, h) * 10.0)
                                    prefab.add(path1, 'c', "RectangleLight", name).apply {
                                        prefab.setUnsafe(this, "width", 0.1f * w / max(w, h))
                                        prefab.setUnsafe(this, "height", 0.1f * h / max(w, h))
                                    }
                                }
                                4 -> {
                                    // circle
                                    extraSize.set(light.areaSizeX * 10.0)
                                    prefab.add(path1, 'c', "CircleLight", name).apply {
                                        prefab.setUnsafe(this, "radius", 0.1f)
                                    }
                                }
                                5 -> {
                                    // ellipse
                                    val w = light.areaSizeX * 10.0
                                    val h = light.areaSizeY * 10.0
                                    extraSize.set(w, max(w, h), h)
                                    prefab.add(path1, 'c', "CircleLight", name).apply {
                                        prefab.setUnsafe(this, "radius", 0.1f)
                                    }
                                }
                                else -> null
                            }
                        }
                        else -> null // deprecated or not supported
                    }
                    prefab[path1, "scale"] = extraSize
                    if (c != null) {
                        // scale energy by 1/scale², because we follow the 1/distance² law in light-local space
                        val e = light.energy * 0.01f / sq(max(extraSize.x, extraSize.z).toFloat())
                        prefab.setUnsafe(c, "color", Vector3f(light.r, light.g, light.b).mul(e))
                        prefab.setUnsafe(c, "shadowMapCascades", light.cascadeCount)
                        prefab.setUnsafe(c, "shadowMapPower", light.cascadeExponent.toDouble())
                        prefab.setUnsafe(c, "autoUpdate", false)
                    }
                } else LOGGER.warn("obj.data of a lamp was not a lamp: ${obj.data?.run { this::class.simpleName }}")
            }
            // todo armatures...
            // todo volumes?
            // todo curves?
            else -> {
                // nothing to do
            }
        }
    }
}