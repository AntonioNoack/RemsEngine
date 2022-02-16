package me.anno.io.unity

import me.anno.Engine
import me.anno.cache.CacheData
import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.json.JsonFormatter
import me.anno.io.unity.UnityProject.Companion.invalidProject
import me.anno.io.unity.UnityProject.Companion.isValidUUID
import me.anno.io.yaml.YAMLNode
import me.anno.io.yaml.YAMLReader.beautify
import me.anno.io.yaml.YAMLReader.parseYAML
import me.anno.io.yaml.YAMLReader.parseYAMLxJSON
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerLinkFile
import me.anno.io.zip.InnerPrefabFile
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.utils.ColorParsing.parseHex
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.Tabs
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Quaterniond
import org.joml.Vector2f
import org.joml.Vector3d
import java.nio.ByteBuffer
import java.nio.ByteOrder

object UnityReader {

    val unityExtensions = listOf("mat", "prefab", "unity", "asset", "controller", "meta")

    private val LOGGER = LogManager.getLogger(UnityReader::class)

    private const val unityProjectTimeout = 300_000L // 5 min

    val assetExtension = ".json"
    val zeroAssetName = "0$assetExtension"

    fun getUnityProject(file: FileReference, async: Boolean = false): UnityProject? {
        if (file.isDirectory) {
            val children = file.listChildren() ?: return null
            if (children.any { it.lcExtension == "meta" }) {
                val data = UnityProjectCache.getEntry(file, unityProjectTimeout, async) {
                    val root = file.getParent()!!
                    if (root is UnityPackageFolder) {
                        LOGGER.info("Fastest indexing ever <3")
                        // fastest indexing ever <3
                        CacheData(root.project)
                    } else {
                        LOGGER.info("Indexing files $root")
                        val project = UnityProject(root)
                        project.register(root.getChild("Assets"))
                        project.register(root.getChild("ProjectSettings"))
                        project.register(root.getChild("UserSettings"))
                        project.clock.total("Loading project ${root.name}")
                        CacheData(project)
                    }
                } as? CacheData<*>
                return data?.value as? UnityProject
            }// else invalid project
        }
        return null
    }

    fun findUnityProject(file: FileReference): UnityProject? {
        // println("$file, ${file.exists}, ${file.isDirectory}, ${file.listChildren()}, ${file.length()}")
        if (!file.exists) return null
        val key = "/Assets/"
        val abs = file.absolutePath
        if (file.isDirectory) {
            val child = getReference(file, "Assets")
            if (child.exists) return getUnityProject(child)
        }
        if (abs.endsWith("/Assets")) {
            return getUnityProject(file)
        } else {
            val index = abs.lastIndexOf(key)
            // todo if there are multiple indices, try all,
            //  from back first?
            if (index > 0) {
                val file2 = getReference(abs.substring(0, index + key.length - 1))
                // LOGGER.info("$file2, ${file2.isDirectory}, ${file2.exists}")
                return getUnityProject(file2)
            }
        }
        return null
    }

    private fun decodePath(guid0: String, path: YAMLNode?, project: UnityProject): FileReference {
        return decodePath(guid0, path?.value, project)
    }

    private fun decodePath(guid0: String, path: String?, project: UnityProject): FileReference {
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
        // val base = project.getGuidFolder(guid)
        // val fine = base.getChild(fileId)
        // LOGGER.info("Parsed $guid/$fileId for ids of path, $base, ${base.listChildren()?.map { it.name }}, $fine")
        var baseFile = project.getChild(guid).getChildByNameOrFirst(fileId) ?: InvalidRef
        while (baseFile is InnerLinkFile) baseFile = baseFile.link
        return baseFile
    }

    private fun FileReference.getChildByNameOrFirst(name: String): FileReference? {
        if (name.isBlank2()) return this
        val byName = getChild(name)
        if (byName != InvalidRef) return byName
        LOGGER.warn("$name is missing from $this, only found ${listChildren()?.map { it.name }}")
        return if (isSomeKindOfDirectory) listChildren()?.firstOrNull() else null
    }

