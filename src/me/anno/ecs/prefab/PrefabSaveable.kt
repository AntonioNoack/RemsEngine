package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.Path
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.zip.InnerTmpFile
import me.anno.studio.Inspectable
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.structures.Hierarchical
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

abstract class PrefabSaveable : NamedSaveable(), Hierarchical<PrefabSaveable>, Inspectable, Cloneable {

    @SerializedProperty
    override var isEnabled = true

    @NotSerializedProperty // ideally, this would have the default value "depth>3" or root.numChildrenAtDepth(depth)>100
    override var isCollapsed = true

    @NotSerializedProperty
    var lastWarning: String? = null

    // @NotSerializedProperty
    // var prefab: PrefabSaveable? = null
    fun getOriginal(): PrefabSaveable? {
        val sampleInstance = prefab?.getSampleInstance() ?: return null
        return Hierarchy.getInstanceAt(sampleInstance, prefabPath!!)
    }

    fun getOriginalOrDefault() = getOriginal() ?: getSuperInstance(className)

    val refOrNull get() = prefab?.source
    val ref: FileReference
        get() {
            var prefab = prefab
            if (prefab == null) {
                prefab = Prefab(className)
                prefab.source = InnerTmpFile.InnerTmpPrefabFile(prefab)
                prefab._sampleInstance = this
                this.prefab = prefab
            }
            return prefab.source
        }

    /**
     * only defined while building the game
     * */
    @NotSerializedProperty
    var prefab: Prefab? = null

    @NotSerializedProperty
    var prefabPath: Path? = null
        get() {
            if (field != null) return field
            val parent = parent
            if (parent != null) {
                val pp = parent.prefabPath
                if (pp != null) {
                    // find, which type we are
                    for (childType in parent.listChildTypes()) {
                        val children = parent.getChildListByType(childType)
                        val index = children.indexOf(this)
                        if (index >= 0) {
                            // find nameId
                            val nameId = Path.generateRandomId()
                            val newPath = Path(pp, nameId, index, childType)
                            LOGGER.warn("Was missing prefab path, so generated new path $newPath")
                            field = newPath
                            return newPath
                        }
                    }
                    // if we are here, parent must be misconfigured
                }
            }
            return null
        }

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
        writer.writeBoolean("isCollapsed", isCollapsed)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "isCollapsed" -> isCollapsed = value
            else -> super.readBoolean(name, value)
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

    // e.g. "ec" for child entities + child components
    open fun listChildTypes(): String = ""
    open fun getChildListByType(type: Char): List<PrefabSaveable> = emptyList()
    open fun getChildListNiceName(type: Char): String = "Children"
    open fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        LOGGER.warn("$className.addChildByType(index,$type,${child.className}) is not supported")
    }

    open fun getOptionsByType(type: Char): List<Option>? = null

    override fun addChild(child: PrefabSaveable) {
        val type = getTypeOf(child)
        val length = getChildListByType(type).size
        addChildByType(length, type, child)
    }

    override fun addChild(index: Int, child: PrefabSaveable) = addChildByType(index, getTypeOf(child), child)
    override fun deleteChild(child: PrefabSaveable) {
        val list = getChildListByType(getTypeOf(child))
        val index = list.indexOf(child)
        if (index < 0) return
        list as MutableList<*>
        list.remove(child)
    }

    fun <V : PrefabSaveable> getInClone(thing: V?, clone: PrefabSaveable): V? {
        thing ?: return null
        val path = thing.prefabPath ?: return null
        val instance = Hierarchy.getInstanceAt(clone.root, path)
        @Suppress("unchecked_cast")
        return instance as V
    }

    open fun getIndexOf(child: PrefabSaveable): Int = getChildListByType(getTypeOf(child)).indexOf(child)
    open fun getTypeOf(child: PrefabSaveable): Char = ' '

    public abstract override fun clone(): PrefabSaveable
    open fun copy(clone: PrefabSaveable) {
        clone.name = name
        clone.description = description
        clone.isEnabled = isEnabled
        clone.isCollapsed = isCollapsed
        clone.prefab = prefab
        clone.prefabPath = prefabPath
    }

    override fun onDestroy() {}

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
        list: PanelListY, style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        PrefabInspector.currentInspector?.inspect(this, list, style) ?: LOGGER.warn("Missing inspector!")
    }

    fun changePaths(prefab: Prefab?, path: Path) {

        this.prefab = prefab
        this.prefabPath = path

        for (type in listChildTypes()) {
            val children = getChildListByType(type)
            for (index in children.indices) {
                val child = children[index]
                val childPath = path.added(child.name, index, type)
                child.changePaths(prefab, childPath)
            }
        }
    }

    fun throwWarning() {
        val lw = lastWarning
        if (lw != null) throw RuntimeException(lw)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(PrefabSaveable::class)
        private fun getSuperInstance(className: String): PrefabSaveable {
            return ISaveable.getSample(className) as? PrefabSaveable
                ?: throw RuntimeException("No super instance was found for class '$className'")
        }

        fun <V : PrefabSaveable> getOptionsByClass(parent: PrefabSaveable?, clazz: KClass<V>): List<Option> {
            // registry over all options... / search the raw files + search all scripts? a bit much... maybe in the local folder?
            val knownComponents = ISaveable.getInstanceOf(clazz)
            return knownComponents.map {
                Option(it.key.camelCaseToTitle(), "") {
                    val comp = it.value.generator() as PrefabSaveable
                    comp.parent = parent
                    comp
                }
            }.sortedBy { it.title }
        }
    }

}