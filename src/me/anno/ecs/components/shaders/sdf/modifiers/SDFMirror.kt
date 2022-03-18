package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.writeVec
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.max
import org.joml.Planef
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

class SDFMirror() : PositionMapper() {

    constructor(position: Vector3f) : this(position, position)

    constructor(position: Vector3f, normal: Vector3f) : this() {
        plane.set(normal.x, normal.y, normal.z, -normal.dot(position))
        plane.normalize()
    }

    var smoothness = 0.1f
    var dynamicSmoothness = false

    @Suppress("SetterBackingFieldAssignment")
    var plane = Planef()
        set(value) {
            field.set(value.a, value.b, value.c, value.d)
            field.normalize()
        }

    var dynamicPlane = false

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        // I - 2.0 * dot(N, I) * N
        val tmpIndex = nextVariableId.next()
        val normal = if (dynamicPlane) defineUniform(uniforms, plane) else {
            val name = "nor${nextVariableId.next()}"
            builder.append("vec4 ").append(name)
            builder.append("=")
            writeVec(builder, plane)
            builder.append(";\n")
            name
        }
        builder.append("float tmp").append(tmpIndex).append("=dot(vec4(")
        builder.append("pos").append(posIndex)
        builder.append(",1.0),").append(normal)
        builder.append(");\n")
        if (dynamicSmoothness || smoothness > 0f) {
            builder.append("pos").append(posIndex)
            builder.append("=mix(pos").append(posIndex)
            builder.append(",pos").append(posIndex)
            builder.append("-2.0*tmp").append(tmpIndex)
            builder.append("*").append(normal)
            builder.append(".xyz,clamp(tmp").append(tmpIndex)
            builder.append("*")
            if (dynamicSmoothness) {
                builder.append(defineUniform(uniforms, GLSLType.V1F, { 1f / max(abs(smoothness), 1e-10f) }))
            } else builder.append(1f / smoothness)
            builder.append("+0.5,0.0,1.0));\n")
        } else {
            builder.append("if(tmp").append(tmpIndex).append("<0.0) pos").append(posIndex)
            builder.append("-=2.0*tmp").append(tmpIndex).append("*").append(normal).append(".xyz;\n")
        }
        return null
    }

    override fun calcTransform(pos: Vector4f) {
        TODO("Not yet implemented")
    }

    override fun clone(): SDFMirror {
        val clone = SDFMirror()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFMirror
        clone.smoothness = smoothness
        clone.dynamicSmoothness = dynamicSmoothness
        clone.plane = plane
        clone.dynamicPlane = dynamicPlane
    }

    override val className = "SDFMirror"

}