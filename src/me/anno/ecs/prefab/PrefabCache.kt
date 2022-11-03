package me.anno.ecs.prefab

import me.anno.cache.CacheSection
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabByFileCache.Companion.ensureClasses
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ScenePrefab
import me.anno.io.ISaveable
import me.anno.io.base.InvalidClassException
import me.anno.io.base.InvalidFormatException
import me.anno.io.base.UnknownClassException
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.unity.UnityReader
import me.anno.io.zip.InnerLinkFile
import me.anno.io.zip.InnerFolderCache
import me.anno.studio.StudioBase
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager
import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate")
object PrefabCache : CacheSection("Prefab") {

    var printJsonErrors = true
    var prefabTimeout = 60_000L

    private val LOGGER = LogManager.getLogger(PrefabCache::class)

    operator fun get(resource: FileReference?, async: Boolean) =
        getPrefabPair(resource, maxPrefabDepth, async)?.prefab

    operator fun get(resource: FileReference?, depth: Int = maxPrefabDepth, async: Boolean = false) =
        getPrefabPair(resource, depth, async)?.prefab

    fun getPrefabInstance(resource: FileReference?, async: Boolean) =
        getPrefabInstance(resource, maxPrefabDepth, async)

    fun getPrefabInstance(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        async: Boolean = false
    ): ISaveable? {
        val pair = getPrefabPair(resource, depth, async) ?: return null
        return pair.instance ?: try {
            pair.prefab?.getSampleInstance(depth)
        } catch (e: UnknownClassException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
        // to do here is some kind of race condition taking place
        // without this println, or Thread.sleep(),
        // prefabs extending ScenePrefab will not produce correct instances
        // LOGGER.info("Creating instance from thread ${Thread.currentThread().name}, from '${prefab?.source}', ${prefab?.adds?.size} adds + ${prefab?.sets?.size}")
        // Thread.sleep(10)
        val instance = createSuperInstance(superPrefab, depth, clazz)
        instance.changePaths(prefab, Path.ROOT_PATH)
        if (adds != null) for (index in adds.indices) {
            val add = adds[index]
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
        val instance = PrefabCache[prefab, depth1]?.createInstance(depth1) ?: ISaveable.create(clazz) as PrefabSaveable
        instance.prefabPath = Path.ROOT_PATH
        return instance
    }

    fun loadScenePrefab(file: FileReference): Prefab {
        val prefab = this[file, maxPrefabDepth] ?: Prefab("Entity").apply { this.prefab = ScenePrefab }
        prefab.source = file
        if (!file.exists) file.writeText(TextWriter.toText(prefab, StudioBase.workspace))
        return prefab
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

    private fun loadPrefab4(file: FileReference, callback: (ISaveable?, Exception?) -> Unit) {
        if (file is PrefabReadable) {
            callback(file.readPrefab(), null)
            return
        }
        Signature.findName(file) { signature ->
            when (signature) {
                "json" -> {
                    file.inputStream { it, e ->
                        if (it != null) {
                            val prefab = TextReader.read(file, StudioBase.workspace, false).firstOrNull()
                            if (prefab is Prefab) prefab.source = file
                            if (prefab != null) {
                                callback(prefab, null)
                            } else {
                                loadPrefab4i(file, callback)
                            }
                        } else {
                            when (e) {
                                is UnknownClassException -> LOGGER.warn("$e by $file", e)
                                is InvalidFormatException -> if (printJsonErrors && file.lcExtension == "json")
                                    LOGGER.warn("$e by $file", e)
                                else -> e?.printStackTrace() // may be interesting
                            }
                            loadPrefab4i(file, callback)
                        }
                    }
                }
                "yaml" -> {
                    try { // todo support async (?)
                        val prefab = loadUnityFile(file)
                        if (prefab is Prefab) prefab.source = file
                        if (prefab != null) {
                            callback(prefab, null)
                        }
                        loadPrefab4i(file, callback)
                    } catch (e: Exception) {
                        LOGGER.warn("$file is yaml, but not from Unity")
                        e.printStackTrace()
                        loadPrefab4i(file, callback)
                    }
                }
                else -> loadPrefab4i(file, callback)
            }
        }
    }

    private fun loadPrefab4i(file: FileReference, callback: (ISaveable?, Exception?) -> Unit) {
        val folder = InnerFolderCache.readAsFolder(file, false)
        if (folder != null) {
            val scene = folder.getChild("Scene.json") as? PrefabReadable
            val scene2 = scene ?: folder.listChildren()?.firstInstanceOrNull<PrefabReadable>()
            if (scene2 != null) {
                val prefab = scene2.readPrefab()
                prefab.source = file
                callback(prefab, null)
            } else callback(null, null)
        } else callback(null, null)
    }

    var debugLoading = false
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
                    if (debugLoading) LOGGER.info("loading $file")
                    ensureClasses()
                    val data = FileReadPrefabData(file)
                    loadPrefab4(file) { loaded, e ->
                        data.value = loaded
                        if (loaded != null) FileWatch.addWatchDog(file)
                        if (debugLoading) LOGGER.info("loaded $file, got ${loaded?.className}")
                        e?.printStackTrace()
                    }
                    data
                }
                if (entry is FileReadPrefabData && entry.hasValue && entry.value == null)
                    throw IOException("Could not load $resource as prefab")
                return entry as? FileReadPrefabData
            }
            else -> null
        }
    }

}