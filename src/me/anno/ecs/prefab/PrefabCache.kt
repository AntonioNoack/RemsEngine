package me.anno.ecs.prefab

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.FileCacheSection.overrideFileEntry
import me.anno.cache.NullCacheData
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabByFileCache.Companion.ensureClasses
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
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

    operator fun get(resource: FileReference?): AsyncCacheData<Prefab> {
        return pairToPrefab(getPrefabPair(resource, maxPrefabDepth, timeoutMillis))
    }

    operator fun get(resource: FileReference?, depth: Int = maxPrefabDepth, timeout: Long = timeoutMillis) =
        pairToPrefab(getPrefabPair(resource, depth, timeout))

    private fun pairToPrefab(pair: AsyncCacheData<PrefabPair>): AsyncCacheData<Prefab> {
        return pair.mapNext { pair ->
            pair.prefab ?: run {
                val instance = pair.instance
                if (instance is PrefabSaveable) {
                    instance.ref
                    instance.prefab
                } else null
            }
        }
    }

    fun getPrefabSampleInstance(resource: FileReference?): AsyncCacheData<Saveable> =
        getPrefabSampleInstance(resource, maxPrefabDepth)

    fun getPrefabSampleInstance(
        resource: FileReference?,
        depth: Int = maxPrefabDepth
    ): AsyncCacheData<Saveable> {
        return getPrefabPair(resource, depth, timeoutMillis).mapNext { pair ->
            pair.instance ?: pair.prefab?.getSampleInstance(depth)
        }
    }

    fun newPrefabInstance(
        resource: FileReference?,
        depth: Int = maxPrefabDepth
    ): AsyncCacheData<PrefabSaveable> {
        return getPrefabSampleInstance(resource, depth).mapNext { base ->
            if (base is PrefabSaveable) {
                val clone = base.clone()
                clone.prefab = null
                clone
            } else null
        }
    }

    fun printDependencyGraph(prefab: FileReference): String {
        val connections = HashMap<FileReference, List<FileReference>>()
        Recursion.collectRecursive(prefab) { next, remaining ->
            val prefab2 = PrefabCache[next].waitFor()
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
        }, ${nameList.map { "${get(it.key).waitFor()?.get(Path.ROOT_PATH, "name")}" }}, $nameMap"
    }

    fun createSuperInstance(prefab: FileReference, depth: Int, clazzName: String): AsyncCacheData<PrefabSaveable> {
        if (depth < 0) {
            LOGGER.warn("Circular dependency in $prefab, ${printDependencyGraph(prefab)}")
            return NullCacheData.get()
        }
        val depth1 = depth - 1
        return PrefabCache[prefab, depth1].mapNextNullable { prefab ->
            val instance = prefab
                ?.createInstance(depth1)
                ?: Saveable.createOrNull(clazzName) as? PrefabSaveable
                ?: Entity()
            instance.prefabPath = Path.ROOT_PATH
            instance
        }
    }

    fun loadJson(resource: FileReference?): AsyncCacheData<Saveable> {
        return when (resource) {
            InvalidRef, null -> NullCacheData.get()
            is PrefabReadable -> AsyncCacheData(resource.readPrefab())
            else -> JsonStringReader.read(resource, EngineBase.workspace, true).mapNext { read ->
                val prefab = read.firstOrNull()
                if (prefab == null) LOGGER.warn("No Prefab found in $resource:${resource::class.simpleName}! $read")
                // else LOGGER.info("Read ${prefab.changes?.size} changes from $resource")
                (prefab as? Prefab)?.wasCreatedFromJson = true
                prefab
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
        SignatureCache[file].waitFor { signature0 ->
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
        timeout: Long = timeoutMillis
    ): AsyncCacheData<PrefabPair> {
        var source = resource
        while (source is InnerLinkFile) {
            notifyLink(source)
            source = source.link
        }
        return when {
            source == null || source == InvalidRef -> NullCacheData.get()
            source is PrefabReadable -> {
                val result = PrefabPair(source)
                result.value = source.readPrefab()
                AsyncCacheData(result)
            }
            source.exists && !source.isDirectory -> {
                getFileEntry(source, false, timeout, ::loadPrefabPair)
            }
            else -> NullCacheData.get()
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

    private fun notifyLink(resource: InnerLinkFile) {
        LOGGER.debug("[link] {} -> {}", resource, resource.link)
    }

    private fun loadPrefabPair(key: FileKey, result: AsyncCacheData<PrefabPair>) {
        val (file: FileReference, lastModified: Long) = key
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

    @Deprecated(AsyncCacheData.ASYNC_WARNING)
    fun <V : PrefabSaveable> loadOrInit(
        source: FileReference, clazz: KClass<V>, workspace: FileReference,
        generateInstance: () -> V
    ): Triple<FileReference, Prefab, V> {
        val prefab0 = PrefabCache[source].waitFor()
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