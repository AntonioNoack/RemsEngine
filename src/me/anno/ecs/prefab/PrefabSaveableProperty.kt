package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.Path
import me.anno.engine.inspector.CachedProperty
import me.anno.engine.inspector.IProperty
import me.anno.ui.Panel
import me.anno.ui.base.text.TextStyleable
import org.apache.logging.log4j.LogManager

/**
 * IProperty for PrefabSaveables
 * */
class PrefabSaveableProperty(
    val pi: PrefabInspector,
    val instances: List<PrefabSaveable>,
    val name: String,
    val property: CachedProperty
) : IProperty<Any?> {

    private fun getPath(instance: PrefabSaveable): Path {
        val path = instance.prefabPath
        // the index may not be set in the beginning
        val li = path.size - 1
        if (li >= 0 && path.lastIndex() < 0) {
            path.index = instance.parent!!.getIndexOf(instance)
        }
        return path
    }

    override fun init(panel: Panel?) {
        (panel as? TextStyleable)?.isBold = instances.any {
            pi.isChanged(getPath(it), name)
        }
    }

    override fun getDefault(): Any? {
        // info("default of $name: ${component.getDefaultValue(name)}")
        return instances.first().getDefaultValue(name)
    }

    override fun set(panel: Panel?, value: Any?, mask: Int) {
        if (pi.prefab.isWritable) {
            LOGGER.info("Setting $name to $value, styleable? ${panel is TextStyleable}, instances: ${instances.size}")
            (panel as? TextStyleable)?.isBold = true
            for (instance in instances) {
                PropertySetter.setPropertyRespectingMask(value, mask, property, instance)
                pi.change(getPath(instance), instance, name, value)
            }
            // todo relative editing might be useful, too
            //  (e.g. for positions, calculate delta, for scale ratio)
            pi.onChange(false)
        } else LOGGER.warn("Cannot modify ${pi.prefab.sourceFile}")
    }

    override fun get(): Any? {
        return property[instances.first()]
    }

    override fun reset(panel: Panel?): Any? {
        (panel as? TextStyleable)?.isBold = false
        // info("reset $name")
        val value = getDefault()
        for (instance in instances) {
            pi.reset(getPath(instance), name)
            property[instance] = value
        }
        return value
    }

    override val annotations: List<Annotation>
        get() = property.annotations

    companion object {
        private val LOGGER = LogManager.getLogger(PrefabSaveableProperty::class)
    }
}