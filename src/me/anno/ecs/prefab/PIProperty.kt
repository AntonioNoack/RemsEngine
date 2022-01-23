package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.Path
import me.anno.engine.IProperty
import me.anno.io.serialization.CachedProperty
import me.anno.ui.base.Panel
import me.anno.ui.base.text.TextStyleable

class PIProperty(
    val pi: PrefabInspector,
    val instance: PrefabSaveable,
    val name: String,
    val property: CachedProperty
) : IProperty<Any?> {

    private fun getPath(): Path? {
        val path = instance.prefabPath ?: return null
        // the index may not be set in the beginning
        val li = path.size - 1
        if (li >= 0 && path.lastIndex() < 0) {
            path.index = instance.parent!!.getIndexOf(instance)
        }
        return path
    }

    override fun init(panel: Panel?) {
        (panel as? TextStyleable)?.setBold(pi.isChanged(getPath(), name))
    }

    override fun getDefault(): Any? {
        // info("default of $name: ${component.getDefaultValue(name)}")
        return instance.getDefaultValue(name)
    }

    override fun set(panel: Panel?, value: Any?) {
        (panel as? TextStyleable)?.setBold()
        // info("setting value of $name, ${panel is TextStyleable}")
        property[instance] = value
        pi.change(getPath(), instance, name, value)
    }

    override fun get(): Any? {
        return property[instance]
    }

    override fun reset(panel: Panel?): Any? {
        (panel as? TextStyleable)?.unsetBold()
        // info("reset $name")
        pi.reset(getPath(), name)
        val value = getDefault()
        property[instance] = value
        return value
    }

    override val annotations: List<Annotation>
        get() = property.annotations

}