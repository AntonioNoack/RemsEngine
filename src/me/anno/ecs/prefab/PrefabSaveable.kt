package me.anno.ecs.prefab

import me.anno.cache.ICacheData
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.EditorField
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.engine.inspector.Inspectable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.base.BaseWriter
import me.anno.io.base.PrefabHelperWriter
import me.anno.io.files.FileReference
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.io.saveable.NamedSaveable
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.utils.structures.Hierarchical
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.withFlag
import me.anno.utils.types.Strings.camelCaseToTitle
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

/**
 * base class for things that can be saved as a Prefab
 * */
abstract class PrefabSaveable : NamedSaveable(), Hierarchical<PrefabSaveable>, Inspectable, ICacheData {

    @SerializedProperty
    var flags = 0 // default shall always be zero

    @EditorField
    @NotSerializedProperty
    override var isEnabled: Boolean
        get() = !flags.hasFlag(DISABLE_FLAG)
        set(value) {
            flags = flags.withFlag(DISABLE_FLAG, !value)
        }

    @EditorField
    @NotSerializedProperty
    override var isCollapsed: Boolean
        get() = !flags.hasFlag(NOT_COLLAPSED_FLAG)
        set(value) {
            flags = flags.withFlag(NOT_COLLAPSED_FLAG, !value)
        }

    /**
     * if something goes wrong, set this field;
     * it will be visible in the editor UI, and print a message to the console
     * */
    @NotSerializedProperty
    var lastWarning: String? = null
        set(value) {
            if (field != value) {
                if (!value.isNullOrEmpty()) {
                    LOGGER.warn("$className: $value")
                }
                field = value
            }
        }

    /**
     * get the original (unmodified) instance for comparisons between properties
     * */
    fun getOriginal(): PrefabSaveable? {
        val sampleInstance = prefab?.getSampleInstance() ?: return null
        return Hierarchy.getInstanceAt(sampleInstance, prefabPath)
    }

    fun getOriginalOrDefault() = getOriginal() ?: getSuperInstance(className)

    val refOrNull get() = prefab?.source
    val ref: FileReference
        get() {
            var prefab = prefab
            if (prefab == null) {
                prefab = Prefab(className)
                prefab.source = InnerTmpPrefabFile(prefab)
                prefab._sampleInstance = this
                this.prefab = prefab
                this.prefabPath = Path.ROOT_PATH
                setAllChildPaths()
                collectPrimaryChanges()
            }
            return prefab.source
        }

    /**
     * only defined while building the game;
     *
     * where a resource is originally coming from;
     * */
    @NotSerializedProperty
    var prefab: Prefab? = null

    /**
     * while loading, stores the path within the original prefab;
     *
     * after that, stores the path within the currently used prefab/scene
     * */
    @NotSerializedProperty
    var prefabPath: Path = Path.ROOT_PATH

