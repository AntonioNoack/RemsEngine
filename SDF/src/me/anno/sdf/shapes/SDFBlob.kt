package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.maths.Maths.PHIf
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.length
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SDFBlob : SDFShape() {

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
        functions.add(sdBlob)
        smartMinBegin(builder, dstIndex)
        builder.append("sdBlob(pos")
        builder.append(trans.posIndex)
        builder.append(")")
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun calculateBaseBounds(dst: AABBf) {
        val s = 1.5f
        dst.setMin(-s, -s, -s)
        dst.setMax(+s, +s, +s)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        var px = abs(pos.x)
        var py = abs(pos.y)
        var pz = abs(pos.z)
        if (px < max(py, pz)) {
            val t = px
            px = py
            py = pz
            pz = t
        }
        if (px < max(py, pz)) {
            val t = px
            px = py
            py = pz
            pz = t
        }
        val b = max(
            max(v0.dot(px, py, pz), v1.dot2(px, pz)),
            max(v2.dot2(py, px), v2.dot2(px, pz)),
        )
        val l = length(px, py, pz)
        return l - 1.5f - vx * cos(min(sqrt(1.01f - b / l) * (PIf * 4f), PIf))
    }

    companion object {

        private val v0 = Vector3f(1f).normalize()
        private val v1 = Vector2f(PHIf + 1f, 1f).normalize()
        private val v2 = Vector2f(1f, PHIf).normalize()
        private const val vx = 0.2f * 0.75f

        // from https://mercury.sexy/hg_sdf
        const val sdBlob = "" +
                "float sdBlob(vec3 p){\n" +
                "   p = abs(p);\n" +
                "   if (p.x < max(p.y, p.z)) p = p.yzx;\n" +
                "   if (p.x < max(p.y, p.z)) p = p.yzx;\n" +
                "   float b = max(max(max(\n" +
                "       dot(p, normalize(vec3(1.0))),\n" +
                "       dot(p.xz, normalize(vec2(PHI+1.0, 1.0)))),\n" +
                "       dot(p.yx, normalize(vec2(1.0, PHI)))),\n" +
                "       dot(p.xz, normalize(vec2(1.0, PHI))));\n" +
                "   float l = length(p);\n" +
                "   return l - 1.5 - $vx * cos(min(sqrt(1.01 - b / l)*(PI * 4.0), PI));" +
                "}\n"
    }
}