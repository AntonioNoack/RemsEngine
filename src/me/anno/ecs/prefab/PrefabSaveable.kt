package me.anno.ecs.prefab

import me.anno.io.NamedSaveable
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.utils.LOGGER
import me.anno.utils.structures.Hierarchical

abstract class PrefabSaveable : NamedSaveable(), Hierarchical<PrefabSaveable>, Inspectable {

    @SerializedProperty
    override var isEnabled = true

    @NotSerializedProperty
    override var isCollapsed = false

    @NotSerializedProperty
    var prefab: PrefabSaveable? = null

    @NotSerializedProperty
    override var parent: PrefabSaveable? = null

    private fun getSuperParent(): PrefabSaveable {
        return prefab ?: this::class.java.getConstructor().newInstance()
    }

    fun getDefaultValue(name: String): Any? {
        return getSuperParent()[name]
    }

    fun resetProperty(name: String): Any? {
        // how do we find the default value, if the root is null? -> create an empty copy
        val parent = getSuperParent()
        val reflections = getReflections()
        val defaultValue = reflections[parent, name]
        reflections[this, name] = defaultValue
        LOGGER.info("Reset $className/$name to $defaultValue")
        return defaultValue
    }

    fun pathInRoot(root: PrefabSaveable? = null): ArrayList<Int> {
        if (this == root) return ArrayList()
        val parent = parent
        return if (parent != null) {
            val index = parent.indexOf(this)
            val list = parent.pathInRoot()
            list.add(index)
            return list
        } else ArrayList()
    }

    fun pathInRoot2(root: PrefabSaveable? = null, withExtra: Boolean): Pair<IntArray, CharArray> {
        val path = pathInRoot(root)
        if (withExtra) {
            path.add(-1)
            path.add(0)
        }
        val size = path.size / 2
        val ia = IntArray(size) { path[it * 2] }
        val ta = CharArray(size) { path[it * 2 + 1].toChar() }
        return ia to ta
    }

    // e.g. "ec" for child entities + child components
    abstract fun listChildTypes(): String
    abstract fun getChildListByType(type: Char): List<PrefabSaveable>
    abstract fun getChildListNiceName(type: Char): String
    abstract fun addChildByType(index: Int, type: Char, instance: PrefabSaveable)
    open fun getOptionsByType(type: Char): List<Option>? = null

    // index + 256 * type
    abstract fun indexOf(child: PrefabSaveable): Int

    override val symbol: String = ""
    override val defaultDisplayName: String get() = name

    override val children: List<PrefabSaveable>
        get() = getChildListByType(listChildTypes()[0])

    override fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        PrefabInspector.currentInspector?.inspect(this, list, style) ?: LOGGER.warn("Missing inspector!")
    }

}