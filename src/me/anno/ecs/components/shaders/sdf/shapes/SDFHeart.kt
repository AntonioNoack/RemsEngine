package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComposer.dot2
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.maths.Maths.SQRT2F
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

@Suppress("unused")
class SDFHeart : SDF2DShape() {

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(dot2)
        functions.add(heartSDF)
        smartMinBegin(builder, dstName)
        builder.append("sdHeart(")
        writeFuncInput(builder, trans.posIndex)
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        val px = abs(pos.x)
        val py = pos.y
        return if (px + py > 1f) sqrt(sq(px - 0.25f, py - 0.75f)) - SQRT2F * 0.25f
        else {
            val di = 0.5f * max(px + py, 0f)
            sqrt(min(sq(px, py - 1f), sq(px - di, py - di))) * sign(px - py)
        }
    }

    override fun clone(): SDFHeart {
        val clone = SDFHeart()
        copy(clone)
        return clone
    }

    override val className = "SDFTorus"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val heartSDF = "" +
                "float sdHeart(vec2 p){\n" +
                "    p.x = abs(p.x);\n" +
                "    if(p.y+p.x>1.0)\n" +
                "        return sqrt(dot2(p-vec2(0.25,0.75))) - sqrt(2.0)/4.0;\n" +
                "    return sqrt(min(dot2(p-vec2(0.00,1.00)),\n" +
                "                    dot2(p-0.5*max(p.x+p.y,0.0)))) * sign(p.x-p.y);\n" +
                "}\n"
    }

}