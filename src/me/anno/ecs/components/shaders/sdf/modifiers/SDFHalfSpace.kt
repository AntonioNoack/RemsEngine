package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.writeVec
import me.anno.ecs.components.shaders.sdf.SDFGroup.Companion.sMaxCubic
import me.anno.ecs.components.shaders.sdf.SDFGroup.Companion.smoothMinCubic
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import org.joml.Vector3f
import org.joml.Vector4f

class SDFHalfSpace() : DistanceMapper() {

    constructor(pos: Vector3f) : this() {
        position = pos
        normal = pos
    }

    constructor(pos: Vector3f, dir: Vector3f) : this() {
        position = pos
        normal = dir
    }

    var smoothness = 0.1f
    var dynamicSmoothness = false

    private val params = Vector4f(0f, 1f, 0f, 0f)

    var position = Vector3f()
        set(value) {
            field.set(value)
            params.w = -value.dot(normal)
        }

    var normal = Vector3f(0f, 1f, 0f)
        set(value) {
            field.set(value)
            field.normalize()
            params.x = field.x
            params.y = field.y
            params.z = field.z
            params.w = -field.dot(position)
        }

    var dynamic = false

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstName: String,
        nextVariableId: Ptr<Int>,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        if (dynamicSmoothness || smoothness > 0f) {
            functions.add(smoothMinCubic)
            builder.append(dstName)
            builder.append(".x=sMaxCubic1(").append(dstName)
            builder.append(".x,dot(vec4(pos").append(posIndex)
            builder.append(",1.0),")
            if (dynamic) builder.append(defineUniform(uniforms, params))
            else writeVec(builder, params)
            builder.append("),")
            if (dynamicSmoothness) builder.append(defineUniform(uniforms, GLSLType.V1F, { smoothness }))
            else builder.append(smoothness)
            builder.append(");\n")
        } else {
            builder.append(dstName)
            builder.append(".x=max(").append(dstName)
            builder.append(".x,dot(vec4(pos").append(posIndex)
            builder.append(",1.0),")
            if (dynamic) builder.append(defineUniform(uniforms, params))
            else writeVec(builder, params)
            builder.append("));\n")
        }
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        return sMaxCubic(distance, params.dot(pos.x, pos.y, pos.z, 1f), smoothness)
    }

    override fun clone(): SDFHalfSpace {
        val clone = SDFHalfSpace()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFHalfSpace
    }

    override val className: String = "SDFHalfSpace"


}