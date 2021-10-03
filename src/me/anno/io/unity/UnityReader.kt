package me.anno.io.unity

import me.anno.cache.CacheData
import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
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
import me.anno.utils.OS
import me.anno.utils.types.Vectors.toVector3d
import org.apache.logging.log4j.LogManager
import org.joml.*
import kotlin.math.abs

object UnityReader {

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
                        // fastest indexing ever <3
                        CacheData(root.project)
                    } else {
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
        val byName = getChild(name)
        if (byName != InvalidRef) return byName
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

    private fun readUnityObject(node: YAMLNode, guid: String, fileId: String, project: UnityProject): Prefab? {
        val clazz = node.key
        if (!isValidUUID(clazz)) throw IllegalArgumentException("Invalid class $clazz")
        if (!isValidUUID(guid)) throw IllegalArgumentException("Invalid guid $guid")
        when (clazz) {
            "Material" -> {
                val prefab = Prefab("Material")
                val name = node["Name"]?.value
                if (name != null) prefab.setProperty("name", name)
                readMaterial(node, guid, prefab, project)
                return prefab
            }
            "Prefab" -> {

                val changes = node["Modification"]?.get("Modifications")?.children ?: emptyList()

                var name = fileId
                val position = Vector3f()
                val scale = Vector3f(1f)
                val rotation = Quaternionf()
                for (change in changes) {

                    val target = decodePath(guid, change["Target"]?.value, project)

                    // e.g. m_LocalPosition.x -> LocalPosition.x,
                    // m_Name -> Name, ...
                    // RootOrder?
                    // LocalRotation.x/y/z/w
                    // LocalEulerAnglesHint.x/y/z?
                    val path = beautify(change["PropertyPath"]!!.value.toString())
                    val value = change["Value"]?.value // e.g. 1.723
                    val objectReference = decodePath(guid, change["ObjectReference"]?.value, project) // ?

                    when (path) {
                        "Name" -> name = value ?: continue
                        // position
                        "LocalPosition.x" -> position.x = value?.toFloatOrNull() ?: continue
                        "LocalPosition.y" -> position.y = value?.toFloatOrNull() ?: continue
                        "LocalPosition.z" -> position.z = value?.toFloatOrNull() ?: continue
                        // rotation
                        "LocalRotation.x" -> rotation.x = value?.toFloatOrNull() ?: continue
                        "LocalRotation.y" -> rotation.y = value?.toFloatOrNull() ?: continue
                        "LocalRotation.z" -> rotation.z = value?.toFloatOrNull() ?: continue
                        "LocalRotation.w" -> rotation.w = value?.toFloatOrNull() ?: continue
                        // scale
                        "LocalScale.x" -> scale.x = value?.toFloatOrNull() ?: continue
                        "LocalScale.y" -> scale.y = value?.toFloatOrNull() ?: continue
                        "LocalScale.z" -> scale.z = value?.toFloatOrNull() ?: continue
                        // mmh... maybe for rotations with large angle?
                        "LocalEulerAnglesHint.x", "LocalEulerAnglesHint.y", "LocalEulerAnglesHint.z" -> {
                        }
                        "RootOrder" -> {
                        } // idk...
                        /*"Materials.Array.data[0]" -> {
                            // todo set the material somehow... is it a material?
                        }*/
                        else -> LOGGER.info("$target, Change: $path, value: $value, ref: $objectReference")
                    }

                }

                /**
                --- !u!4 &3260095 stripped
                Transform:
                m_PrefabParentObject: {fileID: 4001237703748626, guid: 31dd6d847d998664db4d2466ae3ac2b4,
                type: 2}
                m_PrefabInternal: {fileID: 3324434}
                then there is another instance, which seems to have the actual data...
                --- !u!1001 &3324434
                Prefab:
                m_ObjectHideFlags: 0
                serializedVersion: 2
                 * */

            }
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
            "BoxCollider" -> {
                // todo set isTrigger somehow...
                val center = node.getColorAsVector3f("Center")
                val size = node.getColorAsVector3f("Size")
                // todo half the size??
                val isEnabled = node.getBool("Enabled")
                return if (center == null || (abs(center.x) < 1e-5 && abs(center.y) < 1e-5 && abs(center.z) < 1e-5)) {
                    val prefab = Prefab("BoxCollider")
                    if (isEnabled == false) prefab.setProperty("isEnabled", isEnabled)
                    if (size != null) prefab.setProperty("halfExtends", size.toVector3d())
                    prefab
                } else {
                    val prefab = Prefab("Entity")
                    if (isEnabled == false) prefab.setProperty("isEnabled", isEnabled)
                    prefab.setProperty("position", center.toVector3d())
                    val collider = prefab.add(Path.ROOT_PATH, 'c', "BoxCollider", 0)
                    if (size != null) prefab.set(collider, "halfExtends", size.toVector3d())
                    if (center.x != 0f || center.y != 0f || center.z != 0f) {
                        LOGGER.warn("Non-null offset in BoxCollider")
                    }
                    prefab
                }
            }
            /**
             * --- !u!64 &799111319
            MeshCollider:
            m_ObjectHideFlags: 0
            m_PrefabParentObject: {fileID: 0}
            m_PrefabInternal: {fileID: 0}
            m_GameObject: {fileID: 799111318}
            m_Material: {fileID: 0}
            m_IsTrigger: 0
            m_Enabled: 0
            serializedVersion: 2
            m_Convex: 1
            m_InflateMesh: 0
            m_SkinWidth: 0.01
            m_Mesh: {fileID: 43786602751171120, guid: 0c1d93d6dbb32814b886cda43492c302, type: 2}
            --- !u!64 &799111320
             * */
            else -> {
                LOGGER.warn("todo: read $clazz/$guid/$fileId")
            }
        }
        return null
    }

    fun addGameObject2Transform(prefab: Prefab, node: YAMLNode) {

    }

    fun addPrefab2Transform(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject) {

        val changes = node["Modification"]?.get("Modifications")?.children ?: emptyList()

        val position = Vector3d()
        val scale = Vector3d(1.0)
        val rotation = Quaterniond()
        for (change in changes) {

            val target = decodePath(guid, change["Target"]?.value, project)

            // e.g. m_LocalPosition.x -> LocalPosition.x,
            // m_Name -> Name, ...
            // RootOrder?
            // LocalRotation.x/y/z/w
            // LocalEulerAnglesHint.x/y/z?
            val path = beautify(change["PropertyPath"]!!.value.toString())
            val value = change["Value"]?.value // e.g. 1.723
            // a reference to a local object, when dragging sth onto another thing
            // value is then empty
            val objectReference = decodePath(guid, change["ObjectReference"]?.value, project)

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
                /*"Materials.Array.data[0]" -> {
                    // todo set the material somehow... is it a material?
                }*/
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

    fun defineTransform(prefab: Prefab, node: YAMLNode) {

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
        val isTrigger = node.getBool("IsTrigger")
        val isEnabled = node.getBool("Enabled")
        if (isEnabled == false) prefab.setProperty("isEnabled", isEnabled)
        val path = if (center == null) {
            prefab.clazzName = "Entity"
            prefab.set(ROOT_PATH, "position", center)
            prefab.add(ROOT_PATH, 'c', "BoxCollider", 0)
        } else {
            prefab.clazzName = "BoxCollider"
            ROOT_PATH
        }
        if (size != null) prefab.set(path, "halfExtends", size)
        if (isTrigger != null) prefab.set(path, "isTrigger", isTrigger) // mmh...
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
        val isTrigger = node.getBool("IsTrigger")
        val isEnabled = node.getBool("Enabled")
        if (isEnabled == false) prefab.setProperty("isEnabled", isEnabled)
        if (isTrigger != null) prefab.set(ROOT_PATH, "isTrigger", isTrigger) // mmh...
        val isConvex = node.getBool("Convex")
        if (isConvex == false) prefab.set(ROOT_PATH, "isConvex", isConvex)
        val mesh = decodePath(guid, node["Mesh"], project)
        if(mesh != InvalidRef) prefab.set(ROOT_PATH, "mesh", mesh)
    }

    fun defineMeshFilter(prefab: Prefab, node: YAMLNode, guid: String, project: UnityProject): String {
        // later add a Unity-MeshRenderer or similar for the list of materials
        prefab.clazzName = "MeshComponent"
        prefab.setProperty("mesh", decodePath(guid, node["Mesh"], project))
        return node["GameObject"].toString()
    }

    fun readUnityObjects(root: YAMLNode, guid: String, project: UnityProject, folder: InnerFolder): FileReference {

        if (!isValidUUID(guid)) return InvalidRef
        root.children ?: return InvalidRef


        // first the members all need to be registered as files
        forAllUnityObjects(root) { fileId, _ ->
            folder.getOrPut(fileId) {
                val prefab = Prefab("Entity")
                val file = InnerPrefabFile(folder.absolutePath + "/" + fileId, fileId, folder, prefab)
                prefab.source = file
                file
            }
        }

        val meshesByGameObject = HashMap<String, ArrayList<Prefab>>()
        val transformsByGameObject = HashMap<FileReference, Prefab>()
        val prefabsByRef = HashMap<String, YAMLNode>()

        // parse all instances roughly, except relations
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            when (node.key) {
                "Transform" -> {
                    defineTransform(prefab, node)
                    val gameObject = decodePath(guid, node["GameObject"], project)
                    // {fileID: 1420934846718150}
                    if (gameObject != InvalidRef) {
                        transformsByGameObject[gameObject] = prefab
                    }
                    transformsByGameObject[decodePath(guid, fileId, project)] = prefab
                }
                "Prefab" -> prefabsByRef[fileId] = node
                "Material" -> defineMaterial(prefab, node, guid, project)
                "BoxCollider" -> defineBoxCollider(prefab, node)
                "MeshCollider" -> defineMeshCollider(prefab, node, guid, project)
                "MeshFilter" -> {
                    val gameObjectKey = defineMeshFilter(prefab, node, guid, project)
                    meshesByGameObject.getOrPut(gameObjectKey) { ArrayList() }
                        .add(prefab)
                }
            }
        }

        // add their transforms / prefabs / changes
        forAllUnityObjects(root) { fileId, node ->
            val file = folder.get(fileId) as InnerPrefabFile
            val prefab = file.readPrefab()
            when (node.key) {
                "Transform" -> {
                    val prefabRef = decodePath(guid, node["PrefabInternal"], project)
                    if (prefabRef != InvalidRef) {
                        val prefabNode = prefabsByRef[prefabRef.name]
                        if (prefabNode != null) {
                            addPrefab2Transform(prefab, prefabNode, guid, project)
                        } else LOGGER.warn("Missing prefab ${prefabRef.name}, only got ${prefabsByRef.keys}")
                    }
                }
                "MeshRenderer" -> {
                    val gameObjectKey = node["GameObject"].toString()
                    // todo parse list of materials, and assign them to all meshes
                    val castShadows = node.getBool("CastShadows") ?: true
                    val receiveShadows = node.getBool("ReceiveShadows") ?: true
                    val isEnabled = node.getBool("Enabled") ?: true
                    val materials = node["Materials"]
                    for (mesh in meshesByGameObject[gameObjectKey] ?: emptyList()) {
                        if (!isEnabled) mesh.setProperty("isEnabled", false)

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
                        LOGGER.info("todo parse children list, and add them as entities: $children")
                    }
                    val rootGameObject = node["RootGameObject"]
                    if (rootGameObject != null) {
                        // todo replace file with link instead
                        prefab.prefab = transformsByGameObject[decodePath(guid, rootGameObject, project)]?.source
                            ?: InvalidRef
                    }
                }
                "GameObject" -> {
                    // find transform, which belongs to this GameObject
                    val key = decodePath(guid, fileId, project)
                    val transform = transformsByGameObject[key]
                    if (transform != null) {
                        prefab.prefab = transform.source
                        // if not active, then disable the entity prefab
                        val isActive = node.getBool("IsActive")
                        if (isActive == false) transform.setProperty("isEnabled", false)
                        // find all components, and add them
                        /**
                        m_Component:
                        - component: {fileID: 4249636020427460}
                        - component: {fileID: 33405807629220246}
                        - component: {fileID: 23400600892312946}
                        - component: {fileID: 64583973720899574}
                         * */
                        val components = node["Component"]
                        if (components != null) {
                            val paths = components.children?.map {
                                decodePath(guid, it.children!!.first().value, project)
                            } ?: emptyList()
                            for (path in paths) {
                                path as PrefabReadable
                                val child = path.readPrefab()
                                // they may be entities under-cover, so be careful
                                val type = if (child.clazzName == "Entity") 'e' else 'c'
                                val name = child.instanceName ?: child.clazzName
                                // println("would add $name/$path to ${transform.source}")
                                transform.add(ROOT_PATH, type, child.clazzName, name, path)
                            }
                        }
                    } else LOGGER.warn("Missing transform $key, only got ${transformsByGameObject.keys}")
                }
            }
        }

        // todo somehow find global object
        // todo then create Scene.json from that object (LinkNode ofc)

        return folder

        // todo read hierarchy somehow...

        // then they can get decoded and written to the files
        forAllUnityObjects(root) { fileId, node ->
            // write the prefab result to the file, in binary or text
            val prefab = readUnityObject(node, guid, fileId, project)
            if (prefab != null) {
                val file = folder.getChild(fileId) as InnerPrefabFile
                file.prefab = prefab
            }
        }

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

        forAllUnityObjects(root) { fileId, node ->
            if (node.key == "Transform") {
                /*val children = node["Children"]
                if(children != null){
                    throw RuntimeException("found node with children $children")
                }*/
            }
        }

        return folder

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

        return project.getGuidFolder(file)

    }

    @JvmStatic
    fun main(args: Array<String>) {

        val fastProject = getReference(OS.downloads, "up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")
        val assets = getReference(fastProject, "Assets")
        val main = getReference(assets, "PolygonSciFiCity")

        //parseYAML(getReference(main, "Materials/Alternates/PolygonScifi_03_B.mat").readText())
        //parseYAML(getReference(main, "Scenes/Demo.unity"))

        val project = findUnityProject(assets)!!
        val path = decodePath("", "{fileID: 2800000, guid: fff3796fd3630f64890b09296fcb8f85, type: 3}", project)
        LOGGER.info("Path: $path")
        // correct solution: main, Textures/PolygonSciFiCity_Texture_Normal.png

        parseYAML(getReference(OS.downloads, "up/SM_Prop_DiningTable_01.fbx.meta"))

        LOGGER.info(readAsAsset(getReference(OS.downloads, "up/mat/PolygonScifi_03_A.mat")).readText())

        val meshMeta = getReference(main, "Models/Flame_Mesh.fbx")
        val material = getReference(main, "Materials/PolygonSciFi_01_A.mat")
        val scene = getReference(main, "Scenes/Demo.unity")
        LOGGER.info(readAsAsset(material).readText())
        /*readAsAsset(meshMeta)
        readAsAsset(scene)*/

        ECSRegistry.initNoGFX()

        testUI {
            object : FileExplorer(getReference(main, "Prefabs/Buildings/SM_Bld_Advanced_01.prefab"), style) {

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