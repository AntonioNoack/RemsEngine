package me.anno.sdf.shapes

import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.MinMax.max
import me.anno.maths.MinMax.min
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.sign
import kotlin.math.sqrt

@Docs("A cone (pyramid with circle base) centered at half-height")
open class SDFCone : SDFShape() {

    private val params = Vector2f(1f, 2f)

    var axis = Axis.Y
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    var radius
        get() = params.x
        set(value) {
            if (params.x != value) {
                params.x = value
                if (dynamicSize || globalDynamic) invalidateShaderBounds()
                else invalidateShader()
            }
        }

    var height
        get() = params.y
        set(value) {
            if (params.y != value) {
                params.y = value
                if (dynamicSize || globalDynamic) invalidateShaderBounds()
                else invalidateShader()
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val h = height * 0.5
        val r = radius
        dst.setMin(-r, -r, -r)
        dst.setMax(+r, +r, +r)
        dst.setComp(axis.id, -h)
        dst.setComp(axis.id + 3, +h)
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
        builder.append("sdCone(pos").append(trans.posIndex)
        when (axis) {
            Axis.X -> builder.append(".yxz")
            Axis.Z -> builder.append(".yzx")
            else -> {}
        }
        builder.append(',')
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
        val wx = when (axis) {
            Axis.X -> length(pos.y, pos.z)
            Axis.Y -> length(pos.x, pos.z)
            Axis.Z -> length(pos.x, pos.y)
        }
        val wy = qy * 0.5f - pos[axis.id]
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
        if (dst !is SDFCone) return
        dst.params.set(params)
        dst.axis = axis
    }

    companion object {
        // from https://iquilezles.org/www/articles/distfunctions/distfunctions.htm, Inigo Quilez
        const val sdCone = "" +
                "float sdCone(vec3 p, vec2 q) {\n" +
                "  vec2 w = vec2(length(p.xz), q.y*0.5-p.y);\n" +
                "  vec2 a = w - q*clamp(dot(w,q)/dot(q,q), 0.0, 1.0);\n" +
                "  vec2 b = w - q*vec2(clamp(w.x/q.x, 0.0, 1.0), 1.0);\n" +
                "  float k = sign(q.y);\n" +
                "  float d = min(dot(a,a),dot(b,b));\n" +
                "  float s = max(k*(w.x*q.y-w.y*q.x),k*(w.y-q.y));\n" +
                "  return sqrt(d)*sign(s);\n" +
                "}"
    }
}