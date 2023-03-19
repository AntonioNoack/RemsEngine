package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.image.ImageWriter
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.*

class SDFStar : SDF2DShape() {

    private val params = Vector2f(5f, 0.5f)

    @Range(3.0, 2e9)
    var points
        get() = params.x.toInt()
        set(value) {
            val v = value.toFloat()
            if (v != params.x) {
                if (!(dynamicSize || globalDynamic)) invalidateShader()
                params.x = v
            }
        }

    @Range(0.0, 1.0)
    var indent
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (!(dynamicSize || globalDynamic)) invalidateShader()
                params.y = value
            }
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
        functions.add(sdStar)
        smartMinBegin(builder, dstIndex)
        builder.append("sdStar(")
        writeFuncInput(builder, trans.posIndex)
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, params)
        else {
            // precompute the parameters for better performance
            val n = params.x
            val m = mix(2f, n, params.y)
            val an = (PI / n).toFloat()
            val en = (PI / m).toFloat()
            builder.append(an).append(',')
            builder.append("vec2(").append(cos(an)).append(',').append(sin(an)).append("),")
            builder.append("vec2(").append(cos(en)).append(',').append(sin(en)).append(')')
        }
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val n = params.x
        val m = mix(2f, n, params.y)
        val an = (PI / n).toFloat()
        val en = (PI / m).toFloat()
        val can = cos(an)
        val san = sin(an)
        val cen = cos(en)
        val sen = sin(en)
        var px = pos.x
        var py = pos.y
        // until here, the values could be cached :)
        val bn = mod(atan2(px, py), (2f * an)) - an
        val pl = length(px, py)
        px = pl * cos(bn) - can
        py = pl * abs(sin(bn)) - san
        val max = san / sen
        val dot2 = clamp(-(px * cen + py * sen), 0f, max)
        px += cen * dot2
        py += sen * dot2
        return length(px, py) * sign(px)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFStar
        clone.params.set(params)
    }

    override val className get() = "SDFStar"

    companion object {

        private const val sdStar = "" +
                // signed distance to a n-star polygon
                "float sdStar(vec2 p, float an, vec2 acs, vec2 ecs) {\n" +
                // reduce to first sector
                "   float bn = mod(atan(p.x,p.y),2.0*an) - an;\n" +
                "   p = length(p)*vec2(cos(bn),abs(sin(bn)));\n" +
                // line sdf
                "   p -= acs;\n" +
                "   p += ecs*clamp(-dot(p,ecs), 0.0, acs.y/ecs.y);\n" +
                "   return length(p)*sign(p.x);\n" +
                "}\n" +
                "float sdStar(vec2 p, float n, float inner) {\n" +
                // these 4 lines can be precomputed for a given shape
                "   float m = mix(2.0, n, inner);\n" +
                "   float an = PI/n;\n" +
                "   float en = PI/m;\n" +
                "   vec2  acs = vec2(cos(an),sin(an));\n" +
                // ecs=vec2(0,1) and simplify, for regular polygon
                "   vec2  ecs = vec2(cos(en),sin(en));\n" +
                // reduce to first sector
                "   return sdStar(p,an,acs,ecs);\n" +
                "}\n" +
                "float sdStar(vec2 p, vec2 a){ return sdStar(p,a.x,a.y); }\n"

    }

}