package me.anno.sdf.modifiers

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.appendVec
import me.anno.sdf.SDFComponent.Companion.globalDynamic
import me.anno.sdf.SDFGroup.Companion.sMaxCubic
import me.anno.sdf.SDFGroup.Companion.smoothMinCubic
import me.anno.sdf.VariableCounter
import me.anno.sdf.modifiers.SDFMirror.Companion.normalize3
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import org.joml.AABBf
import org.joml.Planef
import org.joml.Vector3f
import org.joml.Vector4f

class SDFHalfSpace() : DistanceMapper() {

    constructor(position: Vector3f) : this(position, position)

    constructor(position: Vector3f, normal: Vector3f) : this() {
        plane.set(normal.x, normal.y, normal.z, -normal.dot(position))
        plane.normalize3()
    }

    @Range(0.0, 1e38)
    var smoothness = 0.1f
        set(value) {
            if (field != value) {
                if (dynamicSmoothness || globalDynamic) invalidateBounds()
                else invalidateShader()
                field = value
            }
        }

    var dynamicSmoothness = false
        set(value) {
            if (field != value) {
                if(!globalDynamic) invalidateShader()
                field = value
            }
        }

    @Suppress("SetterBackingFieldAssignment")
    var plane = Planef(0f, 1f, 0f, 0f)
        set(value) {
            if (dynamicPlane || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value.a, value.b, value.c, value.d)
            field.normalize3()
        }

    var dynamicPlane = false
        set(value) {
            if (field != value) {
                if (!globalDynamic) invalidateShader()
                field = value
            }
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val dynamicPlane = dynamicPlane || globalDynamic
        val dynamicSmoothness = dynamicSmoothness || globalDynamic
        if (dynamicSmoothness || smoothness > 0f) {
            functions.add(smoothMinCubic)
            builder.append("res").append(dstIndex)
            builder.append(".x=sMaxCubic1(res").append(dstIndex)
            builder.append(".x,dot(vec4(pos").append(posIndex)
            builder.append(",1.0),")
            if (dynamicPlane) builder.appendUniform(uniforms, plane)
            else builder.appendVec(plane)
            builder.append("),")
            if (dynamicSmoothness) builder.appendUniform(uniforms, GLSLType.V1F) { smoothness }
            else builder.append(smoothness)
            builder.append(");\n")
        } else {
            builder.append("res").append(dstIndex)
            builder.append(".x=max(res").append(dstIndex)
            builder.append(".x,dot(vec4(pos").append(posIndex)
            builder.append(",1.0),")
            if (dynamicPlane) builder.appendUniform(uniforms, plane)
            else builder.appendVec(plane)
            builder.append("));\n")
        }
    }

    override fun applyTransform(bounds: AABBf) {
        // like mirror, just with one side
        val imx = bounds.minX
        val imy = bounds.minY
        val imz = bounds.minZ
        val ixx = bounds.maxX
        val ixy = bounds.maxY
        val ixz = bounds.maxZ
        bounds.clear()
        val normal = plane
        for (i in 0 until 8) {
            var x = if (i.and(1) == 0) imx else ixx
            var y = if (i.and(2) == 0) imy else ixy
            var z = if (i.and(4) == 0) imz else ixz
            val dot = normal.a * x + normal.b * y + normal.c * z + normal.d
            if (dot > 0f) {// if on wrong side, project points onto plane
                x -= dot * normal.a
                y -= dot * normal.b
                z -= dot * normal.c
            }
            bounds.union(x, y, z)
        }
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        return sMaxCubic(distance, plane.dot(pos), smoothness)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFHalfSpace
        dst.plane = plane
    }

    override val className: String get() = "SDFHalfSpace"

    companion object {

        fun Planef.dot(v: Vector4f): Float {
            return v.dot(a, b, c, 0f) + d
        }

        fun Planef.dot(v: Vector3f): Float {
            return v.dot(a, b, c) + d
        }

    }


}