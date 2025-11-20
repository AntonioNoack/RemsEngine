package me.anno.ecs.prefab

import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.FileCacheSection.overrideFileEntry
import me.anno.cache.Promise
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabByFileCache.Companion.ensureClasses
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
import me.anno.engine.projects.FileEncoding
import me.anno.engine.projects.GameEngineProject
import me.anno.io.base.InvalidFormatException
import me.anno.io.base.UnknownClassException
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
import me.anno.io.xml.generic.XMLReader
import me.anno.io.xml.saveable.XML2JSON
import me.anno.utils.Logging.hash32
import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertEquals
import me.anno.utils.async.Callback
import me.anno.utils.files.LocalFile
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.shorten
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Reads Prefabs and Saveables from a file,
 * and offers you their Prefab (if possible) and their cloning ability
 * */
@Suppress("MemberVisibilityCanBePrivate")
object PrefabCache : CacheSection<FileKey, PrefabPair>("Prefab") {

    var printJsonErrors = true
    var timeoutMillis = 60_000L

    private val LOGGER = LogManager.getLogger(PrefabCache::class)
    val debugLoading get() = LOGGER.isDebugEnabled()

    operator fun get(resource: FileReference?): Promise<PrefabPair> =
        get(resource, maxPrefabDepth, timeoutMillis)

    operator fun get(resource: FileReference?, maxDepth: Int): Promise<PrefabPair> =
        get(resource, maxDepth, timeoutMillis)

    operator fun get(resource: FileReference?, maxDepth: Int, timeoutMillis: Long): Promise<PrefabPair> {
        var source = resource ?: return Promise.empty()
        while (source is InnerLinkFile) {
            notifyLink(source)
            source = source.link
        }
        return getFileEntry(source, false, timeoutMillis, loadPrefabPair)
    }

    fun newInstance(resource: FileReference?, depth: Int = maxPrefabDepth): Promise<Saveable> {
        val result = Promise<Saveable>()
        this[resource, depth].waitFor { prefab ->
            result.value = prefab?.newInstance()
        }
        return result
    }

    fun printDependencyGraph(prefab: FileReference): String {
        val connections = HashMap<FileReference, List<FileReference>>()
        Recursion.collectRecursive(prefab) { next, remaining ->
            val prefab2 = PrefabCache[next].waitFor()?.prefab
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
        }, ${nameList.map { "${get(it.key).waitFor()?.prefab?.instanceName}" }}, $nameMap"
    }

    fun createSuperInstance(prefab: FileReference, depth: Int, clazzName: String): Promise<PrefabSaveable> {
        if (depth < 0) {
            LOGGER.warn("Circular dependency in $prefab, ${printDependencyGraph(prefab)}")
            return Promise.empty()
        }
        val depth1 = depth - 1
        val result = Promise<PrefabSaveable>()
        PrefabCache[prefab, depth1].waitFor { pair ->
            val instance = pair
                ?.prefab
                ?.newInstance(depth1)
                ?: Saveable.createOrNull(clazzName) as? PrefabSaveable
                ?: Entity()
            instance.prefabPath = Path.ROOT_PATH
            result.value = instance
        }
        return result
    }

    private val jsonCache = CacheSection<FileReference, Saveable>("JsonCache")

    fun loadJson(resource: FileReference?): Promise<Saveable> {
        return when (resource) {
            InvalidRef, null -> Promise.empty()
            is PrefabReadable -> Promise(resource.readPrefab())
            else -> jsonCache.getEntry(resource, 10_000) { resource, result ->
                result.waitFor { saveable ->
                    if (saveable == null) LOGGER.warn("No Prefab found in $resource:${resource::class.simpleName}!")
                    if (saveable is Prefab) saveable.wasCreatedFromJson = true
                }
                val workspace = findWorkspace(resource)
                JsonStringReader.readFirstOrNull(
                    resource, workspace,
                    Saveable::class, true, result
                )
            }
        }
    }

    var unityReader: ((FileReference, Callback<Saveable>) -> Unit)? = null

    private fun readXMLRE(file: FileReference, callback: Callback<Saveable>) {
        file.inputStream { str, err ->
            if (str != null) {
                val node = XMLReader(str.reader()).readXMLNode()!!
                assertEquals(MAIN_NODE_NAME, node.type)
                val jsonLike = XML2JSON.fromXML(node)
                readJSONLike(file, jsonLike, callback)
            } else callback.err(err)
        }
    }

