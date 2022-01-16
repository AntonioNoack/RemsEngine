package me.anno.ecs.prefab

import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Change
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.maps.CountMap
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager

class Prefab : Saveable {

    constructor() : super()

    constructor(clazzName: String) : this() {
        this.clazzName = clazzName
    }

    constructor(clazzName: String, prefab: FileReference) : this(clazzName) {
        this.prefab = prefab
    }

    constructor(prefab: Prefab) {
        this.clazzName = prefab.clazzName
        this.prefab = prefab.source
    }

    var clazzName: String = ""

    val addCounts = CountMap<Pair<Char, Path>>()
    var adds: List<CAdd> = emptyList()

    val sets = KeyPairMap<Path, String, Any?>(256)

    var prefab: FileReference = InvalidRef
    var wasCreatedFromJson = false
    var source: FileReference = InvalidRef

    fun invalidateInstance() {
        synchronized(this) {
            sampleInstance?.destroy()
            sampleInstance = null
            isValid = false
        }
        // todo all child prefab instances would need to be invalidated as well
    }

    val instanceName get() = sets[ROOT_PATH, "name"]?.toString()

    @NotSerializedProperty
    var isWritable = true
        private set

    fun sealFromModifications() {
        isWritable = false
        // make adds and sets immutable?
    }

    fun ensureMutableLists() {
        if (adds !is MutableList) adds = ArrayList(adds)
        // if (sets !is MutableList) sets = ArrayList(sets)
    }

    fun addAll(changes: Collection<Change>) {
        if (!isWritable) throw ImmutablePrefabException(source)
        for (change in changes) {
            add(change)
        }
    }

    fun getPrefabOrSource() = prefab.nullIfUndefined() ?: source

    fun countTotalChanges(async: Boolean): Int {
        var sum = adds.size + sets.size
        if (prefab != InvalidRef) {
            val prefab = loadPrefab(prefab, HashSet(), async)
            if (prefab != null) sum += prefab.countTotalChanges(async)
        }
        for (change in adds) {
            val childPrefab = change.prefab
            if (childPrefab != InvalidRef) {
                val prefab = loadPrefab(childPrefab, HashSet(), async)
                if (prefab != null) sum += prefab.countTotalChanges(async)
            }
        }
        return sum
    }

    // for the game runtime, we could save the prefab instance here
    // or maybe even just add the changes, and merge them
    // (we don't need to override twice or more times)

    var history: ChangeHistory? = null
    var isValid = false

    fun add(change: CAdd, index: Int): Path {
        return add(change).getChildPath(index)
    }

    fun set(path: Path, name: String, value: Any?) {
        // add(CSet(path, name, value))
        if (!isWritable) throw ImmutablePrefabException(source)
        sets[path, name] = value
        // apply to sample instance to keep it valid
        updateSample(path, name, value)
        // todo all child prefab instances would need to be updated as well
        // todo same for add...
    }

    fun setIfNotExisting(path: Path, name: String, value: Any?) {
        if (!sets.contains(path, name)) {// could be optimized to use no instantiations
            set(path, name, value)
        }
    }

    /**
     * does not check, whether the change already exists;
     * it assumes, that it does not yet exist
     * */
    fun setUnsafe(path: Path, name: String, value: Any?) {
        /*ensureMutableLists()
        (sets as MutableList).add(CSet(path, name, value))*/
        sets[path, name] = value
    }

    fun add(parentPath: Path, typeChar: Char, type: String, name: String, index: Int): Path {
        return add(CAdd(parentPath, typeChar, type, name, InvalidRef)).getChildPath(index)
    }

    fun add(parentPath: Path, typeChar: Char, type: String, name: String, ref: FileReference): Path {
        val index = addCounts.getAndInc(typeChar to parentPath)
        return add(CAdd(parentPath, typeChar, type, name, ref)).getChildPath(index)
    }

    fun add(parentPath: Path, typeChar: Char, clazzName: String): Path {
        val index = addCounts.getAndInc(typeChar to parentPath)
        return add(CAdd(parentPath, typeChar, clazzName, clazzName, InvalidRef)).getChildPath(index)
    }

    fun add(parentPath: Path, typeChar: Char, clazzName: String, index: Int): Path {
        return add(CAdd(parentPath, typeChar, clazzName, clazzName, InvalidRef)).getChildPath(index)
    }

    fun add(parentPath: Path, typeChar: Char, clazzName: String, name: String): Path {
        val index = addCounts.getAndInc(typeChar to parentPath)
        return add(CAdd(parentPath, typeChar, clazzName, name, InvalidRef)).getChildPath(index)
    }

    fun <V : Change> add(change: V): V {
        if (!isWritable) throw ImmutablePrefabException(source)
        when (change) {
            is CAdd -> {
                ensureMutableLists()
                (adds as MutableList).add(change)
                isValid = false
            }
            is CSet -> {
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
            }
            else -> LOGGER.warn("Unknown change type")
        }
        return change
    }

    private fun updateSample(change: CSet) {
        val sampleInstance = sampleInstance
        if (sampleInstance != null && isValid) {
            change.apply(sampleInstance, null)
        }
    }

    private fun updateSample(path: Path, name: String, value: Any?) {
        val sampleInstance = sampleInstance
        if (sampleInstance != null && isValid) {
            CSet.apply(sampleInstance, path, name, value)
        }
    }

    fun setProperty(name: String, value: Any?) {
        set(ROOT_PATH, name, value)
    }

    fun setProperty(path: Path, name: String, value: Any?) {
        set(path, name, value)
    }

    fun getProperty(name: String): Any? {
        return sets[ROOT_PATH, name]
    }

    private var sampleInstance: PrefabSaveable? = null

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("prefab", prefab)
        writer.writeString("className", clazzName)
        writer.writeObjectList(null, "adds", adds)
        writer.writeObjectList(null, "sets", sets.map { k1, k2, v -> CSet(k1, k2, v) })
        writer.writeObject(null, "history", history)
    }

    override fun readString(name: String, value: String?) {
        if (!isWritable) throw ImmutablePrefabException(source)
        when (name) {
            "prefab" -> prefab = value?.toGlobalFile() ?: InvalidRef
            "className" -> clazzName = value ?: ""
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

    fun getSampleInstance(chain: MutableSet<FileReference>? = HashSet()): PrefabSaveable {
        if (!isValid) synchronized(this) {
            if (!isValid) {
                val instance = PrefabCache.createInstance(this, prefab, adds, sets, chain, clazzName)
                instance.forAll {
                    if (it.prefab !== this) {
                        throw IllegalStateException("Incorrectly created prefab!")
                    }
                }
                // assign super instance? we should really cache that...
                sampleInstance = instance
                isValid = true
            }
        }
        return sampleInstance!!
    }

    fun createInstance(chain: MutableSet<FileReference>? = HashSet()): PrefabSaveable {
        val clone = getSampleInstance(chain).clone()
        clone.forAll {
            if (it.prefab !== this)
                throw IllegalStateException("Incorrectly created prefab!")
        }
        return clone
    }

    override val className: String = "Prefab"
    override val approxSize: Int = 1_000_000_000

    override fun isDefaultValue(): Boolean =
        adds.isEmpty() && sets.isEmpty() && prefab == InvalidRef && history == null

    companion object {
        private val LOGGER = LogManager.getLogger(Prefab::class)
    }

}