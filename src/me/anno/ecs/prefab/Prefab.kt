package me.anno.ecs.prefab

import me.anno.Build
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.base.InvalidClassException
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.structures.maps.CountMap
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager

/**
 * like in Unity: blueprint for any PrefabSaveable (Entities, Components, Materials, ...) ;
 * is serialized as a set of changes (adding instances, changing properties) ;
 * can inherit from other Prefabs, and added instances can do that, too, so you can make your world truly modular
 * */
class Prefab : Saveable {

    constructor() : this("Entity")

    constructor(clazzName: String) {
        this.clazzName = clazzName
    }

    constructor(clazzName: String, prefab: FileReference) : this(clazzName) {
        this.prefab = prefab
    }

    constructor(prefab: Prefab) : this(prefab.clazzName, prefab.source)

    @NotSerializedProperty
    var isWritable = true
        private set

    var clazzName = ""

    val addCounts = CountMap<Pair<Char, Path>>()
    val adds = HashMap<Path, ArrayList<CAdd>>()
    val addedPaths: HashSet<Pair<Path, String>>? = if (Build.isShipped) null else HashSet()

    val sets = KeyPairMap<Path, String, Any?>(256)

    var prefab: FileReference = InvalidRef
    var wasCreatedFromJson = false
    var source: FileReference = InvalidRef

    val listeners = HashSet<Prefab>()

    val instanceName get() = sets[ROOT_PATH, "name"] as? String

    // for the game runtime, we could save the prefab instance here
    // or maybe even just add the changes, and merge them
    // (we don't need to override twice or more times)

    @NotSerializedProperty
    var history: ChangeHistory? = null

    @NotSerializedProperty
    var isValid: Boolean
        get() = _sampleInstance != null
        set(value) {
            if (value) throw IllegalArgumentException()
            invalidateInstance()
        }

    fun find(path: Path, depth: Int = 0): CAdd? {
        if (depth > 0) throw NotImplementedError()
        return adds[path.parent]?.firstOrNull {
            it.nameId == path.nameId
        }
    }

    fun invalidateInstance() {
        if (source !is PrefabReadable || adds.isNotEmpty() || sets.isNotEmpty()) {
            synchronized(this) {
                _sampleInstance?.destroy()
                _sampleInstance = null
            }
            callListeners()
        } else LOGGER.warn("Cannot invalidate tmp-prefab")
    }

