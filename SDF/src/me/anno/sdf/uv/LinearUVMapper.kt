package me.anno.sdf.uv

import me.anno.ecs.components.mesh.TypeValue
import me.anno.sdf.SDFComponent
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.appendVec
import me.anno.sdf.VariableCounter
import me.anno.sdf.modifiers.PositionMapper
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f

// todo not yet working?
//  finalUVs is not showing up
class LinearUVMapper : PositionMapper() {

    var u = Vector4f(0.5f, 0f, 0f, -0.5f)
        set(value) {
            field.set(value)
            if (!(dynamic || SDFComponent.globalDynamic)) invalidateShader()
        }

    var v = Vector4f(0f, 0.5f, 0f, -0.5f)
        set(value) {
            field.set(value)
            if (!(dynamic || SDFComponent.globalDynamic)) invalidateShader()
        }

    var dynamic = false
        set(value) {
            if (field != value && !SDFComponent.globalDynamic) {
                invalidateShader()
            }
            field = value
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {
        val dynamic = dynamic || SDFComponent.globalDynamic
        if (dynamic) {
            builder.append("uv=vec2(dot(")
                .appendUniform(uniforms, u).append(",vec4(pos").append(posIndex).append(",1.0)),dot(")
                .appendUniform(uniforms, v).append(",vec4(pos").append(posIndex).append(",1.0)));\n")
        } else {
            builder.append("uv=vec2(dot(")
                .appendVec(u).append(",vec4(pos").append(posIndex).append(",1.0)),dot(")
                .appendVec(v).append(",vec4(pos").append(posIndex).append(",1.0)));\n")
        }
        return null

    }

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {}

    override val className: String
        get() = "LinearUVMapper"
}