package me.anno.io.unity

import me.anno.cache.CacheData
import me.anno.ecs.prefab.Prefab
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.io.unity.UnityProject.Companion.invalidProject
import me.anno.io.unity.UnityProject.Companion.isValidUUID
import me.anno.io.yaml.YAMLNode
import me.anno.io.yaml.YAMLReader.beautify
import me.anno.io.yaml.YAMLReader.parseYAML
import me.anno.io.yaml.YAMLReader.parseYAMLxJSON
import me.anno.io.zip.InnerByteFile
import me.anno.io.zip.InnerFile
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerLinkFile
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f

object UnityReader {

    private val LOGGER = LogManager.getLogger(UnityReader::class)

    private const val unityProjectTimeout = 300_000L // 5 min

    val assetExtension = ".json"
    val zeroAssetName = "0$assetExtension"

    fun getUnityProject(file: FileReference, async: Boolean = false): UnityProject? {
        if (file.isDirectory) {
            val children = file.listChildren() ?: return null
            if (children.any { it.extension == "meta" }) {
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
            val index = abs.indexOf(key)
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

    fun readUnityObject(node: YAMLNode, guid: String, fileId: String, project: UnityProject): Prefab? {
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
                        "RootOrder" -> {} // idk...
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
            else -> {
                LOGGER.warn("todo: read $clazz/$guid/$fileId")
            }
        }
        return null
    }

    fun readUnityObjects(root: YAMLNode, guid: String, project: UnityProject, folder: InnerFolder): FileReference {

        if (!isValidUUID(guid)) return InvalidRef
        root.children ?: return InvalidRef

        // first the members all need to be registered as files,
        forAllUnityObjects(root) { fileId, _ ->
            folder.getOrPut(fileId) {
                InnerByteFile(folder, fileId, byteArrayOf())
            }
        }

        // then they can get decoded and written to the files
        forAllUnityObjects(root) { fileId, node ->
            // write the prefab result to the file, in binary or text
            val prefab = readUnityObject(node, guid, fileId, project)
            if (prefab != null) {
                val file = folder.getChild(fileId) as InnerFile
                val data = TextWriter.toText(prefab, false).toByteArray()
                file.data = data
                val size = data.size.toLong()
                file.size = size
                file.compressedSize = size
            }
        }

        return folder

    }

    fun forAllUnityObjects(node: YAMLNode, callback: (fileId: String, node: YAMLNode) -> Unit) {
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
            children.minByOrNull { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }!!
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

        LOGGER.info(readAsAsset(getReference(OS.downloads, "up/PolygonScifi_03_A.mat")))

        val meshMeta = getReference(main, "Models/Flame_Mesh.fbx")
        val material = getReference(main, "Materials/PolygonSciFi_01_A.mat")
        val scene = getReference(main, "Scenes/Demo.unity")
        readAsAsset(material)
        /*readAsAsset(meshMeta)
        readAsAsset(scene)*/
    }

    // todo rename doesn't work in zips (shouldn't, but it should show a message or sth like that)
    // todo option to hide .meta files in the file explorer, as it's just distracting

}