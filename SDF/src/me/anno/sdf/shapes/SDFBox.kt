package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

/**
 * Cuboid
 * */
open class SDFBox : SDFSmoothShape() {

    var halfExtends = Vector3f(1f)
        set(value) {
            if (dynamicSize || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var forMorphing = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val h = halfExtends
        dst.setMin(-h.x, -h.y, -h.z)
        dst.setMax(+h.x, +h.y, +h.z)
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
        functions.add(sdBox)
        smartMinBegin(builder, dstIndex)
        if (forMorphing) {
            builder.append("sdBox(pos").append(trans.posIndex)
        } else {
            builder.append("sddBox(pos").append(trans.posIndex).append(",dir").append(trans.posIndex)
        }
        builder.append(',')
        if (dynamicSize || globalDynamic) builder.appendUniform(uniforms, halfExtends)
        else builder.appendVec(halfExtends)
        if (dynamicSmoothness || globalDynamic || smoothness > 0f) {
            builder.append(',').appendUniform(uniforms, GLSLType.V1F) { smoothness }
        }
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val r = smoothness
        val b = halfExtends
        val qx = abs(pos.x) - b.x + r
        val qy = abs(pos.y) - b.y + r
        val qz = abs(pos.z) - b.z + r
        val outer = length(max(0f, qx), max(0f, qy), max(0f, qz))
        val inner = min(max(qx, max(qy, qz)), 0f)
        return outer + inner - r + pos.w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFBox) return
        dst.halfExtends.set(halfExtends)
    }

    companion object {

        fun sdBox(px: Float, py: Float, pz: Float, hx: Float, hy: Float, hz: Float): Float {
            val qx = abs(px) - hx
            val qy = abs(py) - hy
            val qz = abs(pz) - hz
            val outer = length(max(0f, qx), max(0f, qy), max(0f, qz))
            val inner = min(max(qx, max(qy, qz)), 0f)
            return outer + inner
        }

        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdBox = "" +
                "float udBox(float p, float b){\n" +
                "  return max(abs(p) - b,0.0);\n" +
                "}\n" +
                "float udBox(vec2 p, vec2 b){\n" +
                "  vec2 d = abs(p) - b;\n" +
                "  return length(max(d,0.0));\n" +
                "}\n" +
                "float udBox(vec3 p, vec3 b){\n" +
                "  vec3 d = abs(p) - b;\n" +
                "  return length(max(d,0.0));\n" +
                "}\n" +
                "float sdBox(float p, float b){\n" +
                "  return abs(p) - b;\n" +
                "}\n" +
                "float sddBox(float p, float d, float b){\n" +
                "   return abs(p)-b;\n" +
                "}\n" +
                "float sdBox(vec2 p, vec2 b){\n" +
                "  vec2 d = abs(p) - b;\n" +
                "  return min(max(d.x,d.y),0.0) + length(max(d,0.0));\n" +
                "}\n" +
                "float sddBox(vec2 p, vec2 d, vec2 b){\n" +
                "   vec2 dist = (abs(p)-b)/abs(d);\n" +
                "   return max(dist.x,dist.y);\n" +
                "}\n" +
                "float sdBox(vec3 p, vec3 b){\n" +
                "  vec3 d = abs(p) - b;\n" +
                "  return min(max(d.x,max(d.y,d.z)),0.0) + length(max(d,0.0));\n" +
                "}\n" +
                // adjusted from AABB-ray intersection;
                // much quicker traversal
                "float sddBox(vec3 p, vec3 d, vec3 b){\n" +
                "   vec3 dist = (abs(p)-b)/abs(d);\n" +
                "   return max(dist.x,max(dist.y,dist.z));\n" +
                "}\n" +
                "float sdBox(vec3 p, vec3 b, float r){\n" +
                "  vec3 q = abs(p) - b + r;\n" +
                "  return length(max(q,0.0)) + min(max(q.x,max(q.y,q.z)),0.0) - r;\n" +
                "}\n" +
                "float sddBox(vec3 p, vec3 d, vec3 b, float r){\n" +
                "   return r > 0.0 ? sdBox(p,b,r) : sddBox(p,d,b);\n" +
                "}\n"
    }
}