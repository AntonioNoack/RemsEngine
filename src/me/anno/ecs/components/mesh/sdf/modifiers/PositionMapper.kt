package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Vector4f

/**
 * transforms positions, e.g. by sine waves
 * */
abstract class PositionMapper : PrefabSaveable() {

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

    /**
     * returns the name of the sd offset variable (or null if no offset exists)
     * */
    abstract fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String?

    /**
     * 4th component is the offset to the sdf:
     * - just increase it, if applicable
     * - ignore it otherwise
     * */
    abstract fun calcTransform(pos: Vector4f)

    /*companion object {
        private val LOGGER = LogManager.getLogger(PositionMapper::class)
    }*/

}