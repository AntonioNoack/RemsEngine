package me.anno.io.unity

import me.anno.cache.Promise
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.gpu.CullMode
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerLinkFile
import me.anno.io.files.inner.InnerPrefabFile
import me.anno.io.saveable.Saveable
import me.anno.io.unity.UnityProject.Companion.isValidUUID
import me.anno.io.yaml.generic.YAMLNode
import me.anno.io.yaml.generic.YAMLReader.beautify
import me.anno.utils.ColorParsing.parseHex
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import me.anno.utils.structures.maps.BiMap
import me.anno.utils.types.Ints.toLongOrDefault
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Quaterniond
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

// todo directional light in E:/Assets/Unity/POLYGON_Adventure_Unity_Package_2017_1.unitypackage/Assets/PolygonAdventure/DemoScene/Demo_Scene.unity
//  is looking the wrong direction

// todo transforms are broken, e.g.
//  E:/Assets/Unity/Polygon_Construction_Unity_Package_2017_4.unitypackage/Assets/PolygonConstruction/Prefabs/Vehicles/SM_Veh_Mini_Loader_01.prefab
//  worked in the past
object UnityReader {

    fun loadUnityFile(resource: FileReference, callback: Callback<Saveable>) {
        readAsAsset(resource) { json, e ->
            if (json != null) PrefabCache.loadJson(json).waitFor(callback)
            else callback.err(e)
        }
    }

    private val LOGGER = LogManager.getLogger(UnityReader::class)

    private const val unityProjectTimeout = 300_000L // 5 min

    const val assetExtension = ".json"
    const val zeroAssetName = "0$assetExtension"

    private val FileReference.isSomeKindOfDirectory get() = isDirectory || isPacked
    private val FileReference.isPacked get() = !isDirectory && Promise.loadSync { isSerializedFolder(it) } == true

    fun getUnityProjectByRoot(root: FileReference, async: Boolean = false): UnityProject? {
        if (root.isSomeKindOfDirectory) {
            val children = root.listChildren()
            if (children.any {
                    if (it.isDirectory) it.listChildren()
                        .any { c -> c.lcExtension == "meta" }
                    else false
                }) {
                return UnityProjectCache.getFileEntry(
                    root, true, unityProjectTimeout
                ) { key, result ->
                    result.value = loadUnityProject(key.file)
                }.value
            }// else invalid project
        }
        return null
    }

    fun getUnityProjectByChild(file: FileReference, async: Boolean = false): UnityProject? {
        if (file.isDirectory) {
            val children = file.listChildren()
            if (children.any { it.lcExtension == "meta" }) {
                return UnityProjectCache.getFileEntry(file, true, unityProjectTimeout) { key, result ->
                    val root = key.file.getParent() // why the parent?
                    result.value = loadUnityProject(root)
                }?.value
            }// else invalid project
        }
        return null
    }

    private fun loadUnityProject(root: FileReference): UnityProject {
        return if (root is UnityPackageFolder) {
            // LOGGER.info("Fastest indexing ever <3")
            // fastest indexing ever <3
            root.project
        } else {
            LOGGER.info("Indexing files $root")
            val project = UnityProject(root)
            val hasAssetsFolder = project.register(root.getChild("Assets"))
            project.register(root.getChild("ProjectSettings"))
            project.register(root.getChild("UserSettings"))
            if (!hasAssetsFolder) {
                for (childFile in root.listChildren()) {
                    project.register(childFile)
                }
            }
            project.clock.total("Loading project ${root.name}")
            project
        }
    }

    fun findUnityProject(file: FileReference): UnityProject? {
        // LOGGER.debug("$file, ${file.exists}, ${file.isDirectory}, ${file.listChildren()}, ${file.length()}")
        if (!file.exists) {
            return null
        }
        var abs = file.absolutePath
        if (!abs.endsWith("/")) abs += "/"
        if (file.isDirectory) {
            val child = file.getChild("Assets")
            if (child.exists) {
                return getUnityProjectByChild(child)
            }
        }
        val key = "/Assets/"
        var endIndex = abs.lastIndex
        while (true) {
            val index = abs.lastIndexOf(key, endIndex)
            // if there are multiple indices, try all, from back first
            if (index > 0) {
                val root = getReference(abs.substring(0, index + key.length - 1))
                val project = getUnityProjectByChild(root)
                if (project != null) return project
                endIndex = min(index - 1, endIndex - 3)// correct???
            } else break
        }
        // wasn't found...
        // try last zip/rar/tar location
        val keys = listOf(".zip/", ".rar/", ".tar.gz/", ".tar/", ".gz/")
        for (keyI in keys) {
            val index = abs.lastIndexOf(keyI)
            if (index >= 0) {
                val root = getReference(abs.substring(0, index + keyI.length - 1))
                val project = getUnityProjectByRoot(root)
                if (project != null) return project
            }
        }
        return null
    }

    private fun decodePath(
        guid0: String, path: YAMLNode?, project: UnityProject,
        parentName: String? = null
    ): FileReference {
        return decodePath(guid0, path?.value, project, parentName)
    }

