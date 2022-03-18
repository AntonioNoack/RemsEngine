package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector4f

/**
 * transforms signed distances, e.g. by sine waves
 * */
abstract class DistanceMapper : PrefabSaveable() {

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

}