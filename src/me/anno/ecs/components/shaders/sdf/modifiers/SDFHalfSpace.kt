package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.writeVec
import me.anno.ecs.components.shaders.sdf.SDFGroup.Companion.sMaxCubic
import me.anno.ecs.components.shaders.sdf.SDFGroup.Companion.smoothMinCubic
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import org.joml.Planef
import org.joml.Vector3f
import org.joml.Vector4f

class SDFHalfSpace() : DistanceMapper() {

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
        dstName: String,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        if (dynamicSmoothness || smoothness > 0f) {
            functions.add(smoothMinCubic)
            builder.append(dstName)
            builder.append(".x=sMaxCubic1(").append(dstName)
            builder.append(".x,dot(vec4(pos").append(posIndex)
            builder.append(",1.0),")
            if (dynamicPlane) builder.append(defineUniform(uniforms, plane))
            else writeVec(builder, plane)
            builder.append("),")
            if (dynamicSmoothness) builder.append(defineUniform(uniforms, GLSLType.V1F, { smoothness }))
            else builder.append(smoothness)
            builder.append(");\n")
        } else {
            builder.append(dstName)
            builder.append(".x=max(").append(dstName)
            builder.append(".x,dot(vec4(pos").append(posIndex)
            builder.append(",1.0),")
            if (dynamicPlane) builder.append(defineUniform(uniforms, plane))
            else writeVec(builder, plane)
            builder.append("));\n")
        }
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        return sMaxCubic(distance, plane.dot(pos), smoothness)
    }

    override fun clone(): SDFHalfSpace {
        val clone = SDFHalfSpace()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFHalfSpace
        clone.plane.set(plane.a, plane.b, plane.c, plane.d)
    }

    override val className: String = "SDFHalfSpace"

    companion object {

        fun Planef.dot(v: Vector4f): Float {
            return v.dot(a, b, c, 0f) + d
        }

        fun Planef.dot(v: Vector3f): Float {
            return v.dot(a, b, c) + d
        }

    }


}