    private fun decodePath(
        guid0: String, path: String?, project: UnityProject,
        parentName: String?
    ): FileReference {
        path ?: return InvalidRef
        var fileId = ""
        var guid = guid0
        //var type = 0
        parseYAMLxJSON(path) { key, value ->
            when (key) {
                "fileID" -> fileId = value + assetExtension
                "guid" -> guid = value
                //"type" -> type = value.toInt()
                // else unknown stuff
            }
        }
        if (guid == guid0 && fileId == "0$assetExtension") {
            return InvalidRef
        }
        // val base = project.getGuidFolder(guid)
        // val fine = base.getChild(fileId)
        // LOGGER.info("Parsed $guid/$fileId for ids of path, $base, ${base.listChildren()?.map { it.name }}, $fine")
        assertTrue('/' !in guid)
        val guid1 = project.getChild(guid)
        var baseFile = guid1.getChildByNameOrFirst(fileId, parentName) ?: InvalidRef
        while (baseFile is InnerLinkFile) baseFile = baseFile.link
        return baseFile
    }

    private fun decodePathGetFileId(path: String?): String {
        path ?: return ""
        var fileId = ""
        //var type = 0
        parseYAMLxJSON(path) { key, value ->
            if (key == "fileID") {
                fileId = value + assetExtension
            }
        }
        return fileId
    }

    private fun FileReference.getChildByNameOrFirst(
        name: String, parentName: String?
    ): FileReference? {
        if (name.isBlank2()) return this
        if (name == "0.json") return InvalidRef
        val child = getChildImpl(name)
        if (child != InvalidRef) return child
        val children = if (isSomeKindOfDirectory) listChildren() else emptyList()
        var isSubMesh = false
        if (children.size == 1 &&
            name.length == "4300000.json".length &&
            name.startsWith("43000") &&
            name.endsWith(".json")
        ) {
            isSubMesh = true
            val meshes = children.first()
                .getChildImpl("Meshes").listChildren()
            if (meshes.size > 1) {
                val id = name.substring(0, name.length - 5).toInt() - 4300000
                // find submesh
                // todo find mesh by id... who defines the order?
                // for now just return the n-th child...
                if (parentName != null) {
                    val ext = "-002" // idk where that's coming from
                    val byName = meshes.firstOrNull {
                        val name = it.nameWithoutExtension
                        name == parentName ||
                                (name.length == parentName.length + ext.length &&
                                        name.startsWith(parentName))
                    }
                    if (byName != null) {
                        LOGGER.info("Located $name by parentName '$parentName': ${byName.name}")
                        return byName
                    }
                }

                val subMesh = meshes.getOrNull(id)
                if (subMesh != null) {
                    LOGGER.info("$name was missing from mesh file, choose ${subMesh.nameWithoutExtension} based on $id from ${meshes.map { it.nameWithoutExtension }}")
                    return subMesh
                } else LOGGER.warn("Submesh $id could not be found out of ${meshes.size}, from ${meshes.map { it.nameWithoutExtension }}")
            } else if (meshes.size == 1) {
                return meshes.first()
            } else {
                LOGGER.warn("Could not find submeshes in $this")
            }
        }
        val newChild = if (children.isNotEmpty()) {
            getChildImplOrNull("100100000.json")
                ?: getChildImplOrNull("Scene.json")
                ?: children.first()
        } else null
        if (!isSubMesh && name != "2800000.json") {
            // 4300000 is a magic for meshes,
            // 2800000 for textures,
            // 0 idk...
            // our logic is fine: Scene.json is being extracted here
            LOGGER.warn("$name is missing from $this, chose ${newChild?.name}, only found ${children.map { it.name }}")
        }
        return newChild
    }

    private fun readMaterial(node: YAMLNode, guid: String, prefab: Prefab, project: UnityProject) {
        val propertyMap = node["SavedProperties"]
        val isDoubleSided = node["DoubleSidedGI"]?.value != "0"
        if (isDoubleSided) prefab["cullMode"] = CullMode.BOTH
        if (propertyMap != null) {
            val floats = propertyMap["Floats"]
            if (floats != null) {
                val metallic = floats.getFloat("Metallic")
                // mapping correct???...
                if (metallic != null) prefab["metallicMinMax"] = Vector2f(metallic)
                val glossiness = floats.getFloat("Glossiness")
                if (glossiness != null) {// todo mapping correct??
                    prefab["roughnessMinMax"] = Vector2f(0f, 1f - glossiness)
                }
                // val hasShadows = floats.getFloat("ReceiveShadows")
                // could be used in the future for material, which shall not receive shadows :)
            } else LOGGER.warn("Missing floats")
            val colors = propertyMap["Colors"]
            if (colors != null) {
                // color vs baseColor?
                val color = colors.getColorAsVector4f("Color") ?: colors.getColorAsVector4f("BaseColor")
                if (color != null) prefab["diffuseBase"] = color
                val emissive = colors.getColorAsVector3f("EmissionColor")
                if (emissive != null) prefab["emissiveBase"] = emissive
            } else LOGGER.warn("Missing colors")
            val textures = propertyMap["TexEnvs"]?.packListEntries()?.associate { it.key to it }
            if (textures != null) {
                val diffuse = decodePath(guid, textures["MainTex"]?.get("Texture"), project)
                if (diffuse != InvalidRef) prefab["diffuseMap"] = diffuse
                // todo metallic/gloss-map...
                val occlusion = decodePath(guid, textures["OcclusionMap"]?.get("Texture"), project)
                if (occlusion != InvalidRef) prefab["occlusionMap"] = occlusion
                // todo normal map
                // = DetailNormalMap? no, that's secondary data
                val emissive = decodePath(guid, textures["EmissionMap"]?.get("Texture"), project)
                if (emissive != InvalidRef) prefab["emissiveMap"] = emissive
                // todo bump map
                // ...
            } else LOGGER.warn("Missing textures")
        } else LOGGER.warn("Missing saved properties")
        // todo decode shader: transparent?/...
        // LOGGER.info("Material $guid/$fileId: ${prefab.changes}")
    }

