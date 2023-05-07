package me.anno.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.sdf.SDFComponent
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.AABBf
import org.joml.Vector4f

/**
 * transforms signed distances, e.g., by sine waves
 * */
abstract class DistanceMapper : SDFMapper() {

    abstract fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    )

    /**
     * @param pos position plus depth offset
     * @param distance input distance
     * @return output distance
     * */
    abstract fun calcTransform(pos: Vector4f, distance: Float): Float

}