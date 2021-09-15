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
import me.anno.utils.LOGGER
import me.anno.utils.files.LocalFile.toGlobalFile

class Prefab : Saveable {

    constructor() : super()

    constructor(clazzName: String) : this() {
        this.clazzName = clazzName
    }

    constructor(clazzName: String, prefab: FileReference) : this(clazzName) {
        this.prefab = prefab
    }

    var clazzName: String? = null

    var adds: List<CAdd>? = null
    var sets: List<CSet>? = null

    var prefab: FileReference = InvalidRef
    var wasCreatedFromJson = false
    var source: FileReference = InvalidRef

    fun createLists() {
        if (adds == null) adds = ArrayList()
        if (sets == null) sets = ArrayList()
    }

    fun addAll(changes: Collection<Change>) {
        for (change in changes) {
            add(change)
        }
    }

    fun getPrefabOrSource() = prefab.nullIfUndefined() ?: source

    fun countTotalChanges(): Int {
        var sum = adds?.size ?: 0
        if (prefab != InvalidRef) sum += loadPrefab(prefab)?.countTotalChanges() ?: 0
        for (change in adds ?: return sum) {
            if (change.prefab != InvalidRef) {
                sum += loadPrefab(change.prefab)?.countTotalChanges() ?: 0
            }
        }
        sum += sets?.size ?: 0
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

    fun <V : Change> add(change: V): V {
        when (change) {
            is CAdd -> {
                if (adds == null) createLists()
                (adds as MutableList).add(change)
                isValid = false
            }
            is CSet -> {
                if (sets == null) createLists()
                (sets as MutableList).add(change)
                // apply to sample instance to keep it valid
                if (sampleInstance != null && isValid) {
                    change.apply(sampleInstance!!, null)
                }
            }
            else -> LOGGER.warn("Unknown change type")
        }
        return change
    }

    fun setProperty(key: String, value: Any?) {
        if (sets == null) sets = ArrayList()
        val sets = sets as MutableList
        val change = CSet(ROOT_PATH, key, value)
        sets.removeIf { it.path.isEmpty() && it.name == key }
        sets.add(change)
        if (sampleInstance != null && isValid) {
            change.apply(sampleInstance!!, null)
        }
    }

    private var sampleInstance: PrefabSaveable? = null

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("prefab", prefab)
        writer.writeString("className", clazzName)
        writer.writeObjectList(null, "adds", adds)
        writer.writeObjectList(null, "sets", sets)
        writer.writeObject(null, "history", history)
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "prefab" -> prefab = value?.toGlobalFile() ?: InvalidRef
            "className" -> clazzName = value
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "prefab" -> prefab = value
            else -> super.readFile(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "changes" -> {
                adds = values.filterIsInstance<CAdd>()
                sets = values.filterIsInstance<CSet>()
            }
            "adds" -> adds = values.filterIsInstance<CAdd>()
            "sets" -> sets = values.filterIsInstance<CSet>()
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "history" -> history = value as? ChangeHistory ?: return
            else -> super.readObject(name, value)
        }
    }

    fun getSampleInstance(chain: MutableSet<FileReference>? = HashSet()): PrefabSaveable {
        if (!isValid) synchronized(this) {
            if (!isValid) {
                val instance = PrefabCache.createInstance(prefab, adds, sets, chain, clazzName!!)
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
    override val approxSize: Int = 100_000_000

    override fun isDefaultValue(): Boolean =
        (adds == null || adds!!.isEmpty()) && prefab == InvalidRef && history == null

    companion object {
        // private val LOGGER = LogManager.getLogger(Prefab::class)
    }

}