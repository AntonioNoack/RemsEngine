package me.anno.ecs.prefab

import me.anno.cache.CacheSection
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabByFileCache.Companion.ensureClasses
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
import me.anno.engine.ScenePrefab
import me.anno.io.base.InvalidFormatException
import me.anno.io.base.UnknownClassException
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.InnerFolderCache.imageFormats1
import me.anno.io.files.inner.InnerLinkFile
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.Logging.hash32
import me.anno.utils.async.Callback
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.Recursion
import me.anno.utils.types.Strings.shorten
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

/**
 * use this to get access to Prefabs, so you can create new instances from them, or access their sample instance
 * */
@Suppress("MemberVisibilityCanBePrivate")
object PrefabCache : CacheSection("Prefab") {

    var printJsonErrors = true
    var prefabTimeout = 60_000L

    private val LOGGER = LogManager.getLogger(PrefabCache::class)
    val debugLoading get() = LOGGER.isDebugEnabled()

    operator fun get(resource: FileReference?, async: Boolean): Prefab? =
        pairToPrefab(getPrefabPair(resource, maxPrefabDepth, prefabTimeout, async), async)

    operator fun get(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        timeout: Long = prefabTimeout,
        async: Boolean = false
    ): Prefab? = pairToPrefab(getPrefabPair(resource, depth, timeout, async), async)

    private fun pairToPrefab(pair: FileReadPrefabData?, async: Boolean): Prefab? {
        pair ?: return null
        if (!async) pair.waitFor()
        val prefab = pair.prefab
        if (prefab != null) return prefab
        val instance = pair.instance
        return if (instance is PrefabSaveable) {
            instance.ref
            instance.prefab
        } else null
    }

    fun getPrefabInstance(resource: FileReference?, async: Boolean) = getPrefabInstance(resource, maxPrefabDepth, async)

