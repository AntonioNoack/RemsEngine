package me.anno.sdf.modifiers

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.sdf.SDFComponent
import org.joml.AABBf

/**
 * transforms signed distances, e.g. by sine waves
 * */
abstract class SDFMapper : PrefabSaveable() {

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            if (super.isEnabled != value) {
                invalidateShader()
                super.isEnabled = value
            }
        }

    fun invalidateShader() {
        (parent as? SDFComponent)?.invalidateShader()
    }

    fun invalidateBounds() {
        (parent as? SDFComponent)?.invalidateBounds()
    }

    open fun applyTransform(bounds: AABBf) {}

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        if (!setSerializableProperty(name, value)) {
            super.setProperty(name, value)
        }
    }
}