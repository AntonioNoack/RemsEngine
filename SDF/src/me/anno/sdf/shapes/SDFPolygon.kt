package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * Regular polygon, flat shape.
 * */
class SDFPolygon : SDF2DShape() {

    @Range(3.0, 2e9)
    var points = 5
        set(value) {
            if (field != value) {
                if (!(dynamicSize || globalDynamic)) invalidateShader()
                field = value
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
        functions.add(sdPolygon)
        smartMinBegin(builder, dstIndex)
        builder.append("sdPolygon(")
        writeFuncInput(builder, trans.posIndex)
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, GLSLType.V1F) { points.toFloat() }
        else {
            // precompute the parameters for better performance
            val an = (PI / points).toFloat()
            builder.append(an).append(',')
            builder.append("vec2(").append(cos(an)).append(',').append(sin(an)).append(')')
        }
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val an = (PI / points).toFloat()
        val can = cos(an)
        val san = sin(an)
        var px = pos.x
        var py = pos.y
        // until here, the values could be cached :)
        val bn = mod(atan2(px, py), (2f * an)) - an
        val pl = length(px, py)
        px = pl * cos(bn) - can
        py = pl * abs(sin(bn)) - san
        py += clamp(-py, 0f, san)
        return length(px, py) * sign(px)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFPolygon
        dst.points = points
    }

    override val className: String get() = "SDFPolygon"

    companion object {
        private const val sdPolygon = "" +
                // signed distance to a n-star polygon
                "float sdPolygon(vec2 p, float an, vec2 acs) {\n" +
                // reduce to first sector
                "   float bn = mod(atan(p.x,p.y),2.0*an) - an;\n" +
                "   p = length(p)*vec2(cos(bn),abs(sin(bn)));\n" +
                // line sdf
                "   p -= acs;\n" +
                "   p.y += clamp(-p.y, 0.0, acs.y);\n" +
                "   return length(p)*sign(p.x);\n" +
                "}\n" +
                "float sdPolygon(vec2 p, float n) {\n" +
                "   float an = PI/n;\n" +
                "   return sdPolygon(p,an,vec2(cos(an),sin(an)));\n" +
                "}\n"
    }
}