    fun getPrefabInstance(resource: FileReference?, depth: Int = maxPrefabDepth, async: Boolean = false): Saveable? {
        val pair = getPrefabPair(resource, depth, prefabTimeout, async) ?: return null
        if (!async) pair.waitFor()
        return pair.instance ?: try {
            pair.prefab?.getSampleInstance(depth)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPrefabInstanceAsync(resource: FileReference?, depth: Int = maxPrefabDepth, callback: Callback<Saveable?>) {
        getPrefabPairAsync(resource, { pair, err ->
            if (pair != null) {
                callback.ok(pair.instance ?: pair.prefab?.getSampleInstance(depth))
            } else callback.err(err)
        }, depth, prefabTimeout)
    }

    fun printDependencyGraph(prefab: FileReference): String {
        val connections = HashMap<FileReference, List<FileReference>>()
        Recursion.collectRecursive(prefab) { next, remaining ->
            val prefab2 = PrefabCache[next]
            if (prefab2 != null) {
                val con = HashSet<FileReference>()
                val s0 = prefab2.prefab
                if (s0 != InvalidRef) {
                    con.add(s0)
                    remaining.add(s0)
                }
                for ((_, addsI) in prefab2.adds) {
                    for (add in addsI) {
                        val s1 = add.prefab
                        if (s1 != InvalidRef) {
                            con.add(s1)
                            remaining.add(s1)
                        }
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
                .entries.sortedBy { it.key } // toSortedMap() doesn't exist in Kotlin/JS
                .joinToString { "${it.key}: ${it.value}" }
        }, ${nameList.map { "${get(it.key)?.get(Path.ROOT_PATH, "name")}" }}, $nameMap"
    }

    fun createSuperInstance(prefab: FileReference, depth: Int, clazz: String): PrefabSaveable? {
        if (depth < 0) {
            LOGGER.warn("Circular dependency in $prefab, ${printDependencyGraph(prefab)}")
            return null
        }
        val depth1 = depth - 1
        val instance =
            PrefabCache[prefab, depth1]?.createInstance(depth1)
                ?: Saveable.createOrNull(clazz) as? PrefabSaveable
                ?: return null
        instance.prefabPath = Path.ROOT_PATH
        return instance
    }

    fun loadScenePrefab(file: FileReference): Prefab {
        val prefab = this[file, maxPrefabDepth] ?: Prefab("Entity").apply { this.prefab = ScenePrefab }
        prefab.source = file
        return prefab
    }

    fun loadJson(resource: FileReference?): Saveable? {
        return when (resource) {
            InvalidRef, null -> null
            is PrefabReadable -> resource.readPrefab()
            else -> {
                try {
                    val read = JsonStringReader.read(resource, EngineBase.workspace, true)
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

    var unityReader: ((FileReference, (Saveable?, Exception?) -> Unit) -> Unit)? = null

    private fun loadPrefab4(file: FileReference, callback: (Saveable?, Exception?) -> Unit) {
        if (file is PrefabReadable) {
            callback(file.readPrefab(), null)
            return
        }
        ECSRegistry.init()
        Signature.findName(file) { signature, _ ->
            when (signature) {
                "json" -> {
                    if (file.lcExtension == "gltf") {
                        loadPrefabFromFolder(file, callback)
                    } else file.inputStream { it, e ->
                        if (it != null) {
                            val prefab = JsonStringReader.read(it, EngineBase.workspace, false).firstOrNull()
                            it.close()
                            if (prefab is Prefab) prefab.source = file
                            if (prefab != null) {
                                callback(prefab, null)
                            } else {
                                loadPrefabFromFolder(file, callback)
                            }
                        } else {
                            when (e) {
                                is UnknownClassException -> LOGGER.warn("$e by $file", e)
                                is InvalidFormatException -> if (printJsonErrors && file.lcExtension == "json")
                                    LOGGER.warn("$e by $file", e)
                                else -> e?.printStackTrace() // may be interesting
                            }
                            loadPrefabFromFolder(file, callback)
                        }
                    }
                }
                "yaml" -> {
                    val unityReader = unityReader
                    if (unityReader != null) {
                        unityReader(file) { prefab, e ->
                            if (prefab is Prefab) prefab.source = file
                            if (prefab != null) {
                                callback(prefab, null)
                            } else {
                                LOGGER.warn("$file is yaml, but not from Unity")
                                e?.printStackTrace()
                                loadPrefabFromFolder(file, callback)
                            }
                        }
                    } else loadPrefabFromFolder(file, callback)
                }
                else -> {
                    if (signature in imageFormats1 || signature == "gimp" || signature == "webp") {
                        callback(ImagePlane(file), null)
                    } else loadPrefabFromFolder(file, callback)
                }
            }
        }
    }

    private fun loadPrefabFromFolder(file: FileReference, callback: (Saveable?, Exception?) -> Unit) {
        val folder = InnerFolderCache.readAsFolder(file, false)
        if (folder != null) {
            val scene = folder.getChild("Scene.json") as? PrefabReadable
            val scene2 = scene ?: folder.listChildren().firstInstanceOrNull(PrefabReadable::class)
            if (scene2 != null) {
                val prefab = scene2.readPrefab()
                prefab.source = file
                callback(prefab, null)
            } else callback(null, null)
        } else callback(null, null)
    }

    private fun getPrefabPair(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        timeout: Long = prefabTimeout,
        async: Boolean = false
    ): FileReadPrefabData? {
        return when {
            resource == null || resource == InvalidRef -> null
            resource is InnerLinkFile -> {
                notifyLink(resource)
                getPrefabPair(resource.link, depth, timeout, async)
            }
            resource is PrefabReadable -> {
                val result = FileReadPrefabData(resource)
                result.value = resource.readPrefab()
                result
            }
            resource.exists && !resource.isDirectory -> {
                val entry = getFileEntry(resource, false, timeout, async, ::loadPrefabPair)
                warnLoadFailedMaybe(resource, entry)
                return entry
            }
            else -> null
        }
    }

    private fun getPrefabPairAsync(
        resource: FileReference?,
        callback: Callback<FileReadPrefabData?>,
        depth: Int = maxPrefabDepth,
        timeout: Long = prefabTimeout,
    ) {
        when {
            resource == null || resource == InvalidRef -> {
                callback.ok(null)
            }
            resource is InnerLinkFile -> {
                notifyLink(resource)
                getPrefabPairAsync(resource.link, callback, depth, timeout)
            }
            resource is PrefabReadable -> {
                val prefab = resource.readPrefab()
                val result = FileReadPrefabData(resource)
                result.value = prefab
                callback.ok(result)
            }
            resource.exists && !resource.isDirectory -> {
                getFileEntryAsync(
                    resource, false,
                    timeout, true, ::loadPrefabPair
                ) { entry, err ->
                    warnLoadFailedMaybe(resource, entry)
                    callback.call(entry, err)
                }
            }
            else -> {
                callback.ok(null)
            }
        }
    }

    private fun notifyLink(resource: InnerLinkFile) {
        LOGGER.debug("[link] {} -> {}", resource, resource.link)
    }

    private fun warnLoadFailedMaybe(resource: FileReference?, entry: Any?) {
        if (entry is FileReadPrefabData && entry.hasValue && entry.value == null) {
            LOGGER.warn("Could not load $resource as prefab")
        }
    }

    private fun loadPrefabPair(file: FileReference, lastModified: Long): FileReadPrefabData {
        if (debugLoading) LOGGER.debug("Loading {}@{}", file, lastModified)
        ensureClasses()
        val data = FileReadPrefabData(file)
        loadPrefab4(file) { loaded, e ->
            data.value = loaded
            if (loaded != null) FileWatch.addWatchDog(file)
            if (debugLoading) LOGGER.debug(
                "Loaded ${file.absolutePath.shorten(200)}, " +
                        "got ${loaded?.className}@${hash32(loaded)}"
            )
            e?.printStackTrace()
        }
        return data
    }

    @Suppress("unused")
    fun <V : PrefabSaveable> loadOrInit(
        source: FileReference, clazz: KClass<V>, workspace: FileReference,
        generateInstance: () -> V
    ): Triple<FileReference, Prefab, V> {
        val prefab0 = PrefabCache[source]
        val sample0 = prefab0?.createInstance()
        if (prefab0 != null && clazz.isInstance(sample0)) {
            @Suppress("UNCHECKED_CAST")
            return Triple(source, prefab0, sample0 as V)
        }
        val sample1 = generateInstance()
        sample1.ref // create and fill prefab
        val prefab1 = sample1.prefab!!
        prefab1.source = source
        source.writeText(JsonStringWriter.toText(prefab1, workspace))
        return Triple(source, prefab1, sample1)
    }
}