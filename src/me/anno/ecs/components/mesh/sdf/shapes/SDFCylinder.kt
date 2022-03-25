package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max

open class SDFCylinder : SDFSmoothShape() {

    private val params = Vector2f(1f)

    var radius
        get() = params.x
        set(value) {
            if(params.x != value && !dynamicSize) invalidateShader()
            params.x = value
        }

    var halfHeight
        get() = params.y
        set(value) {
            if(params.y != value && !dynamicSize) invalidateShader()
            params.y = value
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(sdCylinder)
        smartMinBegin(builder, dstName)
        builder.append("sdCylinder(pos")
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
        val k = smoothness
        val hkx = h.x - k
        val hky = h.y - k
        val dx = abs(length(pos.x, pos.z)) - hkx
        val dy = abs(pos.y) - hky
        return min(max(dx, dy), 0f) + length(max(dx, 0f), max(dy, 0f)) - k + pos.w
    }

    override fun clone(): SDFCylinder {
        val clone = SDFCylinder()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFCylinder
        clone.params.set(params)
    }

    override val className = "SDFCylinder"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdCylinder = "" +
                "float sdCylinder(vec3 p, vec2 h){\n" +
                "   vec2 d = vec2(length(p.xz),abs(p.y)) - h;\n" +
                "   return min(max(d.x,d.y),0.0) + length(max(d,0.0));\n" +
                "}\n" +
                "float sdCylinder(vec3 p, vec2 h, float k){\n" +
                "   return sdCylinder(p,h-k)-k;\n" +
                "}\n"
    }

}