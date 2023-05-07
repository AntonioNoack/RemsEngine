package me.anno.sdf.modifiers

import me.anno.sdf.SDFComponent
import me.anno.ecs.prefab.PrefabSaveable
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

}