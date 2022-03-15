package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import org.joml.Vector3f
import org.joml.Vector4f

open class SDFPlane : SDFShape() {

    var direction = Vector3f(0f, 1f, 0f)
        set(value) {
            field.set(value)
            field.normalize()
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: Ptr<Int>,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        smartMinBegin(builder, dstName)
        builder.append("dot(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, direction))
        else writeVec(builder, direction)
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        val dir = direction
        return pos.dot(dir.x, dir.y, dir.z, 1f)
    }

    override fun clone(): SDFPlane {
        val clone = SDFPlane()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFPlane
        clone.direction.set(direction)
    }

    override val className = "SDFPlane"

}