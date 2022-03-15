package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.writeVec
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

class SDFHalfSpace : DistanceMapper() {

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
        builder.append(dstName)
        builder.append(".x=max(").append(dstName)
        builder.append(".x,dot(vec4(pos").append(posIndex)
        builder.append(",0.0),")
        if (dynamic) builder.append(defineUniform(uniforms, params))
        else writeVec(builder, params)
        builder.append("));\n")
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        return max(distance, params.dot(pos.x, pos.y, pos.z, 0f))
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