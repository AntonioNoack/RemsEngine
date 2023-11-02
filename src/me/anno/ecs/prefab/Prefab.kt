package me.anno.ecs.prefab

import me.anno.Build
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.structures.maps.CountMap
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager

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
    var adds: List<CAdd> = emptyList()
    val addedPaths: HashSet<Pair<Path, String>>? = if (Build.isShipped) null else HashSet()

    val sets = KeyPairMap<Path, String, Any?>(256)

    var prefab: FileReference = InvalidRef
    var wasCreatedFromJson = false
    var source: FileReference = InvalidRef

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

    fun invalidateInstance() {
        if (source !is PrefabReadable || adds.isNotEmpty() || sets.isNotEmpty()) {
            synchronized(this) {
                _sampleInstance?.destroy()
                _sampleInstance = null
            }
        } else LOGGER.warn("Cannot invalidate tmp-prefab")
        // todo all child prefab instances would need to be invalidated as well
    }

    fun sealFromModifications() {
        isWritable = false
    }

    fun ensureMutableLists() {
        if (adds !is ArrayList) adds = ArrayList(adds)
    }

    fun countTotalChanges(async: Boolean, depth: Int = 20): Int {
        var sum = adds.size + sets.size
        if (depth > 0) {
            if (prefab != InvalidRef) {
                val prefab = PrefabCache[prefab, maxPrefabDepth, async]
                if (prefab != null) sum += prefab.countTotalChanges(async, depth - 1)
            }
            for (change in adds) {
                val childPrefab = change.prefab
                if (childPrefab != InvalidRef) {
                    val prefab = PrefabCache[childPrefab, maxPrefabDepth, async]
                    if (prefab != null) sum += prefab.countTotalChanges(async, depth - 1)
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
                for (add in adds) {
                    // todo we would need a .subpath and .startsWith
                    if (add.path == parentPath) {
                        val prefab = PrefabCache[add.prefab]
                        if (prefab != null) return prefab[ROOT_PATH, property, depth - 1]
                        break
                    }
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
            return adds.none2 {
                it.path == change.path &&
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
        ensureMutableLists()
        val adds = adds as MutableList
        if (insertIndex == -1) {
            adds.add(change)
        } else {
            // find the correct insert index
            // relative to all other local adds
            var globalInsertIndex = 0
            for (index in adds.indices) {
                if (adds[index].path == change.path) {
                    if (globalInsertIndex == insertIndex) {
                        adds.add(index, change)
                        break
                    }
                    globalInsertIndex++
                }
            }
            if (globalInsertIndex <= insertIndex) {
                adds.add(change)
            }
        }
        invalidateInstance()
        return change
    }

    fun add(change: CSet): CSet {
        if (!isWritable) throw ImmutablePrefabException(source)
        /*ensureMutableLists()
        if (sets.none {
                if (it.path == change.path && it.name == change.name) {
                    it.value = change.value
                    true
                } else false
            }) {
            (sets as MutableList).add(change)
        }*/
        sets[change.path, change.name!!] = change.value
        // apply to sample instance to keep it valid
        updateSample(change)
        return change
    }

    private fun updateSample(change: CSet) {
        val sampleInstance = _sampleInstance
        if (sampleInstance != null && isValid) {
            change.apply(sampleInstance, maxPrefabDepth)
        }
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
        writer.writeObjectList(null, "adds", adds)
        writer.writeObjectList(null, "sets", sets.map { k1, k2, v -> CSet(k1, k2, v) })
        writer.writeObject(null, "history", history)
    }

    override fun readString(name: String, value: String?) {
        if (!isWritable) throw ImmutablePrefabException(source)
        when (name) {
            "prefab" -> prefab = value?.toGlobalFile() ?: InvalidRef
            "className", "class" -> clazzName = value ?: ""
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        if (!isWritable) throw ImmutablePrefabException(source)
        when (name) {
            "prefab" -> prefab = value
            else -> super.readFile(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        if (!isWritable) throw ImmutablePrefabException(source)
        when (name) {
            "changes" -> {
                adds = values.filterIsInstance<CAdd>()
                for (v in values) {
                    if (v is CSet) {
                        val vName = v.name
                        if (vName != null) {
                            sets[v.path, vName] = v.value
                        }
                    }
                }
            }
            "adds" -> adds = values.filterIsInstance<CAdd>()
            "sets" -> {
                for (v in values) {
                    if (v is CSet) {
                        val vName = v.name
                        if (vName != null) {
                            sets[v.path, vName] = v.value
                        }
                    }
                }
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (!isWritable) throw ImmutablePrefabException(source)
        when (name) {
            "history" -> history = value as? ChangeHistory ?: return
            else -> super.readObject(name, value)
        }
    }

    fun getSampleInstance(depth: Int = maxPrefabDepth): PrefabSaveable {
        synchronized(this) {
            return if (!isValid) {
                val instance = PrefabCache.createInstance(this, prefab, adds, sets, depth, clazzName)
                _sampleInstance = instance
                instance
            } else _sampleInstance!!
        }
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