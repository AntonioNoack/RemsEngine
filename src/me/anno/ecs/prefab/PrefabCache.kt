package me.anno.ecs.prefab

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.FileCacheSection.getFileEntryAsync
import me.anno.cache.FileCacheSection.overrideFileEntry
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabByFileCache.Companion.ensureClasses
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
import me.anno.engine.ScenePrefab
import me.anno.engine.projects.FileEncoding
import me.anno.engine.projects.GameEngineProject
import me.anno.io.Streams.consumeMagic
import me.anno.io.base.InvalidFormatException
import me.anno.io.base.UnknownClassException
import me.anno.io.binary.BinaryReader
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.InvalidRef
import me.anno.io.files.SignatureCache
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.InnerFolderCache.imageFormats1
import me.anno.io.files.inner.InnerLinkFile
import me.anno.io.json.generic.JsonLike.MAIN_NODE_NAME
import me.anno.io.json.generic.JsonLike.jsonLikeToJson
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.saveable.Saveable
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.io.xml.saveable.XML2JSON
import me.anno.io.yaml.generic.YAMLReader
import me.anno.io.yaml.saveable.YAML2JSON
import me.anno.utils.Logging.hash32
import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertEquals
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.shorten
import org.apache.logging.log4j.LogManager
import java.util.zip.InflaterInputStream
import kotlin.reflect.KClass

/**
 * use this to get access to Prefabs, so you can create new instances from them, or access their sample instance
 * */
@Suppress("MemberVisibilityCanBePrivate")
object PrefabCache : CacheSection<FileKey, PrefabPair>("Prefab") {

    var printJsonErrors = true
    var timeoutMillis = 60_000L

    private val LOGGER = LogManager.getLogger(PrefabCache::class)
    val debugLoading get() = LOGGER.isDebugEnabled()

    operator fun get(resource: FileReference?, async: Boolean): Prefab? =
        pairToPrefab(getPrefabPair(resource, maxPrefabDepth, timeoutMillis, async))

    operator fun get(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        timeout: Long = timeoutMillis,
        async: Boolean = false
    ): Prefab? = pairToPrefab(getPrefabPair(resource, depth, timeout, async))

    private fun pairToPrefab(pair: PrefabPair?): Prefab? {
        pair ?: return null
        val prefab = pair.prefab
        if (prefab != null) return prefab
        val instance = pair.instance
        return if (instance is PrefabSaveable) {
            instance.ref
            instance.prefab
        } else null
    }

    fun getPrefabSampleInstance(resource: FileReference?, async: Boolean): Saveable? =
        getPrefabSampleInstance(resource, maxPrefabDepth, async)

