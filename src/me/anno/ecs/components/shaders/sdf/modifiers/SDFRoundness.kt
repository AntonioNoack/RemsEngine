package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import org.joml.Vector4f

/**
 * makes the object thicker, and makes it round by doing so
 * */
class SDFRoundness : DistanceMapper() {

    var roundness = 0.1f
    var dynamic = false

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstName: String,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        builder.append(dstName).append(".x-=")
        if (dynamic) builder.append(defineUniform(uniforms, GLSLType.V1F, { roundness }))
        else builder.append(roundness)
        builder.append(";\n")
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        return distance - roundness
    }

    override fun clone(): SDFRoundness {
        val clone = SDFRoundness()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFRoundness
        clone.roundness = roundness
        clone.dynamic = dynamic
    }
}