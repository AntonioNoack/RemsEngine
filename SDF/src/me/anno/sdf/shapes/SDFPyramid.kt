package me.anno.sdf.shapes

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

@Docs("A pyramid shape rendered using signed distance functions")
open class SDFPyramid : SDFShape() {

    private val params: Vector2f = Vector2f(1f, 2f)

    @Docs("Negative height flips the pyramid upside down")
    var height: Float
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.y = value
            }
        }

    @Suppress("unused")
    @Range(0.0, 1e38)
    var baseLength: Float
        get() = params.x * 2f
        set(value) {
            val v2 = value * 0.5f
            if (params.x != v2) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.x = v2
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val r = params.x
        val h = abs(params.y) * 0.5f // negative height flips the pyramid
        dst.setMin(-r, -h, -r)
        dst.setMax(+r, +h, +r)
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
        functions.add(sdPyramid)
        smartMinBegin(builder, dstIndex)
        builder.append("sdPyramid(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val px = max(abs(pos.x), abs(pos.z))
        val params = params
        val qx = params.x
        val qy = params.y
        val py = qy * 0.5f - pos.y
        val f0 = clamp((px * qx + py * qy) / (qx * qx + qy * qy), 0f, 1f)
        val ax = px - qx * f0
        val ay = py - qy * f0
        val f1 = clamp(px / qx, 0f, 1f)
        val bx = px - qx * f1
        val by = py - qy // yes, no *f1
        val s = -sign(qy)
        val a2 = ax * ax + ay * ay
        val b2 = bx * bx + by * by
        val dx = min(a2, b2)
        val dy = min(s * (px * qy - py * qx), s * (py - qy))
        return -sqrt(dx) * sign(dy)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFPyramid) return
        dst.params.set(params)
    }

    companion object {
        const val sdPyramid = "" +
                // not perfect, but better than the hollow pyramid from Inigo Quilez' page
                // from https://iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm, isosceles triangle
                // plus symmetry
                "float sdPyramid(vec3 p2, vec2 q){\n" +
                "   p2.xz = abs(p2.xz);\n" + // not ideal, as it cannot be rounded
                "   vec2 p = vec2(max(p2.x,p2.z),q.y*0.5-p2.y);\n" +
                "   vec2 a = p - q*clamp(dot(p,q)/dot(q,q), 0.0, 1.0);\n" +
                "   vec2 b = p - q*vec2(clamp(p.x/q.x, 0.0, 1.0), 1.0);\n" +
                "   float s = -sign(q.y);\n" +
                "   vec2 d = min(vec2(dot(a,a), s*(p.x*q.y-p.y*q.x)),\n" +
                "                vec2(dot(b,b), s*(p.y-q.y)));\n" +
                "   return -sqrt(d.x)*sign(d.y);\n" +
                "}\n"
    }
}