    private fun readMaterial(node: YAMLNode, guid: String, prefab: Prefab, project: UnityProject) {
        val propertyMap = node["SavedProperties"]
        val isDoubleSided = node["DoubleSidedGI"]?.value != "0"
        if (isDoubleSided) prefab.setProperty("isDoubleSided", true)
        if (propertyMap != null) {
            val floats = propertyMap["Floats"]
            if (floats != null) {
                val metallic = floats.getFloat("Metallic")
                // mapping correct???...
                if (metallic != null) prefab.setProperty("metallicMinMax", Vector2f(metallic))
                val glossiness = floats.getFloat("Glossiness")
                if (glossiness != null) {// todo mapping correct??
                    prefab.setProperty("roughnessMinMax", Vector2f(0f, 1f - glossiness))
                }
                // val hasShadows = floats.getFloat("ReceiveShadows")
                // could be used in the future for material, which shall not receive shadows :)
            } else LOGGER.warn("Missing floats")
            val colors = propertyMap["Colors"]
            if (colors != null) {
                // color vs baseColor?
                val color = colors.getColorAsVector4f("Color") ?: colors.getColorAsVector4f("BaseColor")
                if (color != null) prefab.setProperty("diffuseBase", color)
                val emissive = colors.getColorAsVector3f("EmissionColor")
                if (emissive != null) prefab.setProperty("emissiveBase", emissive)
            } else LOGGER.warn("Missing colors")
            val textures = propertyMap["TexEnvs"]?.packListEntries()
            if (textures != null) {
                val diffuse = decodePath(guid, textures["MainTex"]?.get("Texture"), project)
                if (diffuse != InvalidRef) prefab.setProperty("diffuseMap", diffuse)
                // todo metallic/gloss-map...
                val occlusion = decodePath(guid, textures["OcclusionMap"]?.get("Texture"), project)
                if (occlusion != InvalidRef) prefab.setProperty("occlusionMap", occlusion)
                // todo normal map
                // = DetailNormalMap? no, that's secondary data
                val emissive = decodePath(guid, textures["EmissionMap"]?.get("Texture"), project)
                if (emissive != InvalidRef) prefab.setProperty("emissiveMap", emissive)
                // todo bump map
                // ...
            } else LOGGER.warn("Missing textures")
        } else LOGGER.warn("Missing saved properties")
        // todo decode shader: transparent?/...
        // LOGGER.info("Material $guid/$fileId: ${prefab.changes}")
    }

    fun applyTransformOnPrefab(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject) {

        val changes = node["Modification"]?.get("Modifications")?.children ?: emptyList()

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
            // a reference to a local object, when dragging sth onto another thing
            // value is then empty
            val objectReference = decodePath(guid, change["ObjectReference"], project)

            when (path) {
                "Name" -> prefab.setProperty("name", value ?: continue)
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
                    println("todo set material ... $value")
                    // todo set the material somehow... is it a material?
                }
                else -> LOGGER.info("$target, Change: $path, value: $value, ref: $objectReference")
            }

            if (position.lengthSquared() != 0.0) {
                prefab.setProperty("position", position)
            }

            if (scale.distanceSquared(1.0, 1.0, 1.0) != 0.0) {
                prefab.setProperty("scale", scale)
            }

