package me.anno.ecs.prefab

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable
import me.anno.io.base.InvalidClassException
import me.anno.io.base.InvalidFormatException
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.unity.UnityReader
import me.anno.io.zip.InnerLinkFile
import me.anno.io.zip.InnerPrefabFile
import me.anno.io.zip.ZipCache
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.obj.OBJReader2
import me.anno.mesh.vox.VOXReader
import me.anno.studio.StudioBase
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@Suppress("unused", "MemberVisibilityCanBePrivate")
object PrefabCache : CacheSection("Prefab") {

    var printJsonErrors = true
    var prefabTimeout = 60_000L

    private val LOGGER = LogManager.getLogger(PrefabCache::class)

    operator fun get(resource: FileReference?, depth: Int = maxPrefabDepth, async: Boolean = false) =
        getPrefabPair(resource, depth, async)?.prefab

    private fun loadAssimpModel(resource: FileReference): Prefab? {
        return try {
            val reader = AnimatedMeshesLoader
            val meshes = reader.readAsFolder2(resource)
            meshes.second
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadVOXModel(resource: FileReference): Prefab? {
        return try {
            VOXReader().read(resource).toEntityPrefab(resource)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadObjModel(resource: FileReference): Prefab? {
        return try {
            OBJReader2(resource).scenePrefab
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadBlenderModel(resource: FileReference): Prefab? {
        return try {
            val folder = BlenderReader.readAsFolder(resource)
            val sceneFile = folder.getChild("Scene.json") as InnerPrefabFile
            return sceneFile.prefab
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadJson(resource: FileReference?): ISaveable? {
        return when (resource) {
            InvalidRef, null -> null
            is PrefabReadable -> resource.readPrefab()
            else -> {
                try {
                    val read = TextReader.read(resource, StudioBase.workspace, true)
                    val prefab = read.firstOrNull()
                    if (prefab == null) LOGGER.warn("No Prefab found in $resource:${resource::class.simpleName}! $read")
                    // else LOGGER.info("Read ${prefab.changes?.size} changes from $resource")
                    (prefab as? Prefab)?.wasCreatedFromJson = true
                    prefab
                } catch (e: Exception) {
                    LOGGER.warn("$e by $resource")
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    private fun loadUnityFile(resource: FileReference): Prefab? {
        return loadJson(UnityReader.readAsAsset(resource)) as? Prefab
    }

    private fun loadPrefab2(resource: FileReference): ISaveable? {
        return if (resource.exists && !resource.isDirectory) {
            // LOGGER.info("resource $resource has signature $signature")
            val prefab = when (Signature.findName(resource)) {
                "vox" -> loadVOXModel(resource)
                "fbx", "gltf", "md2", "md5mesh" ->
                    loadAssimpModel(resource)
                "blend" -> loadBlenderModel(resource)
                "obj" -> loadObjModel(resource)
                "yaml" -> loadUnityFile(resource)
                "json" -> {
                    // could be gltf as well
                    when (resource.lcExtension) {
                        "gltf", "glb" -> loadAssimpModel(resource)
                        else -> loadJson(resource)
                    }
                }
                else -> {
                    when (resource.lcExtension) {
                        "vox" -> loadVOXModel(resource)
                        "fbx", "dae", "gltf", "glb",
                        "md2", "md5mesh" ->
                            loadAssimpModel(resource)
                        "blend" -> loadBlenderModel(resource)
                        "obj" -> loadObjModel(resource)
                        in UnityReader.unityExtensions -> loadUnityFile(resource)
                        "mtl" -> null
                        else -> loadJson(resource)
                    }
                }
            }
            (prefab as? Prefab)?.source = resource
            prefab
        } else null
    }

    private fun loadPrefab3(file: FileReference): ISaveable? {
        if (file is PrefabReadable) return file.readPrefab()
        when (Signature.findName(file)) {
            "json" ->
                try {
                    val prefab = TextReader.read(file, StudioBase.workspace, false).firstOrNull()
                    if (prefab is Prefab) prefab.source = file
                    if (prefab != null) return prefab
                } catch (e: InvalidFormatException) {
                    if (printJsonErrors) LOGGER.warn("$e by $file", e)
                    // don't care
                } catch (e: Exception) {
                    // might be interesting
                    e.printStackTrace()
                }
            "yaml" -> try {
                val prefab = loadUnityFile(file)
                if (prefab is Prefab) prefab.source = file
                if (prefab != null) return prefab
            } catch (e: Exception) {
                LOGGER.warn("$file is yaml, but not from Unity")
                e.printStackTrace()
            }
        }
        val folder = ZipCache.unzip(file, false) ?: return null
        val scene = folder.getChild("Scene.json") as? PrefabReadable
        val scene2 = scene ?: folder.listChildren()?.firstInstanceOrNull<PrefabReadable>() ?: return null
        val prefab = scene2.readPrefab()
        prefab.source = file
        return prefab
    }

    fun getPrefabInstance(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        async: Boolean = false
    ): ISaveable? {
        val pair = getPrefabPair(resource, depth, async) ?: return null
        return pair.instance ?: pair.prefab?.getSampleInstance(depth)
    }

    private fun getPrefabPair(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        async: Boolean = false
    ): FileReadPrefabData? {
        // LOGGER.info("get prefab from $resource, ${resource?.exists}, ${resource?.isDirectory}")
        return when {
            resource == null -> null
            resource is InnerLinkFile -> getPrefabPair(resource.link, depth, async)
            resource.exists && !resource.isDirectory -> {
                val entry = getFileEntry(resource, false, prefabTimeout, async) { file, _ ->
                    val loaded = loadPrefab3(file)
                    // LOGGER.info("loaded $file, got ${loaded?.className}")
                    // if (loaded is Prefab) LOGGER.info(loaded)
                    if (loaded != null) {
                        FileWatch.addWatchDog(file)
                        FileReadPrefabData(loaded as? Prefab, if (loaded is Prefab) null else loaded, file)
                    } else CacheData(null)
                }
                if (entry is CacheData<*> && entry.value == null)
                    throw IOException("Could not load $resource as prefab")
                return entry as? FileReadPrefabData
            }
            else -> null
        }
    }

    fun createInstance(
        prefab: Prefab?,
        superPrefab: FileReference,
        adds: List<CAdd>?,
        sets: KeyPairMap<Path, String, Any?>?,
        depth: Int,
        clazz: String
    ): PrefabSaveable {
        // todo here is some kind of race condition taking place
        // without this println, or Thread.sleep(),
        // prefabs extending ScenePrefab will not produce correct instances
        // LOGGER.info("Creating instance from thread ${Thread.currentThread().name}, from '${prefab?.source}', ${prefab?.adds?.size} adds + ${prefab?.sets?.size}")
        Thread.sleep(10)
        val instance = createSuperInstance(superPrefab, depth, clazz)
        instance.changePaths(prefab, Path.ROOT_PATH)
        adds?.forEachIndexed { index, add ->
            try {
                add.apply(instance, depth - 1)
            } catch (e: InvalidClassException) {
                throw e
            } catch (e: Exception) {
                LOGGER.warn("Change $index, $add failed")
                throw e
            }
        }
        sets?.forEach { k1, k2, v ->
            try {
                CSet.apply(instance, k1, k2, v)
            } catch (e: Exception) {
                LOGGER.warn("Change '$k1' '$k2' '$v' failed")
                throw e
            }
        }
        // LOGGER.info("  created instance '${entity.name}' has ${entity.children.size} children and ${entity.components.size} components")
        return instance
    }

    fun createInstance(
        superPrefab: FileReference,
        adds: List<CAdd>?,
        sets: List<CSet>?,
        depth: Int,
        clazz: String
    ): PrefabSaveable {
        // LOGGER.info("creating instance from $superPrefab")
        val instance = createSuperInstance(superPrefab, depth, clazz)
        // val changes2 = (changes0 ?: emptyList()).groupBy { it.className }.map { "${it.value.size}x ${it.key}" }
        // LOGGER.info("  creating entity instance from ${changes0?.size ?: 0} changes, $changes2")
        if (adds != null) {
            for ((index, change) in adds.withIndex()) {
                try {
                    change.apply(instance, depth - 1)
                } catch (e: Exception) {
                    LOGGER.warn("Change $index, $change failed")
                    throw e
                }
            }
        }
        if (sets != null) {
            for (index in sets.indices) {
                val change = sets[index]
                try {
                    change.apply(instance, depth - 1)
                } catch (e: Exception) {
                    LOGGER.warn("Change $index, $change failed")
                    throw e
                }
            }
        }
        // LOGGER.info("  created instance '${entity.name}' has ${entity.children.size} children and ${entity.components.size} components")
        return instance
    }

    fun printDependencyGraph(prefab: FileReference): String {
        val added = HashSet<FileReference>()
        val todo = ArrayList<FileReference>()
        val connections = HashMap<FileReference, List<FileReference>>()
        added.add(prefab)
        todo.add(prefab)
        while (todo.isNotEmpty()) {
            val next = todo.removeAt(todo.lastIndex)
            val prefab2 = PrefabCache[next]
            if (prefab2 != null) {
                val con = HashSet<FileReference>()
                val s0 = prefab2.prefab
                if (s0 != InvalidRef) {
                    con.add(s0)
                    if (added.add(s0)) todo.add(s0)
                }
                for (add in prefab2.adds) {
                    val s1 = add.prefab
                    if (s1 != InvalidRef) {
                        con.add(s1)
                        if (added.add(s1)) todo.add(s1)
                    }
                }
                if (con.isNotEmpty()) {
                    connections[next] = con.toList()
                }
            }
        }
        // make the graph easily readable
        val nameList = connections.entries
            .sortedByDescending { it.value.size }
        val nameMap = nameList
            .withIndex()
            .associate { it.value.key to it.index }
        // print the graph for debugging
        return "${
            connections.entries
                .associate { nameMap[it.key]!! to it.value.mapNotNull { v -> nameMap[v] }.sorted() }
                .toSortedMap()
        }, ${nameList.map { "${get(it.key)?.get(Path.ROOT_PATH, "name")}" }}, $nameMap"
    }

    private fun createSuperInstance(
        prefab: FileReference,
        depth: Int,
        clazz: String
    ): PrefabSaveable {
        if (depth < 0) {
            LOGGER.warn("Dependency Graph: ${printDependencyGraph(prefab)}")
            throw StackOverflowError("Circular dependency in $prefab")
        }
        val depth1 = depth - 1
        return PrefabCache[prefab, depth1]?.createInstance(depth1) ?: ISaveable.create(clazz) as PrefabSaveable
    }

    fun loadScenePrefab(file: FileReference): Prefab {
        val prefab = this[file, maxPrefabDepth] ?: Prefab("Entity").apply { this.prefab = ScenePrefab }
        prefab.source = file
        if (!file.exists) file.writeText(TextWriter.toText(prefab, StudioBase.workspace))
        return prefab
    }

}