    fun applyTransformOnPrefab(
        prefab: Prefab,
        file: FileReference,
        node: YAMLNode,
        guid: String,
        project: UnityProject,
        knownChildren: HashSet<FileReference>
    ) {

        val localPos = node["LocalPosition"]?.getVector3d(0.0)
        val localRot = node["LocalRotation"]?.getQuaternion(1e-7)
        val localSca = node["LocalScale"]?.getVector3dScale(1e-7)

        if (localPos != null) prefab["position"] = localPos
        if (localRot != null) prefab["rotation"] = localRot
        if (localSca != null) prefab["scale"] = localSca

        // these changes are properties of the "Prefab" node, not the transform...
        val mods = node["Modification"]
        val prefabParent = decodePath(guid, mods?.get("TransformParent"), project)
        if (prefabParent != InvalidRef) {
            // to do set the parent if not already done
            // todo mark as used
            LOGGER.debug("Adding Transform from Prefab node, {}", file)
            knownChildren.add(file)
            val prefab = (prefabParent as PrefabReadable).readPrefab()
            addPrefabChild(prefab, file)
        }

        val changes = mods?.get("Modifications")?.children
        if (changes != null) {

            val position = Vector3d()
            val scale = Vector3d(1.0)
            val rotation = Quaterniond()

            for (change in changes) {

                val target = decodePath(guid, change["Target"], project)

                // e.g. m_LocalPosition.x -> LocalPosition.x,
                // m_Name -> Name, ...
                // RootOrder?
                // LocalRotation.x/y/z/w
                // LocalEulerAnglesHint.x/y/z?
                val path = beautify(change["PropertyPath"]!!.value.toString())
                val value = change["Value"]?.value // e.g. 1.723

                when (path) {
                    "Name" -> prefab["name"] = value ?: continue
                    // position
                    "LocalPosition.x" -> position.x = value?.toDoubleOrNull() ?: continue
                    "LocalPosition.y" -> position.y = value?.toDoubleOrNull() ?: continue
                    "LocalPosition.z" -> position.z = value?.toDoubleOrNull() ?: continue
                    // rotation
                    "LocalRotation.x" -> rotation.x = value?.toDoubleOrNull() ?: continue
                    "LocalRotation.y" -> rotation.y = value?.toDoubleOrNull() ?: continue
                    "LocalRotation.z" -> rotation.z = value?.toDoubleOrNull() ?: continue
                    "LocalRotation.w" -> rotation.w = value?.toDoubleOrNull() ?: continue
                    // scale
                    "LocalScale.x" -> scale.x = value?.toDoubleOrNull() ?: continue
                    "LocalScale.y" -> scale.y = value?.toDoubleOrNull() ?: continue
                    "LocalScale.z" -> scale.z = value?.toDoubleOrNull() ?: continue
                    // mmh... maybe for rotations with large angle?
                    "LocalEulerAnglesHint.x", "LocalEulerAnglesHint.y", "LocalEulerAnglesHint.z" -> {
                    }
                    "RootOrder" -> {
                    } // probably for changing the order of children
                    "Materials.Array.data[0]" -> {
                        LOGGER.debug("todo set material ... $value")
                        // todo set the material somehow... is it a material?
                    }
                    else -> {
                        // a reference to a local object, when dragging sth onto another thing
                        // value is then empty
                        val objectReference = decodePath(guid, change["ObjectReference"], project)
                        LOGGER.info("$target, Change: $path, value: $value, ref: $objectReference")
                    }
                }
            }

            if (position.lengthSquared() != 0.0) {
                prefab["position"] = position
            }

            if (scale.distanceSquared(1.0, 1.0, 1.0) > 1e-7) {
                prefab["scale"] = scale
            }

            if (abs(rotation.w - 1.0) > 1e-7) {
                prefab["rotation"] = rotation
            }

            // LOGGER.debug("pos rot sca: $position, $rotation, $scale by $changes")
        }
    }

