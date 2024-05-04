package me.anno.sdf.random

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.sdf.uv.UVMapper
import org.joml.Vector4f

// notice: to disable color interpolation, disable linear filtering inside the material :)
class SDFRandomUV : SDFRandom(), UVMapper {

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seed: String
    ): String? {
        builder.append("uv=nextRandF2(").append(seed).append(");\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seed: Int) {}
}