    fun getPrefabSampleInstance(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        async: Boolean = false
    ): Saveable? {
        val pair = getPrefabPair(resource, depth, timeoutMillis, async) ?: return null
        return pair.instance ?: try {
            pair.prefab?.getSampleInstance(depth)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun newPrefabInstance(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        async: Boolean = false
    ): PrefabSaveable? {
        val base = getPrefabSampleInstance(resource, depth, async) as? PrefabSaveable ?: return null
        val clone = base.clone()
        clone.prefab = null // make mutable
        return clone
    }

    fun getPrefabAsync(resource: FileReference?, depth: Int = maxPrefabDepth, callback: Callback<Prefab?>) {
        getPrefabPairAsync(resource, callback.map { it?.prefab }, depth, timeoutMillis)
    }

    fun getPrefabInstanceAsync(resource: FileReference?, depth: Int = maxPrefabDepth, callback: Callback<Saveable?>) {
        getPrefabPairAsync(
            resource, callback.map { if (it != null) getPrefabInstance(it, depth) else null },
            depth, timeoutMillis
        )
    }

    private fun getPrefabInstance(pair: PrefabPair, depth: Int): Saveable? {
        return pair.instance ?: pair.prefab?.getSampleInstance(depth)
    }

    fun printDependencyGraph(prefab: FileReference): String {
        val connections = HashMap<FileReference, List<FileReference>>()
        Recursion.collectRecursive(prefab) { next, remaining ->
            val prefab2 = PrefabCache[next]
            if (prefab2 != null) {
                val con = HashSet<FileReference>()
                val s0 = prefab2.parentPrefabFile
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
        val prefab = this[file, maxPrefabDepth] ?: Prefab("Entity").apply { this.parentPrefabFile = ScenePrefab }
        prefab.sourceFile = file
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

    var unityReader: ((FileReference, Callback<Saveable>) -> Unit)? = null

    private fun readBinRE(file: FileReference, callback: Callback<Saveable>) {
        file.inputStream { str, err ->
            if (str != null) {
                str.consumeMagic(FileEncoding.BINARY_MAGIC)
                val zis = InflaterInputStream(str)
                val reader = BinaryReader(zis)
                reader.readAllInList()
                val prefab = reader.allInstances
                    .firstInstanceOrNull(Prefab::class)
                callback.call(prefab, null)
            } else callback.err(err)
        }
    }

    private fun readXMLRE(file: FileReference, callback: Callback<Saveable>) {
        file.inputStream { str, err ->
            if (str != null) {
                val node = XMLReader(str.reader()).read() as XMLNode
                assertEquals(MAIN_NODE_NAME, node.type)
                val jsonLike = XML2JSON.fromXML(node)
                readJSONLike(file, jsonLike, callback)
            } else callback.err(err)
        }
    }

    private fun readYAMLRE(file: FileReference, callback: Callback<Saveable>) {
        file.inputStream { str, err ->
            if (str != null) {
                val reader = str.bufferedReader()
                val node = YAMLReader.parseYAML(reader, beautify = false)
                val jsonLike = YAML2JSON.fromYAML(node)
                readJSONLike(file, jsonLike, callback)
            } else callback.err(err)
        }
    }

    private fun readJSONLike(file: FileReference, jsonLike: Any?, callback: Callback<Saveable>) {
        val json = jsonLikeToJson(jsonLike)
        val prefab = JsonStringReader.readFirstOrNull(json, EngineBase.workspace, Saveable::class)
        onReadPrefab(file, prefab, callback)
    }

    private fun onReadPrefab(file: FileReference, prefab: Saveable?, callback: Callback<Saveable>) {
        if (prefab is Prefab) prefab.sourceFile = file
        if (prefab != null) {
            callback.ok(prefab)
        } else {
            loadPrefabFromFolder(file, callback)
        }
    }

    private fun loadPrefab4(file: FileReference, callback: Callback<Saveable>) {
        if (file is PrefabReadable) {
            callback.ok(file.readPrefab())
            return
        }
        ECSRegistry.init()
        SignatureCache.getAsync(file) { signature0 ->
            when (val signature = signature0?.name) {
                "rem" -> readBinRE(file, callback)
                "xml-re" -> readXMLRE(file, callback)
                "yaml-re" -> readYAMLRE(file, callback)
                "json" -> {
                    if (file.lcExtension == "gltf") {
                        loadPrefabFromFolder(file, callback)
                    } else file.inputStream { it, e ->
                        if (it != null) {
                            val prefab = JsonStringReader.read(it, EngineBase.workspace, false).firstOrNull()
                            it.close()
                            onReadPrefab(file, prefab, callback)
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
                            if (prefab is Prefab) prefab.sourceFile = file
                            if (prefab != null) {
                                callback.ok(prefab)
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
                        callback.ok(ImagePlane(file))
                    } else loadPrefabFromFolder(file, callback)
                }
            }
        }
    }

    private fun loadPrefabFromFolder(file: FileReference, callback: Callback<Saveable>) {
        InnerFolderCache.readAsFolder(file, true) { folder, err ->
            if (folder != null) {
                val scene = folder.getChild("Scene.json") as? PrefabReadable
                val scene2 = scene ?: folder.listChildren().firstInstanceOrNull(PrefabReadable::class)
                if (scene2 != null) {
                    val prefab = scene2.readPrefab()
                    prefab.sourceFile = file
                    callback.ok(prefab)
                } else callback.err(err)
            } else callback.err(err)
        }
    }

    fun getPrefabPair(
        resource: FileReference?,
        depth: Int = maxPrefabDepth,
        timeout: Long = timeoutMillis,
        async: Boolean = false
    ): PrefabPair? {
        var source = resource
        while (source is InnerLinkFile) {
            notifyLink(source)
            source = source.link
        }
        return when {
            source == null || source == InvalidRef -> null
            source is PrefabReadable -> {
                val result = PrefabPair(source)
                result.value = source.readPrefab()
                result
            }
            source.exists && !source.isDirectory -> {
                val entry = getFileEntry(source, false, timeout, async, ::loadPrefabPair)
                entry?.waitFor(async)
                warnLoadFailedMaybe(source, entry)
                return entry?.value
            }
            else -> null
        }
    }

    fun setPrefabPair(resource: FileReference?, value: PrefabPair, timeout: Long = timeoutMillis): Boolean {
        var source = resource
        while (source is InnerLinkFile) {
            notifyLink(source)
            source = source.link
        }
        return when (source) {
            null, InvalidRef -> false
            is PrefabReadable -> false
            else -> {
                overrideFileEntry(source, value, timeout)
                true
            }
        }
    }

    private fun getPrefabPairAsync(
        resource: FileReference?,
        callback: Callback<PrefabPair?>,
        depth: Int = maxPrefabDepth,
        timeout: Long = timeoutMillis,
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
                val result = PrefabPair(resource)
                result.value = prefab
                callback.ok(result)
            }
            resource.exists && !resource.isDirectory -> {
                getFileEntryAsync(
                    resource, false,
                    timeout, true, ::loadPrefabPair
                ) { entry, err ->
                    if (entry == null) LOGGER.warn("Could not load $resource as prefab")
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

    private fun warnLoadFailedMaybe(resource: FileReference?, entry: AsyncCacheData<PrefabPair>?) {
        if (entry != null && entry.hasValue && entry.value == null) {
            LOGGER.warn("Could not load $resource as prefab")
        }
    }

    private fun loadPrefabPair(key: FileKey, result: AsyncCacheData<PrefabPair>) {
        val (file: FileReference, lastModified: Long) = key
        if (debugLoading) LOGGER.debug("Loading {}@{}", file, lastModified)
        LOGGER.info("Loading {}@{}", file, lastModified)
        ensureClasses()
        loadPrefab4(file) { loaded, e ->
            val pair = PrefabPair(file)
            pair.value = loaded
            result.value = pair
            if (loaded != null) FileWatch.addWatchDog(file)
            if (debugLoading) LOGGER.debug(
                "Loaded ${file.absolutePath.shorten(200)}, " +
                        "got ${loaded?.className}@${hash32(loaded)}"
            )
            e?.printStackTrace()
        }
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
        prefab1.sourceFile = source
        val encoding = GameEngineProject.encoding.getForExtension(source.lcExtension)
        source.writeBytes(encoding.encode(prefab1, workspace))
        return Triple(source, prefab1, sample1)
    }
}