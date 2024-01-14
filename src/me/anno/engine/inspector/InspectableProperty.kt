package me.anno.engine.inspector

import me.anno.io.serialization.CachedProperty
import me.anno.ui.Panel

/**
 * IProperty for Inspectables
 * */
class InspectableProperty(
    val instances: List<Inspectable>,
    val property: CachedProperty,
    val cleanInstance: Inspectable?
) : IProperty<Any?> {

    override fun init(panel: Panel?) {}

    override fun getDefault(): Any? {
        return if (cleanInstance != null) property[cleanInstance] else null
    }

    override fun set(panel: Panel?, value: Any?, mask: Int) {
        for (instance in instances) {
            property[instance] = value
        }
    }

    override fun get(): Any? {
        return property[instances.first()]
    }

    override fun reset(panel: Panel?): Any? {
        val value = getDefault()
        for (instance in instances) {
            property[instance] = value
        }
        return value
    }

    override val annotations: List<Annotation>
        get() = property.annotations
}