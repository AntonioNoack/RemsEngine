package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.sqrt

open class SDFDeathStar : SDFShape() {

    private val params = Vector3f(1f)

    @Range(0.0, 1e38)
    var radius
        get() = params.x
        set(value) {
            if (params.x != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.x = value
            }
        }

    @Suppress("unused")
    @Range(0.0, 1e38)
    var cutoffRadius
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.y = value
            }
        }

    // must be >= inner radius, if == inner radius, we have a single sphere
    var distance
        get() = params.z
        set(value) {
            if (params.z != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.z = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        // incorrect for second huge sphere
        val r = params.x
        dst.setMin(-r, -r, -r)
        dst.setMax(+r, +r, +r)
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
        functions.add(sdDeathStar)
        smartMinBegin(builder, dstIndex)
        builder.append("sdDeathStar(pos").append(trans.posIndex).append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val p = params
        val ra = p.x
        val rb = p.y
        val d = p.z
        val px = pos.x
        val py = length(pos.y, pos.z)
        val ra2 = ra * ra
        val a = (ra2 - rb * rb + d * d) / (2f * d)
        val b = sqrt(max(ra2 - a * a, 0f))
        return if (px * b - py * a > d * max(b - py, 0f)) length(px - a, py - b)
        else max(length(px, py) - ra, -length(px - d, py) - rb)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFDeathStar
        dst.params.set(params)
    }

    companion object {
        // from https://iquilezles.org/www/articles/distfunctions/distfunctions.htm, Inigo Quilez
        // https://www.shadertoy.com/view/7lVXRt
        const val sdDeathStar = "" +
                "float sdDeathStar(vec3 p2, vec3 r) {\n" +
                "   float ra = r.x, rb = r.y, d = r.z;\n" +
                "   vec2 p = vec2(p2.x, length(p2.yz));\n" +
                "   float a = (ra*ra - rb*rb + d*d)/(2.0*d);\n" +
                "   float b = sqrt(max(ra*ra-a*a,0.0));\n" +
                "   if(p.x*b-p.y*a > d*max(b-p.y,0.0)) return length(p-vec2(a,b));\n" +
                "   else return max((length(p)-ra),\n" +
                "                  -(length(p-vec2(d,0))-rb));\n" +
                "}"

    }

}