package me.anno.ecs.components.mesh.sdf.arrays

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFGroup
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.AABBf
import org.joml.Vector3f

// todo implement tri and hex arrays
abstract class SDFGroupArray : SDFGroup() {

    // must be > 0 to be useful
    var overlap = Vector3f(1f)
        set(value) {
            field.set(value)
        }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFGroupArray
        clone.overlap = overlap
    }

    abstract fun defineLoopHead(
        builder: StringBuilder,
        posIndex0: Int,
        innerDstIndex: Int,
        outerDstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): Int

    abstract fun defineLoopFoot(
        builder: StringBuilder,
        posIndex0: Int,
        innerDstIndex: Int,
        outerDstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    )

    abstract fun applyArrayTransform(bounds: AABBf)

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val tmp = nextVariableId.next()
        builder.append("vec4 res").append(tmp).append(";\n")
        // start loops
        val posIndex = defineLoopHead(builder, posIndex0, tmp, dstIndex, nextVariableId, uniforms, functions, seeds)
        // build group contents
        super.buildShader(builder, posIndex, nextVariableId, tmp, uniforms, functions, seeds)
        // finish loops
        defineLoopFoot(builder, posIndex0, tmp, dstIndex, nextVariableId, uniforms, functions, seeds)
    }

}