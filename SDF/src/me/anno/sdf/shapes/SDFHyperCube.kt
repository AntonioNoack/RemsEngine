package me.anno.sdf.shapes

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cuboid in 4D.
 * */
open class SDFHyperCube : SDFSmoothShape() {

    var halfExtends = Vector4f(1f)
        set(value) {
            if (dynamicSize || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    /**
     * rotation in 4th dimension
     * */
    var rotation4d = Quaternionf()
        set(value) {
            field.set(value)
            field.toEulerAnglesRadians(rotation4di)
        }

    /**
     * rotation in 4th dimension;
     * rotation in radians, euler angles, YXZ order
     * */
    @HideInInspector
    @NotSerializedProperty
    var rotation4di = Vector3f()
        set(value) {
            field.set(value)
            field.toQuaternionRadians(rotation4d)
        }

    /**
     * value of 4th coordinate
     * */
    @Range(-2.0, 2.0)
    var w = 0f

    override fun calculateBaseBounds(dst: AABBf) {
        val h = halfExtends
        val s = h.length()
        dst.setMin(-s, -s, -s)
        dst.setMax(+s, +s, +s)
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
        functions.add(hyperProjection)
        functions.add(sdBox4)
        smartMinBegin(builder, dstIndex)
        builder.append("sdBox4(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize || globalDynamic) builder.appendUniform(uniforms, halfExtends)
        else builder.appendVec(halfExtends)
        builder.append(',')
        builder.appendUniform(uniforms, rotation4di)
        builder.append(',')
        builder.appendUniform(uniforms, GLSLType.V1F) { w }
        if (dynamicSmoothness || globalDynamic || smoothness > 0f) {
            builder.append(',')
            builder.appendUniform(uniforms, GLSLType.V1F) { smoothness }
        }
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {

        val c = smoothness
        val b = halfExtends
        val x = pos.x
        val y = pos.y
        val z = pos.z
        val w = pos.w
        pos.w = this.w
        invProject(pos, rotation4di)

        val qx = abs(pos.x) - b.x + c
        val qy = abs(pos.y) - b.y + c
        val qz = abs(pos.z) - b.z + c
        val qw = abs(pos.w) - b.w + c

        pos.set(x, y, z, w)

        val outer = length(max(0f, qx), max(0f, qy), max(0f, qz), max(0f, qw))
        val inner = min(max(max(qx, qy), max(qz, qw)), 0f)
        return outer + inner - c + w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFHyperCube
        dst.halfExtends = halfExtends
        dst.rotation4d = rotation4d
        dst.w = w
    }

    override val className: String get() = "SDFHyperCube"

    companion object {

        fun invProject(p: Vector4f, r: Vector3f): Vector4f {
            p.rotateYW(r.y)
            p.rotateXW(r.x)
            p.rotateZW(r.z)
            return p
        }

        fun Vector4f.rotateXW(angle: Float): Vector4f {
            val c = cos(angle)
            val s = sin(angle)
            return set(x * c - w * s, y, z, x * s + w * c)
        }

        fun Vector4f.rotateYW(angle: Float): Vector4f {
            val c = cos(angle)
            val s = sin(angle)
            return set(x, y * c - w * s, z, y * s + w * c)
        }

        fun Vector4f.rotateZW(angle: Float): Vector4f {
            val c = cos(angle)
            val s = sin(angle)
            return set(x, y, z * c - w * s, z * s + w * c)
        }

        const val hyperProjection = "" +
                // rotate point from 3d into 4d
                "vec4 invProject(vec3 p3, float w, vec3 r){\n" +
                "   vec4 p = vec4(p3, w);\n" +
                "   p.yw *= rot(r.y);\n" +
                "   p.xw *= rot(r.x);\n" +
                "   p.zw *= rot(r.z);\n" +
                "   return p;\n" +
                "}\n"

        const val sdBox4 = "" +
                "float sdBox4(vec3 p, vec4 b, float w, vec3 r){\n" +
                "  vec4 d = abs(invProject(p, w, r)) - b;\n" +
                "  return min(max(max(d.x,d.y),max(d.z,d.w)),0.0) + length(max(d,0.0));\n" +
                "}\n" +
                "float sdBox4(vec3 p, vec4 b, vec3 r, float w, float c){\n" +
                "  vec4 q = abs(invProject(p, w, r)) - b + c;\n" +
                "  return min(max(max(q.x,q.y),max(q.z,q.w)),0.0) + length(max(q,0.0)) - c;\n" +
                "}\n"
    }
}