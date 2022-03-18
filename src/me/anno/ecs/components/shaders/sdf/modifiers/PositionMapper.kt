package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector4f

/**
 * transforms positions, e.g. by sine waves
 * */
abstract class PositionMapper : PrefabSaveable() {

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

}