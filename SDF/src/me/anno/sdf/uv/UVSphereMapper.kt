package me.anno.sdf.uv

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.sdf.SDFComponent
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.appendVec
import me.anno.sdf.VariableCounter
import me.anno.sdf.modifiers.PositionMapper
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.PI

class UVSphereMapper : PositionMapper(), UVMapper {

    var center = Vector3f()
        set(value) {
            field.set(value)
            if (!(dynamic || SDFComponent.globalDynamic)) invalidateShader()
        }

    var rotation = Quaternionf()
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
        functions.add(quatRot)
        functions.add(uvSphere)
        val dynamic = dynamic || SDFComponent.globalDynamic
        builder.append("uv=uvSphere(quatRot(pos").append(posIndex).append("-")
        if (dynamic) builder.appendUniform(uniforms, center)
        else builder.appendVec(center)
        builder.append(",")
        if (dynamic) builder.appendUniform(uniforms, rotation)
        else builder.appendVec(rotation)
        builder.append("));\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {}

    companion object {
        private const val uvSphere = "" +
                "vec2 uvSphere(vec3 pos){\n" +
                "   return vec2(atan(pos.x,pos.z)*${0.5 / PI},atan(length(pos.xz),pos.y)*${1.0 / PI});\n" +
                "}\n"
    }
}