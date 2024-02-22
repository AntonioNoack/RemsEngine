package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

open class SDFHexPrism : SDFSmoothShape() {

    // this maybe should be a 2d shape, but then we couldn't support smoothness correctly

    private val params = Vector2f(1f, 0.1f)

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

    @Range(0.0, 1e38)
    var halfHeight
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                params.y = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val h = halfHeight
        val rz = radius
        val rx = rz * 1.12f //  why is it slightly larger???
        dst.setMin(-rx, -h, -rz)
        dst.setMax(+rx, +h, +rz)
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
        functions.add(sdHexPrism)
        smartMinBegin(builder, dstIndex)
        builder.append("sdHexPrism(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        appendSmoothnessParameter(builder, uniforms)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        // todo looks slightly off
        val h = params
        val magic = SDFHexPrism.Companion.magic
        val k0 = smoothness * min(h.x, h.y)
        val hx = h.x - k0
        val hy = h.y - k0
        val kx = magic.x
        val ky = magic.y
        var px = abs(pos.x)
        val py = abs(pos.y)
        var pz = abs(pos.z)
        val fx = 2f * min(kx * px + ky * pz, 0f)
        px -= fx * kx
        pz -= fx * ky
        val lim = magic.z * hx
        val dx = length(px - clamp(px, -lim, +lim), pz - hx) * sign(pz - hx)
        val dy = py - hy
        return min(max(dx, dy), 0f) + length(max(dx, 0f), max(dy, 0f)) - k0 + pos.w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFHexPrism
        dst.params.set(params)
    }

    override val className: String get() = "SDFHexPrism"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        val magic = Vector3f(-0.8660254f, 0.5f, 0.57735f)
        const val sdHexPrism = "" +
                "float sdHexPrism(vec3 p, vec2 h){\n" +
                "   const vec3 k = vec3(-0.8660254, 0.5, 0.57735);\n" +
                "   p = abs(p);\n" +
                "   p.xz -= 2.0*min(dot(k.xy, p.xz), 0.0)*k.xy;\n" +
                "   vec2 d = vec2(length(p.xz - vec2(clamp(p.x, -k.z*h.x, k.z*h.x), h.x))*sign(p.z - h.x), p.y-h.y);\n" +
                "   return min(max(d.x,d.y),0.0) + length(max(d,0.0));" +
                "}\n" +
                "float sdHexPrism(vec3 p, vec2 h, float k){\n" +
                "   k *= min(h.x,h.y);\n" +
                "   return sdHexPrism(p,h-k)-k;\n" +
                "}\n"
    }

}