    fun callListeners() {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.invalidateInstance()
            }
        }
    }

    /**
     * returns whether this prefab adds that path (without inheritance)
     * */
    fun addsByItself(path: Path): Boolean {
        return path == ROOT_PATH ||
                adds[path.parent ?: ROOT_PATH]?.any2 { it.matches(path) } == true
    }

    fun sealFromModifications() {
        isWritable = false
    }

    fun countTotalChanges(async: Boolean, depth: Int = 20): Int {
        var sum = adds.size + sets.size
        if (depth > 0) {
            if (prefab != InvalidRef) {
                val prefab = PrefabCache[prefab, async]
                if (prefab != null) sum += prefab.countTotalChanges(async, depth - 1)
            }
            for ((_, addI) in adds) {
                for (change in addI) {
                    val childPrefab = change.prefab
                    if (childPrefab != InvalidRef) {
                        val prefab = PrefabCache[childPrefab, async]
                        if (prefab != null) sum += prefab.countTotalChanges(async, depth - 1)
                    }
                }
            }
        }
        return sum
    }

    override operator fun get(propertyName: String): Any? {
        return this[ROOT_PATH, propertyName]
    }

    operator fun get(property: String, depth: Int): Any? {
        return this[ROOT_PATH, property, depth]
    }

    operator fun get(path: Path, property: String, depth: Int = maxPrefabDepth): Any? {
        val sample = sets[path, property]
        if (sample != null) return sample
        if (depth > 0) {
            // check adds
            val parentPath = path.parent
            if (parentPath != null) {
                for (add in adds[parentPath] ?: emptyList()) {
                    // todo we would need a .subpath and .startsWith
                    val prefab = PrefabCache[add.prefab]
                    if (prefab != null) return prefab[ROOT_PATH, property, depth - 1]
                    break
                }
            }
            val prefab = PrefabCache[prefab]
            if (prefab != null) return prefab[path, property, depth - 1]
        }
        return null
    }

    fun add(change: CAdd, index: Int, insertIndex: Int = -1): Path {
        return add(change, insertIndex).getSetterPath(index)
    }

    fun set(instance: PrefabSaveable, key: String, value: Any?) {
        set(instance.prefabPath, key, value)
    }

    operator fun set(path: Path, name: String, value: Any?) {
        // add(CSet(path, name, value))
        if (!isWritable) throw ImmutablePrefabException(source)
        sets[path, name] = value
        // apply to sample instance to keep it valid
        updateSample(path, name, value)
        callListeners()
        // todo all child prefab instances would need to be updated as well
        // todo same for add...
    }

    override fun set(propertyName: String, value: Any?): Boolean {
        set(ROOT_PATH, propertyName, value)
        return true
    }

    /**
     * does not check, whether the change already exists;
     * it assumes, that it does not yet exist
     * */
    fun setUnsafe(path: Path, name: String, value: Any?) {
        if (!isWritable) throw ImmutablePrefabException(source)
        sets.setUnsafe(path, name, value)
    }

    /**
     * does not check, whether the change already exists;
     * it assumes, that it does not yet exist
     * */
    fun setUnsafe(name: String, value: Any?) {
        if (!isWritable) throw ImmutablePrefabException(source)
        sets.setUnsafe(ROOT_PATH, name, value)
    }

    fun add(parentPath: Path, typeChar: Char, type: String, nameId: String, index: Int, insertIndex: Int = -1): Path {
        return add(CAdd(parentPath, typeChar, type, nameId, InvalidRef), insertIndex).getSetterPath(index)
    }

    fun add(
        parentPath: Path,
        typeChar: Char,
        className: String,
        nameId: String,
        ref: FileReference,
        insertIndex: Int = -1
    ): Path {
        val index = findNextIndex(typeChar, parentPath)
        return add(CAdd(parentPath, typeChar, className, nameId, ref), insertIndex).getSetterPath(index)
    }

    fun findNextIndex(typeChar: Char, parentPath: Path): Int {
        return addCounts.getAndInc(typeChar to parentPath)
    }

    @Deprecated("Please use the functions with explicit names")
    fun add(parentPath: Path, typeChar: Char, clazzName: String, insertIndex: Int = -1): Path {
        val index = addCounts.getAndInc(typeChar to parentPath)
        return add(CAdd(parentPath, typeChar, clazzName, clazzName, InvalidRef), insertIndex).getSetterPath(index)
    }

    fun add(parentPath: Path, typeChar: Char, clazzName: String, nameId: String, insertIndex: Int = -1): Path {
        val index = addCounts.getAndInc(typeChar to parentPath) // only true if this is a new instance
        return add(CAdd(parentPath, typeChar, clazzName, nameId, InvalidRef), insertIndex).getSetterPath(index)
    }

    fun canAdd(change: CAdd): Boolean {
        if (!Build.isShipped) {
            val key = Pair(change.path, change.nameId)
            if (addedPaths?.contains(key) == true) return false
            // todo check branched prefabs for adds as well
            val sourcePrefab = PrefabCache[prefab]
            return sourcePrefab?.canAdd(change) ?: true
        } else {
            return (adds[change.path] ?: emptyList()).none2 {
                it.nameId == change.nameId
            }
        }
    }

    fun add(change: CAdd, insertIndex: Int): CAdd {
        if (!isWritable) throw ImmutablePrefabException(source)
        if (!Build.isShipped) {
            val key = Pair(change.path, change.nameId)
            if (addedPaths?.contains(key) == true)
                throw IllegalArgumentException("Duplicate names are forbidden, path: ${change.path}, nameId: ${change.nameId}")
            // todo check branched prefabs for adds as well
            val sourcePrefab = PrefabCache[prefab]
            if (sourcePrefab != null) {
                if (sourcePrefab.addedPaths?.contains(key) == true)
                    throw IllegalArgumentException("Duplicate names are forbidden, path: ${change.path}, nameId: ${change.nameId}")
            }
            addedPaths?.add(key)
        }
        val adds = adds
        if (insertIndex == -1) {
            adds.getOrPut(change.path) { ArrayList() }
                .add(change)
        } else {
            // find the correct insert index
            // relative to all other local adds
            var globalInsertIndex = 0
            val addsI = adds.getOrPut(change.path) { ArrayList() }
            for (index in addsI.indices) {
                if (addsI[index].path == change.path) {
                    if (globalInsertIndex == insertIndex) {
                        addsI.add(index, change)
                        globalInsertIndex = Int.MAX_VALUE
                        break
                    }
                    globalInsertIndex++
                }
            }
            if (globalInsertIndex <= insertIndex) {
                addsI.add(change)
            }
        }
        invalidateInstance()
        return change
    }

    private fun updateSample(path: Path, name: String, value: Any?) {
        val sampleInstance = _sampleInstance
        if (sampleInstance != null && isValid) {
            CSet.apply(sampleInstance, path, name, value)
        }
    }

    @Suppress("PropertyName")
    var _sampleInstance: PrefabSaveable? = null

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("prefab", prefab)
        writer.writeString("class", clazzName)
        writer.writeObjectList(null, "adds", adds.values.flatten())
        writer.writeObjectList(null, "sets", sets.map { k1, k2, v -> CSet(k1, k2, v) })
        writer.writeObject(null, "history", history)
    }

    override fun setProperty(name: String, value: Any?) {
        if (!isWritable) throw ImmutablePrefabException(source)
        when(name){
            "prefab" -> prefab = (value as? String)?.toGlobalFile() ?: (value as? FileReference) ?: InvalidRef
            "className", "class" -> clazzName = value as? String ?: return
            "history" -> history = value as? ChangeHistory ?: return
            "changes" -> {
                val values = value as? Array<*> ?: return
                readAdds(values.filterIsInstance<CAdd>())
                readSets(values.filterIsInstance<CSet>())
            }
            "adds" -> {
                val values = value as? Array<*> ?: return
                readAdds(values.filterIsInstance<CAdd>())
            }
            "sets" -> {
                val values = value as? Array<*> ?: return
                readSets(values.filterIsInstance<CSet>())
            }
            else -> super.setProperty(name, value)
        }
    }

    fun readAdds(values: List<CAdd>) {
        for (v in values) {
            adds.getOrPut(v.path) { ArrayList() }.add(v)
        }
    }

    fun readSets(values: List<CSet>) {
        for (v in values) {
            sets[v.path, v.name] = v.value
        }
    }

    fun getSampleInstance(depth: Int = maxPrefabDepth): PrefabSaveable {
        return synchronized(this) {
            if (!isValid) {
                val instance = createNewInstance(depth)
                _sampleInstance = instance
                instance
            } else _sampleInstance!!
        }
    }

    private fun createNewInstance(depth: Int): PrefabSaveable {
        val adds = adds
        val superPrefab = prefab
        val clazzName = clazzName
        // to do here is some kind of race condition taking place
        // without this println, or Thread.sleep(),
        // prefabs extending ScenePrefab will not produce correct instances
        // LOGGER.info("Creating instance from thread ${Thread.currentThread().name}, from '${prefab?.source}', ${prefab?.adds?.size} adds + ${prefab?.sets?.size}")
        // Thread.sleep(10)
        val instance = PrefabCache.createSuperInstance(superPrefab, depth, clazzName)
        instance.setPath(this, ROOT_PATH)
        for ((_, addsI) in adds.entries.sortedBy { it.key.depth }) {
            for (index in addsI.indices) {
                val add = addsI[index]
                try {
                    add.apply(this, instance, depth - 1)
                } catch (e: InvalidClassException) {
                    throw e
                } catch (e: Exception) {
                    LOGGER.warn("Change $index, $add failed")
                    throw e
                }
            }
        }
        sets.forEach { k1, k2, v ->
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


    fun createInstance(depth: Int = maxPrefabDepth): PrefabSaveable {
        return getSampleInstance(depth).clone()
    }

    override val className: String get() = "Prefab"
    override val approxSize get() = 1_000_000_096

    override fun isDefaultValue(): Boolean =
        adds.isEmpty() && sets.isEmpty() && prefab == InvalidRef && history == null

    companion object {
        var maxPrefabDepth = 25
        private val LOGGER = LogManager.getLogger(Prefab::class)
    }
}