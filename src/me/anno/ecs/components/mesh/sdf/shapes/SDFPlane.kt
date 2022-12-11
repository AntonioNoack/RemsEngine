package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.AABBf
import org.joml.Vector4f

/**
 * plane in x/y/z-direction, can be rotated ofc
 * */
open class SDFPlane : SDFShape() {

    // todo should be an enum
    @Docs("Allowed values: x/y/z")
    var axis = 'y'
        set(value) {
            if (field != value && value in "xyz") {
                invalidateShader()
                field = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        when (axis) {// I hope this infinity causes no issues...
            'x' -> {
                dst.minX = Float.NEGATIVE_INFINITY
                dst.maxX = 0f
                dst.minY = Float.NEGATIVE_INFINITY
                dst.maxY = Float.POSITIVE_INFINITY
                dst.minZ = Float.NEGATIVE_INFINITY
                dst.maxZ = Float.POSITIVE_INFINITY
            }
            'y' -> {
                dst.minX = Float.NEGATIVE_INFINITY
                dst.maxX = Float.POSITIVE_INFINITY
                dst.minY = Float.NEGATIVE_INFINITY
                dst.maxY = 0f
                dst.minZ = Float.NEGATIVE_INFINITY
                dst.maxZ = Float.POSITIVE_INFINITY
            }
            'z' -> {
                dst.minX = Float.NEGATIVE_INFINITY
                dst.maxX = Float.POSITIVE_INFINITY
                dst.minY = Float.NEGATIVE_INFINITY
                dst.maxY = Float.POSITIVE_INFINITY
                dst.minZ = Float.NEGATIVE_INFINITY
                dst.maxZ = 0f
            }
        }

    }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        smartMinBegin(builder, dstIndex)
        builder.append("pos")
        builder.append(trans.posIndex)
        builder.append('.').append(axis)
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        return pos.y
    }

    override fun clone(): SDFPlane {
        val clone = SDFPlane()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFPlane
        clone.axis = axis
    }

    override val className get() = "SDFPlane"

}