package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.sqrt

// center it, and the pyramid as well?
open class SDFRoundCone : SDFShape() {

    private val params: Vector3f = Vector3f(1f, 0f, 2f)
    private val helpers: Vector2f = Vector2f()

    @Range(0.0, 1e38)
    var innerRadius: Float
        get() = params.x
        set(value) {
            if (params.x != value) {
                if (dynamicSize || globalDynamic) invalidateShaderBounds()
                else invalidateShader()
                params.x = value
            }
        }

    @Range(0.0, 1e38)
    var outerRadius: Float
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (dynamicSize || globalDynamic) invalidateShaderBounds()
                else invalidateShader()
                params.y = value
            }
        }

    // must be >= inner radius, if == inner radius, we have a single sphere
    @Range(0.0, 1e38)
    var height: Float
        get() = params.z
        set(value) {
            if (params.z != value) {
                if (dynamicSize || globalDynamic) invalidateShaderBounds()
                else invalidateShader()
                params.z = value
            }
        }

    private fun calcHelpers() {
        helpers.y = (innerRadius - outerRadius) / height
        helpers.x = sqrt(1f - sq(helpers.y))
    }

    override fun calculateBaseBounds(dst: AABBf) {
        val r = max(innerRadius, outerRadius)
        dst.setMin(-r, -innerRadius, -r)
        dst.setMax(+r, outerRadius + height, +r)
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
        functions.add(sdRoundCone)
        smartMinBegin(builder, dstIndex)
        builder.append("sdRoundCone(pos").append(trans.posIndex).append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) {
            builder.appendUniform(uniforms, GLSLType.V2F) {
                calcHelpers()
                helpers
            }.append(',')
            builder.appendUniform(uniforms, params)
        } else {
            calcHelpers()
            builder.appendVec(helpers).append(',')
            builder.appendVec(params)
        }
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        // correct? should be
        val r1 = innerRadius
        val r2 = outerRadius
        val h = height
        calcHelpers()
        val a = helpers.x
        val b = helpers.y
        val qx = length(pos.x, pos.z)
        val qy = pos.y
        val k = qy * a - qx * b
        if (k < 0f) return length(qx, qy) - r1
        if (k > a * h) return length(qx, qy - h) - r2
        return qx * a + qy * b - r1
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFRoundCone) return
        dst.params.set(params)
        dst.helpers.set(helpers)
    }

    companion object {
        // from https://iquilezles.org/www/articles/distfunctions/distfunctions.htm, Inigo Quilez
        const val sdRoundCone = "" +
                "float sdRoundCone(vec3 p, vec2 ab, vec3 rrh) {\n" +
                // sampling dependant computations
                "  vec2 q = vec2(length(p.xz), p.y);\n" +
                "  float k = dot(q,vec2(-ab.y,ab.x));\n" +
                "  if(k<0.0) return length(q) - rrh.x;\n" +
                "  if(k>ab.x*rrh.z) return length(q-vec2(0.0,rrh.z)) - rrh.y;\n" +
                "  return dot(q, ab) - rrh.x;\n" +
                "}"
    }

}