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
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.BiMap
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
                        // LOGGER.info("Fastest indexing ever <3")
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
            var endIndex = abs.lastIndex
            while (true) {
                val index = abs.lastIndexOf(key, endIndex)
                // if there are multiple indices, try all, from back first
                if (index > 0) {
                    val file2 = getReference(abs.substring(0, index + key.length - 1))
                    val project = getUnityProject(file2)
                    if (project != null) return project
                    endIndex = min(index - 1, endIndex - 3)// correct???
                } else return null
            }
        }
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
        if (guid == guid0 && fileId == "0$assetExtension") {
            return InvalidRef
        }
        // val base = project.getGuidFolder(guid)
        // val fine = base.getChild(fileId)
        // LOGGER.info("Parsed $guid/$fileId for ids of path, $base, ${base.listChildren()?.map { it.name }}, $fine")
        val guid1 = project.getChild(guid)
        var baseFile = guid1.getChildByNameOrFirst(fileId) ?: InvalidRef
        while (baseFile is InnerLinkFile) baseFile = baseFile.link
        return baseFile
    }

    private fun FileReference.getChildByNameOrFirst(name: String): FileReference? {
        if (name.isBlank2()) return this
        if (name == "0.json") return InvalidRef
        val child = getChild(name)
        if (child != InvalidRef) return child
        val children = if (isSomeKindOfDirectory) listChildren() else emptyList()
        if (children?.size == 1 && name.length == "4300000.json".length && name.startsWith("43000") && name.endsWith(".json")) {
            val meshes = children.first().getChild("Meshes").listChildren()
            if (meshes != null && meshes.size > 1) {
                val id = name.substring(0, name.length - 5).toInt() - 4300000
                // find submesh
                // todo find mesh by id... who defines the order?
                // for now just return the n-th child...
                val subMesh = meshes.getOrNull(id)
                if (subMesh != null) {
                    LOGGER.info("$name was missing from mesh file, choose ${subMesh.nameWithoutExtension} based on $id from ${meshes.map { it.nameWithoutExtension }}")
                    return subMesh
                } else LOGGER.warn("Submesh $id could not be found out of ${meshes.size}, from ${meshes.map { it.nameWithoutExtension }}")
            } else if (meshes?.size == 1) {
                return meshes.first()
            } else {
                LOGGER.warn("Could not find submeshes in $this")
            }
        }
        val newChild = if (children != null && children.isNotEmpty()) {
            getChildOrNull("100100000.json") ?: getChildOrNull("Scene.json") ?: children.first()
        } else null
        if (name != "4300000.json" && name != "2800000.json") {
            // 4300000 is a magic for meshes,
            // 2800000 for textures,
            // 0 idk...
            // our logic is fine: Scene.json is being extracted here
            LOGGER.warn("$name is missing from $this, chose ${newChild?.name}, only found ${children?.map { it.name }}")
        }
        return newChild
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

        if (localPos != null) prefab.setProperty("position", localPos)
        if (localRot != null) prefab.setProperty("rotation", localRot)
        if (localSca != null) prefab.setProperty("scale", localSca)

        // these changes are properties of the "Prefab" node, not the transform...
        val mods = node["Modification"]
        val prefabParent = decodePath(guid, mods?.get("TransformParent"), project)
        if (prefabParent != InvalidRef) {
            // to do set the parent if not already done
            // todo mark as used
            LOGGER.debug("Adding Transform from Prefab node, $file")
            knownChildren.add(file)
            addPrefabChild((prefabParent as PrefabReadable).readPrefab(), file)
        }

        val changes = mods?.get("Modifications")?.children

        if (changes != null) {

            val position = JomlPools.vec3d.create().set(0.0)
            val scale = JomlPools.vec3d.create().set(1.0)
            val rotation = JomlPools.quat4d.create().identity()

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

            }

            if (position.lengthSquared() != 0.0) {
                prefab.setProperty("position", Vector3d(position))
            }

            if (scale.distanceSquared(1.0, 1.0, 1.0) > 1e-7) {
                prefab.setProperty("scale", Vector3d(scale))
            }

            if (abs(rotation.w - 1.0) > 1e-7) {
                prefab.setProperty("rotation", Quaterniond(rotation))
            }

            println("pos rot sca: $position, $rotation, $scale by $changes")

            JomlPools.vec3d.sub(2)
            JomlPools.quat4d.sub(1)

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
        if (isConvex == false) prefab[ROOT_PATH, "isConvex"] = isConvex
        // todo is the mesh correct? from what i've found, it was just empty...
        val mesh = decodePath(guid, node["Mesh"], project)
        if (mesh != InvalidRef) prefab[ROOT_PATH, "meshFile"] = mesh
    }

    fun defineMeshFilter(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject): FileReference {
        // later add a Unity-MeshRenderer or similar for the list of materials
        prefab.clazzName = "MeshComponent"
        prefab.setProperty("mesh", decodePath(guid, node["Mesh"], project))
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
                        // x is mirrored (in Synty's collision models), why ever...
                        positions[indexOut + 0] = -data.getFloat(indexIn)
                        positions[indexOut + 1] = +data.getFloat(indexIn + 4)
                        positions[indexOut + 2] = +data.getFloat(indexIn + 8)
                    }
                    prefab.setProperty("positions", positions)
                }
            }
        }
    }

    private fun addPrefabChild(prefab: Prefab, path: FileReference) {
        if (path is PrefabReadable) {
            val child = path.readPrefab()
            val type = if (child.clazzName == "Entity") 'e' else 'c'
            val unityFileId = path.nameWithoutExtension // collisions should be rare
            // to do if collision happens, use index as fileId
            prefab.add(ROOT_PATH, type, child.clazzName, unityFileId, path)
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
                prefab.setProperty("name", node.key)
                file
            }
        }

        val sceneFile = folder.getOrPut("Scene.json") {
            val prefab = Prefab("Entity")
            val file = InnerPrefabFile(folder.absolutePath + "/Scene.json", "Scene.json", folder, prefab)
            prefab.source = file
            file
        }

        val meshesByGameObject = HashMap<FileReference, ArrayList<Prefab>>()
        val transformToGameObject = BiMap<FileReference, FileReference>()
        val knownChildren = HashSet<FileReference>()

        // todo use Transforms instead of GameObjects for everything?
        // parse all instances roughly, except relations
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            // LOGGER.info("rough parsing $fileId -> ${node.key}")
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
                    // println("transform $fileId has game object $gameObject, ${gameObject is PrefabReadable}")
                }
                //

                // to do supports tags(?)
                "OcclusionCullingSettings", "RenderSettings",
                "LightmapSettings", "NavMeshSettings",
                "Behaviour", "Animator", "AudioListener" -> {
                    // not used, and no use as assets
                    file.hide()
                }
                /*
                * --- !u!20 &572435603
                Camera: to do all simple camera properties
                  m_ObjectHideFlags: 0
                  m_PrefabParentObject: {fileID: 0}
                  m_PrefabInternal: {fileID: 0}
                  m_GameObject: {fileID: 572435598}
                  m_Enabled: 1
                  serializedVersion: 2
                  m_ClearFlags: 1
                  m_BackGroundColor: {r: 0.19215687, g: 0.3019608, b: 0.4745098, a: 0}
                  m_NormalizedViewPortRect:
                    serializedVersion: 2
                    x: 0
                    y: 0
                    width: 1
                    height: 1
                  near clip plane: 0.3
                  far clip plane: 1000
                  field of view: 41.8
                  orthographic: 0
                  orthographic size: 5
                  m_Depth: -1
                  m_CullingMask:
                    serializedVersion: 2
                    m_Bits: 4294967295
                  m_RenderingPath: -1
                  m_TargetTexture: {fileID: 0}
                  m_TargetDisplay: 0
                  m_TargetEye: 3
                  m_HDR: 1
                  m_AllowMSAA: 1
                  m_ForceIntoRT: 0
                  m_OcclusionCulling: 1
                  m_StereoConvergence: 10
                  m_StereoSeparation: 0.022
                  m_StereoMirrorMode: 0
                * */
                "Camera" -> {
                    // todo are those properties with the spaces correct?
                    prefab.clazzName = "Camera"
                    val near = node["Near Clip Plane"]?.getFloat()
                    if (near != null) prefab.setProperty("near", near)
                    val far = node["Far Clip Plane"]?.getFloat()
                    if (far != null) prefab.setProperty("far", far)
                    if (node["Orthographic"]?.getBool() == true) {
                        prefab.setProperty("isPerspective", false)
                    }
                    val fov = node["Field Of View"]?.getFloat()
                    if (fov != null) {
                        prefab.setProperty("fovY", fov)
                    }
                    val fov2 = node["Orthographic Size"]?.getFloat()
                    if (fov2 != null) {
                        prefab.setProperty("fovOrthographic", fov2)
                    }
                }
                /* to do support lights
                --- !u!108 &327501804
                Light:
                  m_ObjectHideFlags: 0
                  m_PrefabParentObject: {fileID: 0}
                  m_PrefabInternal: {fileID: 0}
                  m_GameObject: {fileID: 327501803}
                  m_Enabled: 1
                  serializedVersion: 8
                  m_Type: 1
                  m_Color: {r: 1, g: 1, b: 1, a: 1}
                  m_Intensity: 1
                  m_Range: 10
                  m_SpotAngle: 30
                  m_CookieSize: 10
                  m_Shadows:
                    m_Type: 2
                    m_Resolution: -1
                    m_CustomResolution: -1
                    m_Strength: 1
                    m_Bias: 0.05
                    m_NormalBias: 0.4
                    m_NearPlane: 0.2
                  m_Cookie: {fileID: 0}
                  m_DrawHalo: 0
                  m_Flare: {fileID: 0}
                  m_RenderMode: 0
                  m_CullingMask:
                    serializedVersion: 2
                    m_Bits: 4294967295
                  m_Lightmapping: 4
                  m_AreaSize: {x: 1, y: 1}
                  m_BounceIntensity: 1
                  m_FalloffTable:
                    m_Table[0]: 0
                    m_Table[1]: 0
                    m_Table[2]: 0
                    m_Table[3]: 0
                    m_Table[4]: 0
                    m_Table[5]: 0
                    m_Table[6]: 0
                    m_Table[7]: 0
                    m_Table[8]: 0
                    m_Table[9]: 0
                    m_Table[10]: 0
                    m_Table[11]: 0
                    m_Table[12]: 0
                  m_ColorTemperature: 6570
                  m_UseColorTemperature: 0
                  m_ShadowRadius: 0
                  m_ShadowAngle: 0
                * */
                "Light" -> {
                    // to do copy shadow map settings, angle and such
                    val color = node["Color"]?.getColorAsVector3f() ?: Vector3f(1f)
                    // todo find out all light types
                    // 1 = directional light
                    prefab.clazzName = "DirectionalLight"
                    prefab.setProperty("color", color)
                }
                "Material" -> defineMaterial(prefab, node, guid, project)
                "BoxCollider" -> defineBoxCollider(prefab, node)
                "Mesh" -> defineMesh(prefab, node, guid, project)
                "MeshCollider" -> defineMeshCollider(prefab, node, guid, project)
                "MeshFilter" -> {
                    file.hide()
                    val gameObjectKey = defineMeshFilter(prefab, node, guid, project)
                    meshesByGameObject.getOrPut(gameObjectKey) { ArrayList() }.add(prefab)
                }
                "MonoBehaviour" -> {
                    // an unknown script plus properties
                    // in the original without _ and without m_
                    val script = decodePath(guid, node["Script"], project)
                    if (script != InvalidRef) LOGGER.debug(
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
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            when (node.key) {
                "MeshRenderer" -> {
                    file.hide()
                    val gameObjectKey = decodePath(guid, node["GameObject"], project)
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
                        /*
                          - {fileID: 861958571}
                          - {fileID: 1586427708}
                          - {fileID: 1067863450}
                          - {fileID: 1166300874}
                        * */
                        val children2 = children.packListEntries().children
                        if (children2 != null) {
                            // LOGGER.info("processing ${children2.size} children from prefab")
                            for (childNode in children2) {
                                val childPath = decodePath(guid, childNode, project)
                                addPrefabChild(prefab, childPath)
                                knownChildren.add(childPath)
                            }
                        }
                    }
                    /*val rootGameObject = node["RootGameObject"]
                    if (rootGameObject != null) {
                        val link = decodePath(guid, rootGameObject, project)
                        LOGGER.info("[540, Prefab.RootGameObject] Set root object of $fileId to $link")
                        prefab.prefab = link
                    }*/
                    val base = node["PrefabParent"]
                    if (base != null) {
                        val link = decodePath(guid, base, project)
                        LOGGER.info("[540, Prefab.PrefabParent] Set root object of $fileId to $link")
                        prefab.prefab = link
                    }
                }
                "Transform" -> {
                    val prefab2 = node["PrefabParentObject"]
                    val prefabPath = decodePath(guid, prefab2, project)
                    if (prefabPath != InvalidRef) {
                        LOGGER.info("Setting $file.prefab = $prefabPath")
                        prefab.prefab = prefabPath
                    }
                    val children = node["Children"]
                    if (children != null) {
                        /*
                         - {fileID: 861958571}
                         - {fileID: 1586427708}
                         - {fileID: 1067863450}
                         - {fileID: 1166300874}
                        */
                        val children2 = children.packListEntries().children
                        LOGGER.info("processing ${children2?.size} children from transform, adding to $file")
                        for (childNode in children2 ?: emptyList()) {
                            val path0 = decodePath(guid, childNode, project)
                            val path1 = transformToGameObject.reverse[path0] ?: path0
                            addPrefabChild(prefab, path1)
                            knownChildren.add(path1)
                        }
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
                    // println("looking up gameObject $fileId.transform with $file")
                    val transform = transformToGameObject.reverse[file]
                    val components = node["Component"]
                    val prefab3 = (transform as? PrefabReadable)?.readPrefab()
                    if (components != null && prefab3 != null) {
                        components.children?.forEach { listNode ->
                            val childPath = decodePath(guid, listNode.children!!.first().value, project)
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
                        if (isActive == false) prefab.setProperty("isEnabled", false)
                        val name = node["Name"]?.value
                        if (name != null) {
                            prefab3.setProperty("name", name)
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

        // apply write-protection to all internal prefabs,
        // because we cannot save changes to them
        forAllUnityObjects(root) { fileId, _ ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            prefab.sealFromModifications()
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
            LOGGER.warn("Not a unity yaml file")
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
        }
        val objects = project.getGuidFolder(file)
        val scene = objects.getChild("Scene.json")
        if (scene != InvalidRef) return scene
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

    fun testRendering(file: FileReference, size: Int = 512, index: Int) {
        val prefab = PrefabCache[file]!!
        println(JsonFormatter.format(prefab.toString()))
        val sample = prefab.createInstance()
        println(sample)
        Thumbs.generateSomething(
            prefab, file,
            desktop.getChild("$index.png"), size
        ) {}
    }

    fun smallRenderTest() {

        val projectPath = getReference(downloads, "up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")
        val colliderComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/64472554668277586.json")
        val meshComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/33053279949580010.json")
        val entityOfComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/1661159153272266.json")
        val bbox =
            getReference("E:/Assets/Polygon_Street_Racer_Unity_Package_2018_4_Update_01.unitypackage/Assets/PolygonStreetRacer/Prefabs/Generic/SM_Generic_TreeStump_01.prefab")

        HiddenOpenGLContext.createOpenGL()

        ShaderLib.init()
        ECSShaderLib.init()

        ECSRegistry.init()

        Thumbs.useCacheFolder = true
        for ((index, file) in listOf(meshComponent, colliderComponent, entityOfComponent, bbox).withIndex()) {
            testRendering(file, 512, index)
        }

        Engine.requestShutdown()

    }

    fun sceneRenderTest() {

        // todo analyse triplanar scene & recreate complete tree structure from it
        // todo -> the object with fileId 0 as parent is root

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
        for ((index, file) in listOf(scene).withIndex()) {
            testRendering(file, 1024, index)
        }

        Engine.requestShutdown()

    }

    @JvmStatic
    fun main(args: Array<String>) {

        Prefab.maxPrefabDepth = 7

        // to do support for submeshes:
        // MeshFilters reference submeshes by changing the fileId
        // 4300002 is the 2nd (probably) submesh

        /*sceneRenderTest()
        return*/

        val projectPath = getReference(downloads, "up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")

        /*
        return*/

        /*val file =
            getReference("E:/Assets/POLYGON_Pirates_Pack_Unity_5_6_0.zip/PolygonPirates/Assets/PolygonPirates/Materials")
        println("file exists? ${file.exists}, children: ${file.listChildren()}")
        val projectPath2 = findUnityProject(file)
        println("project from file? $projectPath2")*/

        /*val file2 = file.getParent()!!.getChild("Prefabs/Vehicles/SM_Flag_British_01.prefab")
        println("file2 exists? ${file2.exists}")
        println(PrefabCache.getPrefab(file2))*/

        // return

        /*val colliderComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/64472554668277586.json")
        val meshComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/33053279949580010.json")
        val entityOfComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/1661159153272266.json")

        HiddenOpenGLContext.createOpenGL()*/

        ShaderLib.init()
        ECSShaderLib.init()

        ECSRegistry.init()

        /*val circularDependencies = listOf(
            "6e7e49849c96318418dbd28b88bc6d06/100100000.json",
            "cae9881699f289945baf66e9c9958a45/100100000.json",
            "9baabcdff9f934e4f93321577d7858e5/1637145686889916.json",
            "6924b6055d3b89c49be5c9d309e8e14c/100100000.json",
            "32713ca9df7701740ab3e8677019c63a/1656211306410468.json",
            "32713ca9df7701740ab3e8677019c63a/1656211306410468.json"
        )
        for(sample in circularDependencies){
            val prefab = PrefabCache.getPrefab(getReference(projectPath, sample))
            LOGGER.info(sample)
            LOGGER.info(JsonFormatter.format(prefab.toString()))
            LOGGER.info(prefab!!.getSampleInstance())
        }*/

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

        // val project = findUnityProject(assets)!!
        // val path = decodePath("", "{fileID: 2800000, guid: fff3796fd3630f64890b09296fcb8f85, type: 3}", project)
        // LOGGER.info("Path: $path")
        // correct solution: main, Textures/PolygonSciFiCity_Texture_Normal.png

        // parseYAML(getReference(downloads, "up/SM_Prop_DiningTable_01.fbx.meta"))


        // val meshMeta = getReference(main, "Models/Flame_Mesh.fbx")
        // val material = getReference(main, "Materials/PolygonSciFi_01_A.mat")
        // val scene = getReference(main, "Scenes/Demo.unity")
        // LOGGER.info(readAsAsset(material).readText())

        // val sampleMaterial = getReference(downloads, "up/mat/PolygonScifi_03_A.mat")
        // LOGGER.info(readAsAsset(sampleMaterial).readText())

        // ECSRegistry.initNoGFX()

        val testScene = getReference(main, "Scenes/Demo_TriplanarDirt.unity")
        for (fileName in listOf("2130288114", "668974552")) {
            val file = getReference(testScene, "$fileName.json")
            println("$fileName: " + PrefabCache.printDependencyGraph(file))
        }
        //Engine.requestShutdown()
        //return

        testUI {
            object : FileExplorer(testScene, style) {

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