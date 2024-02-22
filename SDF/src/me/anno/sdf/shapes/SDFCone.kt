package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.sign
import kotlin.math.sqrt

// center it, and the pyramid as well?
open class SDFCone : SDFShape() {

    private val params = Vector2f(1f, 2f)

    var radius
        get() = params.x
        set(value) {
            if (params.x != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.x = value
            }
        }

    var height
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.y = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val h = height
        val r = radius
        dst.setMin(-r, 0f, -r)
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
        functions.add(sdCone)
        smartMinBegin(builder, dstIndex)
        builder.append("sdCone(pos").append(trans.posIndex).append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val q = params
        val qx = q.x
        val qy = q.y
        val wx = length(pos.x, pos.z)
        val wy = qy - pos.y
        val af = clamp((wx * q.x + wy * q.y) / q.lengthSquared())
        val ax = wx - qx * af
        val ay = wy - qy * af
        val bx = wx - qx * clamp(wx / qx)
        val by = wy - qy
        val k = sign(qy)
        val d = min(ax * ax + ay * ay, bx * bx + by * by)
        val s = max(k * (wx * qy - wy * qx), k * (wy - qy))
        return sqrt(d) * sign(s)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFCone
        dst.params.set(params)
    }

    override val className: String get() = "SDFCone"

    companion object {
        // from https://iquilezles.org/www/articles/distfunctions/distfunctions.htm, Inigo Quilez
        const val sdCone = "" +
                "float sdCone(vec3 p, vec2 q) {\n" +
                "  vec2 w = vec2(length(p.xz), -p.y+q.y);\n" +
                "  vec2 a = w - q*clamp(dot(w,q)/dot(q,q), 0.0, 1.0);\n" +
                "  vec2 b = w - q*vec2(clamp(w.x/q.x, 0.0, 1.0), 1.0);\n" +
                "  float k = sign(q.y);\n" +
                "  float d = min(dot(a,a),dot(b,b));\n" +
                "  float s = max(k*(w.x*q.y-w.y*q.x),k*(w.y-q.y));\n" +
                "  return sqrt(d)*sign(s);\n" +
                "}"

    }

}