    @NotSerializedProperty
    override var parent: PrefabSaveable? = null
        set(value) {
            val oldField = field
            if (oldField != null && oldField !== value) {
                oldField.removeChild(this)
            }
            field = value
        }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("flags", flags, true)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "isCollapsed" -> isCollapsed = value == true
            "nonCollapsed" -> isCollapsed = value != true
            "flags" -> flags = value as? Int ?: return
            else -> super.setProperty(name, value)
        }
    }

    fun getDefaultValue(name: String): Any? {
        val original = getOriginalOrDefault()
        if (original.className != className) {
            LOGGER.warn("Original has type ${original.className}, self is $className")
            return getSuperInstance(className)[name]
        }
        return original[name]
    }

    fun resetProperty(name: String): Any? {
        // how do we find the default value, if the root is null? -> create an empty copy
        val defaultValue = getDefaultValue(name)
        this[name] = defaultValue
        LOGGER.info("Reset $className/$name to $defaultValue")
        return defaultValue
    }

    fun forAll(run: (PrefabSaveable) -> Unit) {
        run(this)
        for (type in listChildTypes()) {
            val childList = getChildListByType(type)
            for (index in childList.indices) {
                childList[index].forAll(run)
            }
        }
    }

    override val listOfAll: List<PrefabSaveable>
        get() {
            val result = ArrayList<PrefabSaveable>()
            result.add(this)
            var workerIndex = 0
            while (workerIndex < result.size) {
                val item = result[workerIndex++]
                for (type in item.listChildTypes()) {
                    result.addAll(item.getChildListByType(type))
                }
            }
            return result
        }

    // e.g., "ec" for child entities + child components
    open fun listChildTypes(): String = ""
    open fun getChildListByType(type: Char): List<PrefabSaveable> = children
    open fun getChildListNiceName(type: Char): String = "Children"
    open fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        LOGGER.warn("$className.addChildByType(index,$type,${child.className}) is not supported")
    }

    fun ensurePrefab() {
        if (prefab != null) return
        val parent = parent
        if (parent != null) {
            parent.ensurePrefab()
        } else {
            ref // prefab is ensured :)
        }
    }

    fun setChildPath(child: PrefabSaveable, index: Int, type: Char) {
        val prefab = prefab
        if (child.prefab == null && prefab != null) {
            child.prefab = prefab
            val nameId = Path.generateRandomId()
            child.prefabPath = prefabPath.added(nameId, index, type)
            // register path in prefab.adds
            prefab.adds.getOrPut(prefabPath, ::ArrayList)
                .add(CAdd(prefabPath, type, child.className, nameId))
            // update all children within child as well
            child.setAllChildPaths()
            // define all prefab.sets
            PrefabHelperWriter(prefab).run(child)
        }
    }

    private fun setAllChildPaths() {
        for (type2 in listChildTypes()) {
            val children = getChildListByType(type2)
            for (ci in children.indices) {
                val child2 = children[ci]
                setChildPath(child2, ci, type2)
            }
        }
    }

    private fun collectPrimaryChanges() {
        // collect all changes, and save them to the prefab
        PrefabHelperWriter(prefab!!).run(this)
    }

    open fun getOptionsByType(type: Char): List<Option>? = null

    override fun addChild(child: PrefabSaveable) {
        val someType = getValidTypesForChild(child).getOrNull(0) ?: ' '
        val index = getChildListByType(someType).size
        addChildByType(index, someType, child)
    }

    override fun addChild(index: Int, child: PrefabSaveable) {
        val someType = getValidTypesForChild(child).getOrNull(0) ?: ' '
        addChildByType(index, someType, child)
    }

    override fun deleteChild(child: PrefabSaveable) {
        for (type in getValidTypesForChild(child)) {
            val list = getChildListByType(type)
            val index = list.indexOf(child)
            if (index >= 0) {
                list as MutableList<*>
                list.removeAt(index)
                break
            }
        }
    }

    fun <V : PrefabSaveable> getInClone(thing: V?, clone: PrefabSaveable): V? {
        thing ?: return null
        val path = thing.prefabPath
        val instance = Hierarchy.getInstanceAt(clone.root, path)
        @Suppress("unchecked_cast")
        return instance as V
    }

    open fun getIndexOf(child: PrefabSaveable): Int {
        for (type in getValidTypesForChild(child)) {
            val list = getChildListByType(type)
            val idx = list.indexOf(child)
            if (idx >= 0) return idx
        }
        return -1
    }

    open fun getValidTypesForChild(child: PrefabSaveable): String = ""

    open fun clone(): PrefabSaveable {
        val clone = this.javaClass.newInstance()
        copyInto(clone)
        return clone
    }

    open fun copyInto(dst: PrefabSaveable) {
        dst.name = name
        dst.description = description
        dst.flags = flags
        dst.prefab = prefab
        dst.prefabPath = prefabPath
    }

    override fun destroy() {}

    @NotSerializedProperty
    override val symbol: String = ""

    @NotSerializedProperty
    override val defaultDisplayName: String
        get() = name

    @NotSerializedProperty
    override val children: List<PrefabSaveable>
        get() {
            val types = listChildTypes()
            return if (types.isEmpty()) emptyList()
            else getChildListByType(types[0])
        }

    override fun createInspector(
        inspected: List<Inspectable>,
        list: PanelListY,
        style: Style,
        getGroup: (nameDesc: NameDesc) -> SettingCategory
    ) {
        val inspector = PrefabInspector.currentInspector
        if (inspector != null) inspector.inspect(inspected.filterIsInstance<PrefabSaveable>(), list, style)
        else super.createInspector(inspected, list, style, getGroup)
    }

    fun setPath(prefab: Prefab?, path: Path) {

        this.prefab = prefab
        this.prefabPath = path

        val childTypes = listChildTypes()
        for (ti in childTypes.indices) {
            val type = childTypes[ti]
            val children = getChildListByType(type)
            for (ci in children.indices) {
                val child = children[ci]
                val childPath = path.added(child.prefabPath.nameId, ci, type)
                child.setPath(prefab, childPath)
            }
        }
    }

    fun throwWarning() {
        val lw = lastWarning
        if (lw != null) throw RuntimeException(lw)
    }

    @DebugAction
    fun selectParent() {
        val parent = parent as? Inspectable ?: return
        ECSSceneTabs.refocus()
        EditorState.select(parent)
    }

    companion object {

        const val DISABLE_FLAG = 1
        private const val NOT_COLLAPSED_FLAG = 2

        private val LOGGER = LogManager.getLogger(PrefabSaveable::class)
        private fun getSuperInstance(className: String): PrefabSaveable {
            return getSample(className) as? PrefabSaveable
                ?: throw RuntimeException("No super instance was found for class '$className'")
        }

        fun <V : PrefabSaveable> getOptionsByClass(parent: PrefabSaveable?, clazz: KClass<V>): List<Option> {
            // registry over all options... / search the raw files + search all scripts? a bit much... maybe in the local folder?
            val knownComponents = getInstanceOf(clazz)
            return knownComponents.map {
                Option(NameDesc(it.key.camelCaseToTitle())) {
                    val comp = it.value.generate() as PrefabSaveable
                    comp.parent = parent
                    comp
                }
            }.sortedBy { it.nameDesc.name }
        }
    }
}