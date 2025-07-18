package me.anno.sdf.shapes

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * plane in x/y/z-direction, can be rotated ofc
 * */
open class SDFPlane : SDFShape() {

    var axis = Axis.Y
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        when (axis) {// I hope this infinity causes no issues...
            Axis.X -> {
                dst.minX = Float.NEGATIVE_INFINITY
                dst.maxX = 0f
                dst.minY = Float.NEGATIVE_INFINITY
                dst.maxY = Float.POSITIVE_INFINITY
                dst.minZ = Float.NEGATIVE_INFINITY
                dst.maxZ = Float.POSITIVE_INFINITY
            }
            Axis.Y -> {
                dst.minX = Float.NEGATIVE_INFINITY
                dst.maxX = Float.POSITIVE_INFINITY
                dst.minY = Float.NEGATIVE_INFINITY
                dst.maxY = 0f
                dst.minZ = Float.NEGATIVE_INFINITY
                dst.maxZ = Float.POSITIVE_INFINITY
            }
            Axis.Z -> {
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
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
        smartMinBegin(builder, dstIndex)
        builder.append("pos")
        builder.append(trans.posIndex)
        builder.append('.').append('x' + axis.id)
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        return pos[axis.id]
    }

    override fun raycast(
        origin: Vector3f, direction: Vector3f,
        near: Float, far: Float,
        maxSteps: Int,
        sdfReliability: Float,
        maxRelativeError: Float,
        seeds: IntArrayList
    ): Float {
        val t = origin[axis.id] / direction[axis.id]
        return if (t in near..far) t else Float.POSITIVE_INFINITY
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFPlane) return
        dst.axis = axis
    }
}