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
import me.anno.utils.LOGGER
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.maps.CountMap
import me.anno.utils.structures.maps.KeyPairMap

// todo allow to make immutable
// todo and send a warning: instantiate to make mutable
class Prefab : Saveable {

    constructor() : super()

    constructor(clazzName: String) : this() {
        this.clazzName = clazzName
    }

    constructor(clazzName: String, prefab: FileReference) : this(clazzName) {
        this.prefab = prefab
    }

    constructor(prefab: Prefab){
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

    fun invalidate() {
        isValid = false
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

    fun countTotalChanges(): Int {
        var sum = adds.size
        if (prefab != InvalidRef) sum += loadPrefab(prefab)?.countTotalChanges() ?: 0
        for (change in adds) {
            if (change.prefab != InvalidRef) {
                sum += loadPrefab(change.prefab)?.countTotalChanges() ?: 0
            }
        }
        sum += sets.size
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
        add(CSet(path, name, value))
    }

    fun setIfNotExisting(path: Path, name: String, value: Any?) {
        if (sets.contains(path, name)) {// could be optimized to use no instantiations
            add(CSet(path, name, value))
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

    fun add(parentPath: Path, typeChar: Char, type: String, name: String, ref: FileReference): Path {
        val index = addCounts.getAndInc(typeChar to parentPath)
        return add(CAdd(parentPath, typeChar, type, name, ref)).getChildPath(index)
    }

    fun add(path: Path, type: Char, clazzName: String) {
        add(CAdd(path, type, clazzName))
    }

    fun add(path: Path, type: Char, clazzName: String, index: Int): Path {
        return add(CAdd(path, type, clazzName), index)
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
        if (sampleInstance != null && isValid) {
            change.apply(sampleInstance!!, null)
        }
    }

    fun setProperty(name: String, value: Any?) {
        set(ROOT_PATH, name, value)
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
                        sets[v.path, v.name!!] = v.value
                    }
                }
            }
            "adds" -> adds = values.filterIsInstance<CAdd>()
            "sets" -> {
                for (v in values) {
                    if (v is CSet) {
                        sets[v.path, v.name!!] = v.value
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
                val instance = PrefabCache.createInstance(prefab, adds, sets, chain, clazzName)
                // assign super instance? we should really cache that...
                instance.prefab2 = this
                sampleInstance = instance
                isValid = true
            }
        }
        return sampleInstance!!
    }

    fun createInstance(chain: MutableSet<FileReference>? = HashSet()): PrefabSaveable {
        return getSampleInstance(chain).clone()
    }

    override val className: String = "Prefab"
    override val approxSize: Int = 1_000_000_000

    override fun isDefaultValue(): Boolean =
        adds.isEmpty() && sets.isEmpty() && prefab == InvalidRef && history == null

    companion object {
        // private val LOGGER = LogManager.getLogger(Prefab::class)
    }

}