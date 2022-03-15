package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

open class SDFHexPrism : SDFSmoothShape() {

    private val params = Vector2f(1f, 0.1f)

    var radius
        get() = params.x
        set(value) {
            params.x = value
        }

    var prismHeight
        get() = params.y
        set(value) {
            params.y = value
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: Ptr<Int>,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(sdHexPrism)
        smartMinBegin(builder, dstName)
        builder.append("sdHexPrism(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, params))
        else writeVec(builder, params)
        if (dynamicSmoothness || smoothness > 0f) {
            builder.append(',')
            if (dynamicSmoothness) builder.append(defineUniform(uniforms, GLSLType.V1F, { smoothness }))
            else builder.append(smoothness)
        }
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        val h = params
        val magic = magic
        val k0 = smoothness * min(h.x, h.y)
        val hx = h.x - k0
        val hy = h.y - k0
        val kx = magic.x
        val ky = magic.y
        var px = abs(pos.x)
        var py = abs(pos.z)
        val pz = abs(pos.y)
        val fx = 2f * min(kx * px + ky * py, 0f)
        px -= fx * kx
        py -= fx * ky
        val lim = magic.z * hx
        val dx = length(px - clamp(px, -lim, +lim), py - hx) * sign(py - hx)
        val dy = pz - hy
        return min(max(dx, dy), 0f) + length(max(dx, 0f), max(dy, 0f)) - k0 + pos.w
    }

    override fun clone(): SDFHexPrism {
        val clone = SDFHexPrism()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFHexPrism
        clone.params.set(params)
    }

    override val className = "SDFHexPrism"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        val magic = Vector3f(-0.8660254f, 0.5f, 0.57735f)
        const val sdHexPrism = "" +
                "float sdHexPrism(vec3 p, vec2 h){\n" +
                "   const vec3 k = vec3(-0.8660254, 0.5, 0.57735);\n" +
                "   p = abs(p);\n" +
                "   p.xy -= 2.0*min(dot(k.xy, p.xy), 0.0)*k.xy;\n" +
                "   vec2 d = vec2(length(p.xy - vec2(clamp(p.x, -k.z*h.x, k.z*h.x), h.x))*sign(p.y - h.x), p.z-h.y);\n" +
                "   return min(max(d.x,d.y),0.0) + length(max(d,0.0));" +
                "}\n" +
                "float sdHexPrism(vec3 p, vec2 h, float k){\n" +
                "   k *= min(h.x,h.y);\n" +
                "   return sdHexPrism(p.xzy,h-k)-k;\n" +
                "}\n"
    }

}