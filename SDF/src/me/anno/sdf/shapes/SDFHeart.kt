package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.SDFComposer.dot2
import me.anno.sdf.VariableCounter
import me.anno.maths.Maths.SQRT2f
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

@Suppress("unused")
class SDFHeart : SDF2DShape() {

    override fun calculateBaseBounds2d(dst: AABBf) {
        dst.setMin(-0.60f, -0.55f, Float.NEGATIVE_INFINITY)
        dst.setMax(+0.60f, +0.55f, Float.POSITIVE_INFINITY)
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
        functions.add(dot2)
        functions.add(heartSDF)
        smartMinBegin(builder, dstIndex)
        builder.append("sdHeart(")
        writeFuncInput(builder, trans.posIndex)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        val px = abs(pos.x)
        val py = pos.y + 0.55f
        return if (px + py > 1f) sqrt(sq(px - 0.25f, py - 0.75f)) - SQRT2f * 0.25f
        else {
            val di = 0.5f * max(px + py, 0f)
            sqrt(min(sq(px, py - 1f), sq(px - di, py - di))) * sign(px - py)
        }
    }

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val heartSDF = "" +
                "float sdHeart(vec2 p){\n" +
                "   p.y += 0.55;\n" + // centering
                "   p.x = abs(p.x);\n" +
                "   if(p.y+p.x>1.0)\n" +
                "       return sqrt(dot2(p-vec2(0.25,0.75))) - sqrt(2.0)/4.0;\n" +
                "   return sqrt(min(dot2(p-vec2(0.00,1.00)),\n" +
                "                   dot2(p-0.5*max(p.x+p.y,0.0)))) * sign(p.x-p.y);\n" +
                "}\n"
    }

}