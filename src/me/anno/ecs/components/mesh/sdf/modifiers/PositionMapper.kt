package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f

/**
 * transforms positions, e.g., by sine waves
 * */
abstract class PositionMapper : SDFMapper() {

    /**
     * @return the name of the sd offset variable (or null if no offset exists)
     * */
    abstract fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String?

    /**
     * applies the transform to a vector, e.g., rotation or such
     * @param pos position plus depth offset
     * */
    abstract fun calcTransform(pos: Vector4f, seeds: IntArrayList)

}