            if (rotation.w != 1.0) {
                prefab.setProperty("rotation", rotation)
            }

        }
    }

    fun defineMaterial(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject) {
        prefab.clazzName = "Material"
        val name = node["Name"]?.value
        if (name != null) prefab.setProperty("name", name)
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
        if (isEnabled == false) prefab.setProperty("isEnabled", isEnabled)
        val path = if (center != null) {
            prefab.clazzName = "Entity"
            prefab.set(ROOT_PATH, "position", center)
            prefab.add(ROOT_PATH, 'c', "BoxCollider", 0)
        } else {
            prefab.clazzName = "BoxCollider"
            ROOT_PATH
        }
        if (size != null) prefab.set(path, "halfExtends", size)
        // todo this is not known/supported...
        // val isTrigger = node.getBool("IsTrigger")
        // if (isTrigger != null) prefab.set(path, "isTrigger", isTrigger) // mmh...
    }

    fun defineMeshCollider(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject) {
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
        if (isEnabled == false) prefab.setProperty("isEnabled", isEnabled)
        // todo program triggers...
        // val isTrigger = node.getBool("IsTrigger")
        // if (isTrigger != null) prefab.set(ROOT_PATH, "isTrigger", isTrigger) // mmh...
        val isConvex = node.getBool("Convex")
        if (isConvex == false) prefab.set(ROOT_PATH, "isConvex", isConvex)
        // todo is the mesh correct? from what i've found, it was just empty...
        val mesh = decodePath(guid, node["Mesh"], project)
        if (mesh != InvalidRef) prefab.set(ROOT_PATH, "meshFile", mesh)
    }

    fun defineMeshFilter(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject): String {
        // later add a Unity-MeshRenderer or similar for the list of materials
        prefab.clazzName = "MeshComponent"
        prefab.setProperty("mesh", decodePath(guid, node["Mesh"], project))
        return node["GameObject"].toString()
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

    fun defineMesh(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject) {
        // sample: Military, SM_Veh_Rocket_Truck_01_Convex 1.asset
        prefab.clazzName = "Mesh"
        val indexBuffer = node["IndexBuffer"]?.value?.parseIndexBuffer()
        if (indexBuffer != null) {
            prefab.setProperty("indices", indexBuffer)
        }
        val vertexData = node["VertexData"]
        if (vertexData != null) {
            val vertexCount = vertexData["VertexCount"]?.value?.toInt() ?: 0
            val dataSize = vertexData["DataSize"]?.value?.toInt() ?: 0
            val data = vertexData["Typelessdata"]?.value?.parseBuffer() // dataSize bytes in hex
            if (data == null) LOGGER.warn("typeless-data wasn't found! ${vertexData.children?.map { it.key }}")
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
                        positions[indexOut + 0] = data.getFloat(indexIn)
                        positions[indexOut + 1] = data.getFloat(indexIn + 4)
                        positions[indexOut + 2] = data.getFloat(indexIn + 8)
                    }
                    prefab.setProperty("positions", positions)
                }
            }
        }
    }

    fun addPrefabChild(prefab: Prefab, path: FileReference) {
        if (path is PrefabReadable) {
            val child = path.readPrefab()
            val type = if (child.clazzName == "Entity") 'e' else 'c'
            val name = child.instanceName ?: child.clazzName
            prefab.add(ROOT_PATH, type, child.clazzName, name, path)
        }
    }

    fun readUnityObjects(root: YAMLNode, guid: String, project: UnityProject, folder: InnerFolder): FileReference {

        if (!isValidUUID(guid)) return InvalidRef
        root.children ?: return InvalidRef

        // first the members all need to be registered as files
        forAllUnityObjects(root) { fileId, node ->
            folder.getOrPut(fileId) {
                val prefab = Prefab("Entity")
                val file = InnerPrefabFile(folder.absolutePath + "/" + fileId, fileId, folder, prefab)
                prefab.source = file
                prefab.setProperty("description", node.key)
                file
            }
        }

        val meshesByGameObject = HashMap<String, ArrayList<Prefab>>()
        val transformsByGameObject = HashMap<FileReference, YAMLNode>()

        // parse all instances roughly, except relations
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            // LOGGER.info("rough parsing $fileId -> ${node.key}")
            when (node.key) {
                "Transform" -> {
                    val gameObject = decodePath(guid, node["GameObject"], project)
                    if (gameObject != InvalidRef) {
                        transformsByGameObject[gameObject] = node
                    } else LOGGER.warn("Could not find game object for transform")
                }
                "Material" -> defineMaterial(prefab, node, guid, project)
                "BoxCollider" -> defineBoxCollider(prefab, node)
                "Mesh" -> defineMesh(prefab, node, guid, project)
                "MeshCollider" -> defineMeshCollider(prefab, node, guid, project)
                "MeshFilter" -> {
                    val gameObjectKey = defineMeshFilter(prefab, node, guid, project)
                    meshesByGameObject.getOrPut(gameObjectKey) { ArrayList() }
                        .add(prefab)
                }
                "MonoBehaviour" -> {
                    // an unknown script plus properties
                    // in the original without _ and without m_
                    val script = decodePath(guid, node["Script"], project)
                    if (script != InvalidRef) println(
                        "script for behaviour: ${
                            try {
                                script.readText()
                            } catch (e: Exception) {
                                script
                            }
                        }"
                    )
                    /*for (child in node.children ?: emptyList()) {
                        if (child.value?.startsWith("{fileID") == true) {
                            addPrefabChild(prefab, decodePath(guid, child, project))
                        }
                        for (child2 in child.children ?: emptyList()) {
                            if (child2.value?.startsWith("{fileID") == true) {
                                addPrefabChild(prefab, decodePath(guid, child2, project))
                            }
                        }
                    }*/
                }
            }
        }

        // add their transforms / prefabs / changes
        val transformToGameObject = HashMap<FileReference, FileReference>()
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            // val prefab = file.readPrefab()
            when (node.key) {
                "MeshRenderer" -> {
                    val gameObjectKey = node["GameObject"].toString()
                    // val castShadows = node.getBool("CastShadows") ?: true
                    // val receiveShadows = node.getBool("ReceiveShadows") ?: true
                    val isEnabled = node.getBool("Enabled") ?: true
                    val materials = node["Materials"]
                    val meshes = meshesByGameObject[gameObjectKey]
                    if (meshes != null && meshes.isNotEmpty()) {
                        val materialList = materials?.packListEntries()?.children
                            ?.map { decodePath(guid, it, project) }
                        for (mesh in meshes) {
                            if (!isEnabled) mesh.setProperty("isEnabled", false)
                            if (materials != null) mesh.setProperty("materials", materialList)
                        }
                    }
                }
                "Transform" -> {
                    val goFile = decodePath(guid, node["GameObject"], project)
                    if (goFile is PrefabReadable) {
                        transformToGameObject[file] = goFile
                    } else {
                        val pi = decodePath(guid, node["PrefabInternal"], project)
                        if (pi is PrefabReadable) {
                            transformToGameObject[file] = pi
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
                    val children = node["Children"]
                    if (children != null) {
                        /*
                          - {fileID: 861958571}
                          - {fileID: 1586427708}
                          - {fileID: 1067863450}
                          - {fileID: 1166300874}
                        * */
                        val children2 = children.packListEntries().children
                        if (children2 != null) {
                            LOGGER.info("processing ${children2.size} children from prefab")
                            for (childNode in children2) {
                                addPrefabChild(prefab, decodePath(guid, childNode, project))
                            }
                        }
                    }
                    val rootGameObject = node["RootGameObject"]
                    if (rootGameObject != null) {
                        val link = decodePath(guid, rootGameObject, project)
                        // LOGGER.info("set root object of $fileId to $link")
                        prefab.prefab = link
                    }
                }
                "Transform" -> {
                    val children = node["Children"]
                    if (children != null) {
                        val file2 = transformToGameObject[file]
                        if (file2 is PrefabReadable) {
                            val prefab2 = file2.readPrefab()
                            /*
                             - {fileID: 861958571}
                             - {fileID: 1586427708}
                             - {fileID: 1067863450}
                             - {fileID: 1166300874}
                            */
                            val children2 = children.packListEntries().children
                            LOGGER.info("processing ${children2?.size} children from transform, adding to $file2")
                            for (childNode in children2 ?: emptyList()) {
                                val path0 = decodePath(guid, childNode, project)
                                val path1 = transformToGameObject[path0] ?: path0
                                addPrefabChild(prefab2, path1)
                            }
                        }
                    }
                }
                "GameObject" -> {
                    val prefab2 = node["PrefabParentObject"]
                    val prefabPath = decodePath(guid, prefab2, project)
                    if (prefabPath != InvalidRef) {
                        prefab.prefab = prefabPath
                    }
                    // find transform, which belongs to this GameObject
                    val key = decodePath(guid, fileId, project)
                    val transform = transformsByGameObject[key]
                    if (transform != null) {
                        // if not active, then disable the entity prefab
                        val isActive = node.getBool("IsActive")
                        if (isActive == false) prefab.setProperty("isEnabled", false)
                        applyTransformOnPrefab(prefab, transform, guid, project)
                        // find all components, and add them
                        /**
                        m_Component:
                        - component: {fileID: 4249636020427460}
                        - component: {fileID: 33405807629220246}
                        - component: {fileID: 23400600892312946}
                        - component: {fileID: 64583973720899574}
                         * */
                    } else LOGGER.warn("GameObject $guid/$fileId has no transform")
                    val components = node["Component"]
                    if (components != null) {
                        components.children?.forEach { listNode ->
                            val path = decodePath(guid, listNode.children!!.first().value, project)
                            if (path is PrefabReadable) {
                                val child = path.readPrefab()
                                val type2 = child.getProperty("description") as? String
                                if (type2 != "Transform") {
                                    addPrefabChild(prefab, path)
                                }
                            }
                        }
                    }
                }
            }
        }

        // find root, and create Scene.json-link with that
        var hasFoundRoot = false
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
        }

        /*forAllUnityObjects(root) { fileId, _ ->
            val file = folder.get(fileId) as InnerFile
            file.lastModified++
        }*/

        return folder

        /*
        --- !u!4 &441804715 stripped
        Transform:
          m_PrefabParentObject: {fileID: 4774022774639150, guid: f685963809caafb42bf2ef0c3d5fec4e,
            type: 2}
          m_PrefabInternal: {fileID: 441804714}
        * */

        /*
        --- !u!4 &752095192
        Transform:
          m_ObjectHideFlags: 0
          m_PrefabParentObject: {fileID: 0}
          m_PrefabInternal: {fileID: 0}
          m_GameObject: {fileID: 752095191}
          m_LocalRotation: {x: 0, y: 0, z: 0, w: 1}
          m_LocalPosition: {x: -0.066163585, y: 5.009288, z: 15.072274}
          m_LocalScale: {x: 1, y: 1, z: 1}
          m_Children:
          - {fileID: 1958462913}
          - {fileID: 1721309181}
          - {fileID: 1492832708}
          - {fileID: 918619053}
          - {fileID: 1111747038}
          - {fileID: 1410615079}
          - {fileID: 1421171474}
          - {fileID: 861958571}
          - {fileID: 1586427708}
          - {fileID: 1067863450}
          - {fileID: 1166300874}
          m_Component:
          - component: {fileID: 4903746094873878}
          - component: {fileID: 33571608055006908}
          - component: {fileID: 23533547921062132}
          - component: {fileID: 65592928011705840}
          ...
          m_Father: {fileID: 0}
          m_RootOrder: 0
          m_LocalEulerAnglesHint: {x: 0, y: 0, z: 0}
        * */

    }

    private fun forAllUnityObjects(node: YAMLNode, callback: (fileId: String, node: YAMLNode) -> Unit) {
        val children = node.children ?: return
        if (!children[0].key.startsWith("%YAML")) {
            throw IllegalArgumentException("Not a unity yaml file")
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
            } else if (descriptor.children != null) {
                // LOGGER.info("$key -> ? (${descriptor.key})")
                callback(zeroAssetName, descriptor)
            }
        }
    }

    fun readAsAsset(file: FileReference): FileReference {
        val project = findUnityProject(file)
        if (project == null) {
            LOGGER.warn("No project found in $file")
            // try to read without project
            val node = parseYAML(file.readText(), true)
            val tmpFolder = InnerFolder(file)
            val objects = readUnityObjects(node, "0", invalidProject, tmpFolder)
            if (objects !== tmpFolder) return InvalidRef
            return objects.listChildren().firstOrNull() ?: InvalidRef
            // return InvalidRef
        }
        val objects = project.getGuidFolder(file)
        val children = objects.listChildren() ?: return InvalidRef
        if (children.size <= 1) return children.firstOrNull() ?: InvalidRef
        val meta = project.getMeta(file)
        // find the main id
        val mainFileId = project.getMainId(meta)
        return if (mainFileId != null) {
            objects.getChild(mainFileId)
        } else {
            // get the object with the lowest id
            children.minByOrNull { it.nameWithoutExtension.toLongOrNull() ?: Long.MAX_VALUE }!!
        }
    }

    fun readAsFolder(file: FileReference): FileReference {

        // val tree = parseYAML(file.readText(), true)
        // LOGGER.info(tree)

        // if it is only one thing, and has no references, we don't need the project...
        // but these materials would be boring anyways ;)

        val project = findUnityProject(file)
        if (project == null) {
            LOGGER.warn("No project found in $file")
            return InvalidRef
        }

        println("returning ${project.getGuidFolder(file)}")
        return project.getGuidFolder(file)

    }

    fun inspectAsset(asset: FileReference) {
        for (file in asset.listChildren()!!.filterIsInstance<InnerLinkFile>().sortedBy { -it.readText().length }) {
            LOGGER.info(file.name + " links to " + file.link)
        }
        for (file in asset.listChildren()!!.filter { it !is InnerLinkFile }.sortedBy { -it.readText().length }) {
            LOGGER.info(file.name + ", " + file.readText().length.toLong().formatFileSize())
            LOGGER.info(JsonFormatter.format(file.readText()))
        }
    }

    fun FileReference.printTree(depth: Int, maxDepth: Int) {
        if (!isHidden) {
            println(Tabs.spaces(depth * 2) + name)
            if (depth + 1 < maxDepth && (if (depth == 0) isSomeKindOfDirectory else isDirectory)) {
                for (child in listChildren() ?: emptyList()) {
                    child.printTree(depth + 1, maxDepth)
                }
            }
        }
    }

    fun testRendering(file: FileReference, size: Int = 512) {
        val prefab = PrefabCache.loadPrefab(file)!!
        println(JsonFormatter.format(prefab.toString()))
        val sample = prefab.createInstance()
        println(sample)
        Thumbs.generateSomething(
            prefab, file,
            desktop.getChild(sample::class.simpleName + ".png"), size
        ) {}
    }

    fun smallRenderTest() {

        val projectPath = getReference(downloads, "up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")
        val colliderComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/64472554668277586.json")
        val meshComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/33053279949580010.json")
        val entityOfComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/1661159153272266.json")

        HiddenOpenGLContext.createOpenGL()

        ShaderLib.init()
        ECSShaderLib.init()

        ECSRegistry.init()

        Thumbs.useCacheFolder = true
        for (file in listOf(meshComponent, colliderComponent, entityOfComponent)) {
            testRendering(file, 512)
        }

        Engine.requestShutdown()

    }

    fun sceneRenderTest() {

        // todo the object with fileId 0 as parent is root
        /*
--- !u!1 &752095191
GameObject:
  m_ObjectHideFlags: 0
  m_PrefabParentObject: {fileID: 0}
  m_PrefabInternal: {fileID: 0}
  serializedVersion: 5
  m_Component:
  - component: {fileID: 752095192}
  m_Layer: 0
  m_Name: Demo_Scene
  m_TagString: Untagged
  m_Icon: {fileID: 0}
  m_NavMeshLayer: 0
  m_StaticEditorFlags: 0
  m_IsActive: 1
--- !u!4 &752095192
Transform:
  m_ObjectHideFlags: 0
  m_PrefabParentObject: {fileID: 0}
  m_PrefabInternal: {fileID: 0}
  m_GameObject: {fileID: 752095191}
  m_LocalRotation: {x: 0, y: 0, z: 0, w: 1}
  m_LocalPosition: {x: -0.066163585, y: 5.009288, z: 15.072274}
  m_LocalScale: {x: 1, y: 1, z: 1}
  m_Children:
  - {fileID: 1958462913}
  - {fileID: 1721309181}
  - {fileID: 1492832708}
  ...
  m_Father: {fileID: 0}
  m_RootOrder: 0
  m_LocalEulerAnglesHint: {x: 0, y: 0, z: 0}
        * */

        val projectPath = getReference(downloads, "up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")
        val scene = projectPath.getChild("Assets/PolygonSciFiCity/Scenes/Demo_TriplanarDirt.unity/2130288114.json")

        HiddenOpenGLContext.createOpenGL()

        ShaderLib.init()
        ECSShaderLib.init()

        ECSRegistry.init()

        Thumbs.useCacheFolder = true
        for (file in listOf(scene)) {
            testRendering(file, 1024)
        }

        Engine.requestShutdown()

    }

    @JvmStatic
    fun main(args: Array<String>) {

        // todo analyse triplanar scene & recreate complete tree structure from it

        /*sceneRenderTest()
        return*/

        val projectPath = getReference(downloads, "up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")

        val file = getReference("E:/Assets/POLYGON_Pirates_Pack_Unity_5_6_0.zip/PolygonPirates/Assets/PolygonPirates/Materials")
        println("file exists? ${file.exists}, children: ${file.listChildren()}")
        val projectPath2 = findUnityProject(file)
        println("project from file? $projectPath2")

        val file2 = file.getParent()!!.getChild("Prefabs/Vehicles/SM_Flag_British_01.prefab")
        println("file2 exists? ${file2.exists}")
        println(PrefabCache.getPrefab(file2))

        return

        /*val colliderComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/64472554668277586.json")
        val meshComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/33053279949580010.json")
        val entityOfComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/1661159153272266.json")

        HiddenOpenGLContext.createOpenGL()*/

        ShaderLib.init()
        ECSShaderLib.init()

        ECSRegistry.init()

        /*Thumbs.useCacheFolder = true
        for (file in listOf(meshComponent, colliderComponent, entityOfComponent)) {
            val prefab = PrefabCache.loadPrefab(file)!!
            println(JsonFormatter.format(prefab.toString()))
            val sample = prefab.createInstance()
            println(sample)
            Thumbs.generateSomething(prefab, file,
                desktop.getChild(sample::class.simpleName + ".png"), 512
            ) {}
        }

        Engine.requestShutdown()

        return*/

        ImageCPUCache.getImage(getReference(projectPath, "Assets/PolygonSciFiCity/Textures/LineTex 4.png"), false)!!
            .write(desktop.getChild("LineTex4.png"))

        // circular sample
        // inspectAsset(getReference(projectPath, "f3ffd5a2a26fdf04e93bfde173e2b50d"))

        // blank sample
        /*val blankSample =
            getReference(projectPath, "Assets/PolygonSciFiCity/Prefabs/Buildings/SM_Building_Shack_01.prefab")
        inspectAsset(blankSample)
        ECSRegistry.initWithGFX(512)
        testEntityMeshFrame(blankSample)
        Engine.shutdown()*/


        val assets = getReference(projectPath, "Assets")
        val main = getReference(assets, "PolygonSciFiCity")

        //parseYAML(getReference(main, "Materials/Alternates/PolygonScifi_03_B.mat").readText())
        //parseYAML(getReference(main, "Scenes/Demo.unity"))

        val project = findUnityProject(assets)!!
        val path = decodePath("", "{fileID: 2800000, guid: fff3796fd3630f64890b09296fcb8f85, type: 3}", project)
        LOGGER.info("Path: $path")
        // correct solution: main, Textures/PolygonSciFiCity_Texture_Normal.png

        parseYAML(getReference(downloads, "up/SM_Prop_DiningTable_01.fbx.meta"))


        // val meshMeta = getReference(main, "Models/Flame_Mesh.fbx")
        val material = getReference(main, "Materials/PolygonSciFi_01_A.mat")
        // val scene = getReference(main, "Scenes/Demo.unity")
        LOGGER.info(readAsAsset(material).readText())

        // val sampleMaterial = getReference(downloads, "up/mat/PolygonScifi_03_A.mat")
        // LOGGER.info(readAsAsset(sampleMaterial).readText())

        // ECSRegistry.initNoGFX()

        testUI {
            object : FileExplorer(getReference(main, "Prefabs/Icons"), style) {

                override fun getRightClickOptions(): List<FileExplorerOption> = emptyList()

                override fun onDoubleClick(file: FileReference) {
                    switchTo(file)
                }

                override fun onPaste(x: Float, y: Float, data: String, type: String) {

                }
            }
        }
    }

    // todo rename doesn't work in zips (shouldn't, but it should show a message or sth like that)
    // todo option to hide .meta files in the file explorer, as it's just distracting

}