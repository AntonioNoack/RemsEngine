package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.Path
import me.anno.engine.inspector.IProperty
import me.anno.io.serialization.CachedProperty
import me.anno.maths.Maths.hasFlag
import me.anno.ui.Panel
import me.anno.ui.base.text.TextStyleable
import org.apache.logging.log4j.LogManager
import org.joml.*

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
            (panel as? TextStyleable)?.isBold = true
            val dim = when (value) {
                is Vector2f, is Vector2d -> 2
                is Vector3f, is Vector3d -> 3
                is Vector4f, is Vector4d -> 4
                else -> 0
            }
            // todo relative editing might be useful, too
            //  (e.g. for positions, calculate delta, for scale ratio)
            if (mask != 0 && dim != 0 && !mask.hasFlag((1 shl dim) - 1)) {
                // if mask != -1, and value is a Vector, respect that and only set the corresponding values
                // todo  quaternions are a little special, we need to transfer them by euler angles
                val names = listOf("x", "y", "z", "w")
                val clazz = value!!.javaClass
                val fields = names.subList(0, dim)
                    .filterIndexed { index, _ -> mask.hasFlag(1 shl index) }
                    .map { name -> clazz.getField(name) }
                for (instance in instances) {
                    val value1 = property[instance]
                    for (field in fields) {
                        field.set(value1, field.get(value))
                    }
                    property[instance] = value1
                    pi.change(getPath(instance), instance, name, value1)
                }
            } else {
                for (instance in instances) {
                    property[instance] = value
                    pi.change(getPath(instance), instance, name, value)
                }
            }
            pi.onChange(false)
        } else LOGGER.warn("Cannot modify ${pi.prefab.source}")
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