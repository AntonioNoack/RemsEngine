package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Vector4f

/**
 * transforms signed distances, e.g. by sine waves
 * */
abstract class DistanceMapper : PrefabSaveable() {

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            if (super.isEnabled != value) {
                invalidateShader()
                super.isEnabled = value
            }
        }

    fun invalidateShader() {
        when (val parent = parent) {
            is SDFComponent -> parent.invalidateShader()
            // else -> LOGGER.warn("Incorrect parent: ${parent?.className}")
        }
    }

    fun invalidateBounds() {
        when (val parent = parent) {
            is SDFComponent -> parent.invalidateBounds()
            // else -> LOGGER.warn("Incorrect parent: ${parent?.className}")
        }
    }

    open fun applyTransform(bounds: AABBf) {}

    abstract fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstName: String,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    )

    /**
     * 4th component is the offset to the sdf:
     * - just increase it, if applicable
     * - ignore it otherwise
     * */
    abstract fun calcTransform(pos: Vector4f, distance: Float): Float

    /*companion object {
        private val LOGGER = LogManager.getLogger(DistanceMapper::class)
    }*/

}