    private fun readJSONLike(file: FileReference, jsonLike: Any?, callback: Callback<Saveable>) {
        // todo implement a reader without to-string conversion
        val json = jsonLikeToJson(jsonLike)
        val workspace = findWorkspace(file)
        val prefab = JsonStringReader.readFirstOrNull(json, workspace, Saveable::class)
        onReadPrefab(file, prefab, callback)
    }

    private fun readFileEncoding(file: FileReference, encoding: FileEncoding, callback: Callback<Saveable>) {
        file.readBytes { bytes, err ->
            if (bytes != null) {
                val workspace = findWorkspace(file)
                val saveable = encoding.decode(bytes, workspace, true).firstOrNull()
                if (saveable != null) callback.ok(saveable)
                else callback.err(null)
            } else callback.err(err)
        }
    }

    private fun findWorkspace(file: FileReference): FileReference {
        return LocalFile.findWorkspace(file, EngineBase.workspace)
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
            when (signature0?.name) {
                "rem" -> readFileEncoding(file, FileEncoding.BINARY, callback)
                "xml-re" -> readXMLRE(file, callback)
                "yaml-re" -> readFileEncoding(file, FileEncoding.YAML, callback)
                "json" -> readPrefabJson(file, callback)
                "yaml" -> readPrefabYAML(file, callback)
                in imageFormats1, "gimp", "webp" -> callback.ok(ImagePlane(file))
                else -> loadPrefabFromFolder(file, callback)
            }
        }
    }

    private fun readPrefabYAML(file: FileReference, callback: Callback<Saveable>) {
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

    private fun readPrefabJson(file: FileReference, callback: Callback<Saveable>) {
        if (file.lcExtension == "gltf") {
            loadPrefabFromFolder(file, callback)
        } else file.inputStream { it, e ->
            if (it != null) {
                val workspace = findWorkspace(file)
                val prefab = JsonStringReader.read(it, workspace, false).firstOrNull()
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

    fun setPrefabPair(
        resource: FileReference?, value: PrefabPair,
        timeoutMillis: Long = PrefabCache.timeoutMillis
    ): Boolean {
        var source = resource
        while (source is InnerLinkFile) {
            notifyLink(source)
            source = source.link
        }
        val isWritableSource = when (source) {
            null, InvalidRef, is PrefabReadable -> false
            else -> true
        }
        if (isWritableSource && source != null) {
            overrideFileEntry(source, value, timeoutMillis)
        }
        return isWritableSource
    }

    private fun notifyLink(resource: InnerLinkFile) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Link] {} -> {}", resource, resource.link)
        }
    }

    private val loadPrefabPair = { key: FileKey, result: Promise<PrefabPair> ->
        val file = key.file
        // LOGGER.info("Loading {}@{}", file, lastModified)
        ensureClasses()
        loadPrefab4(file) { loaded, e ->
            result.value = PrefabPair(file, loaded)
            if (loaded != null) FileWatch.addWatchDog(file)
            if (debugLoading) LOGGER.debug(
                "Loaded ${file.absolutePath.shorten(200)}, " +
                        "got ${loaded?.className}@${hash32(loaded)}"
            )
            e?.printStackTrace()
        }
    }

    @Deprecated(Promise.ASYNC_WARNING)
    fun <V : PrefabSaveable> loadOrInit(
        source: FileReference, clazz: KClass<V>, workspace: FileReference,
        fallbackGenerator: () -> V
    ): Triple<FileReference, Prefab, V> {
        val pair = PrefabCache[source].waitFor()
        if (pair != null) {
            val sample0 = clazz.safeCast(pair.newInstance())
            if (sample0 != null) {
                val prefab0 = pair.prefab ?: sample0.getOrCreatePrefab()
                return Triple(source, prefab0, sample0)
            }
        }

        val sample1 = fallbackGenerator()
        val prefab1 = sample1.getOrCreatePrefab()
        prefab1.sourceFile = source
        val encoding = GameEngineProject.encoding.getForExtension(source.lcExtension)
        source.writeBytes(encoding.encode(prefab1, workspace))
        return Triple(source, prefab1, sample1)
    }
}