package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.image.ImageWriter
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.length
import me.anno.utils.pooling.JomlPools
import org.joml.Vector4f
import kotlin.math.*

class SDFPolygon : SDF2DShape() {

    @Range(3.0, 2e9)
    var points = 5
        set(value) {
            if (field != value) {
                if (!dynamicSize) invalidateShader()
                field = value
            }
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
        functions.add(sdPolygon)
        smartMinBegin(builder, dstName)
        builder.append("sdPolygon(")
        writeFuncInput(builder, trans.posIndex)
        builder.append(',')
        if (dynamicSize) builder.appendUniform(uniforms, GLSLType.V1F) { points.toFloat() }
        else {
            // precompute the parameters for better performance
            val an = (Math.PI / points).toFloat()
            builder.append(an).append(',')
            builder.append("vec2(").append(cos(an)).append(',').append(sin(an)).append(')')
        }
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        val an = (Math.PI / points).toFloat()
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

    override fun clone(): SDFComponent {
        val clone = SDFPolygon()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFPolygon
        clone.points = points
    }

    override val className = "SDFPolygon"

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
                "   float an = 3.141593/n;\n" +
                "   return sdPolygon(p,an,vec2(cos(an),sin(an)));\n" +
                "}\n"

        /** 2d sdf test */
        @JvmStatic
        fun main(args: Array<String>) {
            val size = 512
            val star = SDFPolygon()
            star.scale = size * 0.3f
            ImageWriter.writeImageFloat(size, size, "polygon.png", 0, true) { x, y, _ ->
                val p = JomlPools.vec4f.create()
                val distance = star.computeSDF(p.set(x - size * 0.5f, y - size * 0.5f, 0f, 0f))
                JomlPools.vec4f.sub(1)
                fract(distance * 0.01f) * sign(distance)
            }
        }

    }

}