    fun defineMaterial(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject) {
        prefab.clazzName = "Material"
        val name = node["Name"]?.value
        if (name != null) prefab["name"] = name
        readMaterial(node, guid, prefab, project)
    }

    fun defineBoxCollider(prefab: Prefab, node: YAMLNode) {
        /**
        --- !u!65 &65012021841241252
        BoxCollider:
        m_ObjectHideFlags: 1
        m_PrefabParentObject: {fileID: 0}
        m_PrefabInternal: {fileID: 100100000}
        m_GameObject: {fileID: 1352954537195680}
        m_Material: {fileID: 0}
        m_IsTrigger: 0
        m_Enabled: 1
        serializedVersion: 2
        m_Size: {x: 1.9708383, y: 1.4273263, z: 0.14620924}
        m_Center: {x: 0.00000014901161, y: 0, z: 0}
         * */
        val center = node.getVector3d(1e-5)
        val size = node.getVector3dScale(1e-5)
        // physics material?
        val isEnabled = node.getBool("Enabled")
        if (isEnabled == false) prefab["isEnabled"] = isEnabled
        val clazz = "BoxCollider"
        val path = if (center != null) {
            prefab.clazzName = "Entity"
            prefab[ROOT_PATH, "position"] = center
            prefab.add(ROOT_PATH, 'c', clazz)
        } else {
            prefab.clazzName = clazz
            ROOT_PATH
        }
        if (size != null) prefab[path, "halfExtents"] = size
        // todo this is not known/supported...
        // val isTrigger = node.getBool("IsTrigger")
        // if (isTrigger != null) prefab.set(path, "isTrigger", isTrigger) // mmh...
    }

    fun defineSphereCollider(prefab: Prefab, node: YAMLNode) {
        /**
        --- !u!65 &65012021841241252
        SphereCollider: ''
        ObjectHideFlags: 1
        PrefabParentObject: {fileID: 0}
        PrefabInternal: {fileID: 100100000}
        GameObject: {fileID: 1680666296336530}
        Material: {fileID: 0}
        IsTrigger: 0
        Enabled: 1
        SerializedVersion: 2
        Radius: 0.43986845
        Center: {x: 0.0000003799796, y: 0, z: 0}
         * */
        val center = node.getVector3d(1e-5)
        val size = node.getFloat("Radius")
        // physics material?
        val isEnabled = node.getBool("Enabled")
        val clazz = "SphereCollider"
        if (isEnabled == false) prefab["isEnabled"] = isEnabled
        val path = if (center != null) {
            prefab.clazzName = "Entity"
            prefab["position"] = center
            prefab.add(ROOT_PATH, 'c', clazz, clazz)
        } else {
            prefab.clazzName = clazz
            ROOT_PATH
        }
        prefab[path, "radius"] = size
        // todo this is not known/supported...
        // val isTrigger = node.getBool("IsTrigger")
        // if (isTrigger != null) prefab.set(path, "isTrigger", isTrigger) // mmh...
    }

    fun defineMeshCollider(
        prefab: Prefab, node: YAMLNode, guid: String,
        project: UnityProject, parentName: String?
    ) {
        /**
        --- !u!64 &64583973720899574
        MeshCollider:
        m_ObjectHideFlags: 1
        m_PrefabParentObject: {fileID: 0}
        m_PrefabInternal: {fileID: 100100000}
        m_GameObject: {fileID: 1812538408343958}
        m_Material: {fileID: 0}
        m_IsTrigger: 0
        m_Enabled: 1
        serializedVersion: 2
        m_Convex: 1
        m_InflateMesh: 0 // ?
        m_SkinWidth: 0.01 // ?
        m_Mesh: {fileID: 43862125802689732, guid: c1e9ae942424bfa449f2bc5ea9448db7, type: 2}
         * */
        // physics material?
        prefab.clazzName = "MeshCollider"
        val isEnabled = node.getBool("Enabled")
        if (isEnabled == false) prefab["isEnabled"] = isEnabled
        // todo program triggers...
        // val isTrigger = node.getBool("IsTrigger")
        // if (isTrigger != null) prefab.set(ROOT_PATH, "isTrigger", isTrigger) // mmh...
        val isConvex = node.getBool("Convex")
        if (isConvex == false) prefab[ROOT_PATH, "isConvex"] = isConvex
        // todo is the mesh correct? from what i've found, it was just empty...
        val mesh = decodePath(guid, node["Mesh"], project, parentName)
        if (mesh != InvalidRef) prefab[ROOT_PATH, "meshFile"] = mesh
    }

    fun defineMeshFilter(
        prefab: Prefab, node: YAMLNode, guid: String,
        project: UnityProject, parentName: String?
    ): FileReference {
        // later add a Unity-MeshRenderer or similar for the list of materials
        prefab.clazzName = "MeshComponent"
        prefab["meshFile"] = decodePath(guid, node["Mesh"], project, parentName)
        return decodePath(guid, node["GameObject"], project)
    }

