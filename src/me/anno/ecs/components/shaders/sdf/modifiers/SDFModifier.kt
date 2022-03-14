package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3f

abstract class SDFModifier : PrefabSaveable() {

    // transforms positions, e.g. by sine waves
    abstract fun createTransform(
        builder: StringBuilder,
        posIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    )

    abstract fun applyTransform(pos: Vector3f)

}