    fun String.parseIndexBuffer(): IntArray {
        /*
        * 0b00 0400 0f00 0200 0100 0300 0100 02000900020004000900070001000900050007000900040006000a00060008000a00090004000a
        * 00030006000b00060004000b00070000000c00000008000c00080006000c00070005000d00000007000d0008000000
        * 0d00050009000d000a0008000d0009000a000d00030001000e00060003000e00010007000e000c0006000e0007000c000e000200
        * 03000f00040002000f0003000b000f00
        * */
        val indices = IntArray(length / 4)
        for (i in indices.indices) {
            val o = i * 4
            indices[i] = parseHex(this[o], this[o + 1]) or parseHex(this[o + 2], this[o + 3]).shl(8)
        }
        return indices
    }

    fun String.parseBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(length / 2)
            // byte order is defined by Unity's file format -> must not be changed!
            .order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until length / 2) {
            val o = i * 2
            buffer.put(i, parseHex(this[o], this[o + 1]).toByte())
        }
        return buffer
    }

    @Suppress("UNUSED_PARAMETER")
    fun defineMesh(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject) {
        // sample: Military, SM_Veh_Rocket_Truck_01_Convex 1.asset
        prefab.clazzName = "Mesh"
        val indexBuffer = node["IndexBuffer"]?.value?.parseIndexBuffer()
        if (indexBuffer != null) {
            prefab["indices"] = indexBuffer
        }
        val vertexData = node["VertexData"]
        if (vertexData != null) {
            val vertexCount = vertexData["VertexCount"]?.value?.toInt() ?: 0
            val dataSize = vertexData["DataSize"]?.value?.toInt() ?: 0
            val data = vertexData["Typelessdata"]?.value?.parseBuffer() // dataSize bytes in hex
            if (data == null) LOGGER.warn("typeless-data wasn't found! ${vertexData.children.map { it.key }}")
            // there are channels, and they probably define position,normal,...
            // how many channels are actually filled with data (dimension > 0)
            // val numChannels = vertexData["CurrentChannels"]?.value?.toInt()
            // format 0 is little endian float
            val channels = vertexData["Channels"]?.children ?: emptyList()
            if (data != null && channels.isNotEmpty()) {
                val format0 = channels[0]["Format"]?.value?.toInt()
                val dimension0 = channels[0]["Dimension"]?.value?.toInt()
                if (format0 == 0 && dimension0 == 3 && vertexCount > 0) {
                    val stride = dataSize / vertexCount
                    val offset = channels[0]["Offset"]?.value?.toInt() ?: 0
                    val positions = FloatArray(vertexCount * 3)
                    for (i in 0 until vertexCount) {
                        val indexOut = i * 3
                        val indexIn = i * stride + offset
                        // x is mirrored (in Synty's collision models), why ever...
                        positions[indexOut + 0] = -data.getFloat(indexIn)
                        positions[indexOut + 1] = +data.getFloat(indexIn + 4)
                        positions[indexOut + 2] = +data.getFloat(indexIn + 8)
                    }
                    prefab["positions"] = positions
                }
            }
        }
    }

    private fun addPrefabChild(prefab: Prefab, path: FileReference) {
        if (path is PrefabReadable) {
            val child = path.readPrefab()
            val type = if (child.clazzName == "Entity") 'e' else 'c'
            val nameId = child["name"] as? String ?: path.nameWithoutExtension // collisions should be rare
            val add = CAdd(ROOT_PATH, type, child.clazzName, nameId, path)
            if (!prefab.canAdd(add)) add.nameId = path.nameWithoutExtension
            while (!prefab.canAdd(add)) add.nameId += "-"
            prefab.add(add, prefab.findNextIndex(type, ROOT_PATH))
        }
    }

    fun readUnityObjects(
        root: YAMLNode, guid: String, project: UnityProject,
        folder: InnerFolder
    ): FileReference {

        if (!isValidUUID(guid)) {
            LOGGER.warn("Invalid guid '$guid'")
            return InvalidRef
        }

        var nodeCount = 0
        lateinit var lastNode: FileReference

        // first the members all need to be registered as files
        forAllUnityObjects(root) { fileId, node ->
            nodeCount++
            lastNode = folder.getOrPut(fileId) {
                val prefab = Prefab("Entity")
                val file = InnerPrefabFile(folder.absolutePath + "/" + fileId, fileId, folder, prefab)
                file.hide()
                prefab.sourceFile = file
                prefab["name"] = node.key // this a class type, not really a name
                file
            }
        }

        if (nodeCount == 0) {
            LOGGER.warn("Didn't find any nodes for $guid")
            return folder
        }

        // fileId[self] -> name
        val parentNames = HashMap<String, String>()

        forAllUnityObjects(root) { fileId, node ->
            if (node.key == "GameObject") {
                val name = node["Name"]?.value
                val components = node["Component"]
                if (components != null && name != null) {
                    val children = components.children
                    for (i in children.indices) {
                        val componentFileId = decodePathGetFileId(children[i].children.first().value)
                        if (componentFileId != "") {
                            parentNames[componentFileId] = name
                        }
                    }
                }
            }
        }

        val sceneFile = folder.getOrPut("Scene.json") {
            val absolutePath = folder.absolutePath + "/Scene.json"
            if (nodeCount == 1) {
                InnerLinkFile(absolutePath, "Scene.json", folder, lastNode)
            } else {
                // children will be added later
                val prefab = Prefab("Entity")
                val file = InnerPrefabFile(absolutePath, "Scene.json", folder, prefab)
                prefab.sourceFile = file
                file
            }
        }

        val meshesByGameObject = HashMap<FileReference, ArrayList<Prefab>>()
        val transformToGameObject = BiMap<FileReference, FileReference>()
        val knownChildren = HashSet<FileReference>()
        val fileToPrefab = HashMap<FileReference, YAMLNode>()

        // parse all instances roughly, except relations
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            when (node.key) {
                "Transform" -> {
                    val gameObject0 = node["GameObject"]
                    val gameObject1 = decodePath(guid, gameObject0, project)
                    if (gameObject1 is PrefabReadable) {
                        transformToGameObject[file] = gameObject1
                        applyTransformOnPrefab(prefab, file, node, guid, project, knownChildren)
                    } else {
                        // todo probably stripped, handle it correctly
                        transformToGameObject[file] = file // hack
                        /*val pi = decodePath(guid, node["PrefabInternal"], project)
                        if (pi is PrefabReadable) {
                            transformToGameObject[file] = pi
                        }*/
                    }
                    // LOGGER.debug("transform $fileId has game object $gameObject, ${gameObject is PrefabReadable}")
                }

                // to do supports tags(?)
                "OcclusionCullingSettings", "RenderSettings",
                "LightmapSettings", "NavMeshSettings",
                "Behaviour", "Animator", "AudioListener" -> {
                    // not used, and no use as assets
                    file.hide()
                }
                "Camera" -> {
                    // todo are those properties with the spaces correct?
                    prefab.clazzName = "Camera"
                    val near = node["Near Clip Plane"]?.getFloat()
                    if (near != null) prefab["near"] = near
                    val far = node["Far Clip Plane"]?.getFloat()
                    if (far != null) prefab["far"] = far
                    if (node["Orthographic"]?.getBool() == true) {
                        prefab["isPerspective"] = false
                    }
                    val fov = node["Field Of View"]?.getFloat()
                    if (fov != null) {
                        prefab["fovY"] = fov
                    }
                    val fov2 = node["Orthographic Size"]?.getFloat()
                    if (fov2 != null) {
                        prefab["fovOrthographic"] = fov2
                    }
                }
                "Light" -> {
                    // to do copy shadow map settings, angle and such
                    val color = node["Color"]?.getColorAsVector3f() ?: Vector3f(1f)
                    // todo find out all light types
                    // 1 = directional light
                    prefab.clazzName = "DirectionalLight"
                    prefab["color"] = color.mul(30f)
                }
                "Material" -> defineMaterial(prefab, node, guid, project)
                "BoxCollider" -> defineBoxCollider(prefab, node)
                "SphereCollider" -> defineSphereCollider(prefab, node)
                "Mesh" -> defineMesh(prefab, node, guid, project)
                "MeshCollider" -> {
                    val parentName = parentNames[fileId]
                    defineMeshCollider(prefab, node, guid, project, parentName)
                }
                "MeshFilter" -> {
                    file.hide()
                    val parentName = parentNames[fileId]
                    val gameObjectKey = defineMeshFilter(prefab, node, guid, project, parentName)
                    meshesByGameObject.getOrPut(gameObjectKey) { ArrayList() }.add(prefab)
                }
                "SkinnedMeshRenderer" -> {
                    file.hide()
                    val parentName = parentNames[fileId]
                    val gameObjectKey = defineMeshFilter(prefab, node, guid, project, parentName)
                    meshesByGameObject.getOrPut(gameObjectKey) { ArrayList() }.add(prefab)
                }
                "MonoBehaviour" -> {
                    // an unknown script plus properties
                    // in the original without _ and without m_
                    val script = decodePath(guid, node["Script"], project)
                    if (script != InvalidRef) {
                        val script1 = try {
                            script.readTextSync()
                        } catch (_: Exception) {
                            script
                        }
                        LOGGER.debug("script for behaviour: {}", script1)
                    }
                }
                "Prefab" -> fileToPrefab[file] = node
            }
        }

        // add their transforms / prefabs / changes
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            when (node.key) {
                // todo skinned mesh renderer doesn't seem to work yet
                "MeshRenderer", "SkinnedMeshRenderer" -> {
                    file.hide()
                    val gameObjectKey = decodePath(guid, node["GameObject"], project)
                    val castShadows = node.getBool("CastShadows") ?: true
                    val receiveShadows = node.getBool("ReceiveShadows") ?: true
                    val isEnabled = node.getBool("Enabled") ?: true
                    val materials = node["Materials"]
                    val meshes = meshesByGameObject[gameObjectKey]
                    if (!meshes.isNullOrEmpty()) {
                        val materialList = materials?.packListEntries()
                            ?.map { decodePath(guid, it, project) }
                        for (mesh in meshes) {
                            if (!isEnabled) mesh["isEnabled"] = false
                            if (materials != null) mesh["materials"] = materialList
                            if (!castShadows) mesh["castShadows"] = false
                            if (!receiveShadows) mesh["receiveShadows"] = false
                        }
                    }
                }
            }
        }

        // todo then parse all their relations, and assign paths
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            when (node.key) {
                "Prefab" -> {
                    file.hide() // not visible
                    val children = node["Children"]
                    if (children != null) {
                        val children2 = children.packListEntries()
                        for (childNode in children2) {
                            val childPath = decodePath(guid, childNode, project)
                            addPrefabChild(prefab, childPath)
                            knownChildren.add(childPath)
                        }
                    }
                    val base = node["PrefabParent"]
                    if (base != null) {
                        val link = decodePath(guid, base, project)
                        LOGGER.info("[540, Prefab.PrefabParent] Set root object of $fileId to $link")
                        prefab.parentPrefabFile = link
                    }
                }
                "Transform" -> {
                    val prefab2 = node["PrefabParentObject"]
                    val prefabPath = decodePath(guid, prefab2, project)
                    if (prefabPath != InvalidRef) {
                        // LOGGER.info("Setting $file.prefab = $prefabPath")
                        prefab.parentPrefabFile = prefabPath
                    }
                    val children = node["Children"]
                    if (children != null) {
                        val children2 = children.packListEntries()
                        // LOGGER.info("Processing ${children2.size} children from transform, adding to $file")
                        for (childNode in children2) {
                            val path0 = decodePath(guid, childNode, project)
                            val path1 = transformToGameObject.reverse[path0] ?: path0
                            addPrefabChild(prefab, path1)
                            knownChildren.add(path1)
                        }
                    }
                    val prefab3 = node["PrefabInternal"]
                    val prefabPath2 = decodePath(guid, prefab3, project)
                    val prefabNode = fileToPrefab[prefabPath2]
                    if (prefabNode != null) {
                        // hopefully correct...
                        // todo this is not quite correct...
                        // E:/Assets/humbleLowPoly/polygonwestern_syntystudios_windows.zip/POLYGON_Western_SyntyStudios/POLYGON_Western_Unity_Package_2017_1.unitypackage/Assets/PolygonWestern/Demo/Demo.unity
                        applyTransformOnPrefab(prefab, file, prefabNode, guid, project, knownChildren)
                    }
                }
                "GameObject" -> {
                    val prefab2 = node["PrefabParentObject"]
                    val prefabPath = decodePath(guid, prefab2, project)
                    if (prefabPath != InvalidRef) {
                        LOGGER.info("[570, GameObject.PrefabParentObject] Set $fileId.prefab to $prefabPath")
                        // prefab.prefab = prefabPath
                    }
                    // find transform, which belongs to this GameObject
                    // LOGGER.debug("looking up gameObject $fileId.transform with $file")
                    val transform = transformToGameObject.reverse[file]
                    val components = node["Component"]
                    val prefab3 = (transform as? PrefabReadable)?.readPrefab()
                    if (components != null && prefab3 != null) {
                        val children = components.children
                        for (i in children.indices) {
                            val childPath = decodePath(guid, children[i].children.first(), project)
                            if (childPath is PrefabReadable) {
                                val child = childPath.readPrefab()
                                val type2 = child.instanceName
                                if (type2 != "Transform") {
                                    addPrefabChild(prefab3, childPath)
                                    knownChildren.add(childPath)
                                }
                            }
                        }
                    }
                    if (prefab3 != null) {
                        file.hide() // no longer visibly used
                        val isActive = node.getBool("IsActive")
                        if (isActive == false) prefab["isEnabled"] = false
                        val name = node["Name"]?.value
                        if (name != null) {
                            prefab3["name"] = name
                        }
                    }
                }
            }
        }

        // find root, and create Scene.json-link with that
        /*var hasFoundRoot = false
        forAllUnityObjects(root) { _, node ->
            if (!hasFoundRoot) {
                val root2 = node["RootGameObject"]
                if (root2 != null) {
                    val path = decodePath(guid, root2, project)
                    if (path != InvalidRef) {
                        val fileName = "Scene.json"
                        folder.getOrPut(fileName) {
                            InnerLinkFile(folder.absolutePath + "/" + fileName, fileName, folder, path)
                        }
                        hasFoundRoot = true
                    }
                }
            }
        }*/

        if (nodeCount > 1) {
            val scenePrefab = (sceneFile as PrefabReadable).readPrefab()
            forAllUnityObjects(root) { fileId, node ->
                val file = folder.get(fileId) as InnerPrefabFile
                when (node.key) {
                    "Transform" -> {
                        if (file !in knownChildren) {
                            LOGGER.debug("Adding $fileId to Scene.json, because it hasn't been added yet")
                            addPrefabChild(scenePrefab, file)
                        }
                    }
                }
            }
        }

        // apply write-protection to all internal prefabs,
        // because we cannot save changes to them
        forAllUnityObjects(root) { fileId, _ ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            flattenPrefab(prefab)
            prefab.sealFromModifications()
        }

        return folder
    }

    /**
     * flatten all prefabs, so we properly can work with them
     *  meaning we want to include all adds
     *  aka remove its prefab-ref, apply its changes directly onto our elements if not overridden
     * */
    private fun flattenPrefab(prefab: Prefab) {
        for ((path, adds) in prefab.adds) {
            for (i in adds.indices) {
                val add = adds[i]
                val srcFile = add.prefab
                if (srcFile is PrefabReadable) {
                    val addPrefab = srcFile.readPrefab()
                    flattenPrefab(addPrefab) // hopefully not killing us

                    add.prefab = InvalidRef
                    val basePath = add.getSetterPath(path, i)
                    copyOverAdds(prefab, addPrefab, basePath)
                    copyOverSets(prefab, addPrefab, basePath)
                }
            }
        }
    }

    private fun copyOverAdds(prefab: Prefab, addPrefab: Prefab, basePath: Path) {
        for ((path, adds) in addPrefab.adds) {
            for (i in adds.indices) {
                val add = adds[i]
                val newPath = basePath + path
                prefab.add(add.withPath(newPath, false), i)
            }
        }
    }

    private fun copyOverSets(prefab: Prefab, addPrefab: Prefab, basePath: Path) {
        addPrefab.sets.forEach { path, key, value ->
            val newPath = basePath + path
            if (prefab[newPath, key] == null) {
                prefab[newPath, key] = value
            }
        }
    }

    private fun forAllUnityObjects(node: YAMLNode, callback: (fileId: String, node: YAMLNode) -> Unit) {
        val children = node.children
        if (!children[0].key.startsWith("%YAML")) {
            LOGGER.warn("Not a unity yaml file: $node")
            return
        }
        var index = -1
        while (++index < children.size) {
            val descriptor = children[index]
            val key = descriptor.key
            if (index + 1 < children.size && key.startsWith("--- !u!")) {
                // --- !u!21 &2100000
                // Material:
                // ...
                val fileIdIndex = key.indexOf('&')
                val isStripped = key.endsWith("stripped") // just a link to a prefab
                val fileId = if (fileIdIndex < 0) zeroAssetName else key
                    .substring(fileIdIndex + 1)
                    .run { if (isStripped) split(' ')[0] else this } + assetExtension
                val main = children[index + 1]
                // LOGGER.info("$key -> $fileId (${main.key})")
                callback(fileId, main)
                index++
            } else if (descriptor.children.isNotEmpty()) {
                // LOGGER.info("$key -> ? (${descriptor.key})")
                callback(zeroAssetName, descriptor)
            }
        }
    }

    fun readAsAsset(file: FileReference, callback: Callback<FileReference?>) {
        val project = findUnityProject(file)
        if (project == null) {
            LOGGER.warn("No project found in $file")
            // try to read without project
            /* file.inputStream { s, e ->
                 if (s != null) {
                     val node = parseYAML(s.bufferedReader(), true)
                     val tmpFolder = InnerFolder(file)
                     val objects = readUnityObjects(node, "0", invalidProject, tmpFolder)
                     if (objects !== tmpFolder) callback.ok(null)
                     else callback.ok(objects.listChildren().firstOrNull())
                 } else callback.err(e)
             }*/
            callback.ok(null)
        } else {
            LOGGER.debug("Found unity project for {}: {} :)", file, project)
            val objects = project.getGuidFolder(file)
            val scene = objects.getChildImpl("Scene.json")
            if (scene != InvalidRef) {
                callback.ok(scene)
                return
            }
            val children = objects.listChildren()
            if (children.size <= 1) {
                callback.ok(children.firstOrNull())
                return
            }
            val meta = project.getMeta(file)
            // find the main id
            val mainFileId = project.getMainId(meta)
            val file2 = if (mainFileId != null) {
                objects.getChildImpl(mainFileId)
            } else {
                // get the object with the lowest id
                children.minByOrNull { it.nameWithoutExtension.toLongOrDefault(Long.MAX_VALUE) }
            }
            callback.ok(file2)
        }
    }

    fun readAsFolder(file: FileReference): FileReference {

        val project = findUnityProject(file)
        if (project == null) {
            LOGGER.warn("No project found in $file")
            return InvalidRef
        }

        LOGGER.debug("returning {}", project.getGuidFolder(file))
        return project.getGuidFolder(file)
    }

    // todo rename doesn't work in zips (shouldn't, but it should show a message or sth like that)
    // todo option to hide .meta files in the file explorer, as it's just distracting

}