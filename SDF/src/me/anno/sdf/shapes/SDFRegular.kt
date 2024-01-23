package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.PHIf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.pow
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

class SDFRegular : SDFSmoothShape() {

    enum class Type(val id: Int, val start: Int, val end: Int) {
        OCTAHEDRON(0, 3, 7),
        DODECAHEDRON(1, 13, 19),
        ICOSAHEDRON(2, 3, 13),
    }

    var type: Type = Type.OCTAHEDRON
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val s = when (type) {
            Type.OCTAHEDRON -> 1.732051f // sqrt(3)
            Type.DODECAHEDRON -> 1.2f // guess
            Type.ICOSAHEDRON -> 1.1f
        }
        dst.setMin(-s, -s, -s)
        dst.setMax(+s, +s, +s)
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
        functions.add(sdRegular)
        smartMinBegin(builder, dstIndex)
        builder.append("sdRegular(pos")
        builder.append(trans.posIndex).append(',')
        val dynamic = dynamicSmoothness || globalDynamic
        if (dynamic || smoothness > 0f) {
            if (dynamic) builder.appendUniform(uniforms, GLSLType.V1F) { smoothness }
            else builder.append(smoothness)
            builder.append(',')
        }
        builder.append(type.start).append(',')
        builder.append(type.end).append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        var d = 0f
        val smoothness = smoothness
        if (smoothness > 0f) {
            for (i in type.start until type.end) {
                d += pow(abs(pos.dot(constants[i])), smoothness)
            }
            d = pow(d, 1f / smoothness)
        } else {
            for (i in type.start until type.end) {
                d = max(d, abs(pos.dot(constants[i])))
            }
        }
        return d - 1f
    }

    override val className: String get() = "SDFRegular"

    companion object {

        private fun Vector4f.dot(o: Vector3f): Float {
            return o.dot(x, y, z)
        }

        private val constants = listOf(
            normalize(1f, 0f, 0f),
            normalize(0f, 1f, 0f),
            normalize(0f, 0f, 1f),

            normalize(+1f, 1f, 1f),
            normalize(-1f, 1f, 1f),
            normalize(1f, -1f, +1f),
            normalize(1f, +1f, -1f),

            normalize(0f, +1f, PHIf + 1f),
            normalize(0f, -1f, PHIf + 1f),
            normalize(+PHIf + 1f, 0f, 1f),
            normalize(-PHIf - 1f, 0f, 1f),
            normalize(+1f, PHIf + 1f, 0f),
            normalize(-1f, PHIf + 1f, 0f),

            normalize(0f, +PHIf, 1f),
            normalize(0f, -PHIf, 1f),
            normalize(+1f, 0f, PHIf),
            normalize(-1f, 0f, PHIf),
            normalize(+PHIf, 1f, 0f),
            normalize(-PHIf, 1f, 0f)
        )

        fun normalize(a: Float, b: Float, c: Float): Vector3f {
            return Vector3f(a, b, c).normalize()
        }

        // from https://mercury.sexy/hg_sdf
        const val sdRegular = "" +
                "const vec3 GDFVectors[19] = vec3[](\n" +
                "   normalize(vec3(1.0, 0.0, 0.0)),\n" +
                "   normalize(vec3(0.0, 1.0, 0.0)),\n" +
                "   normalize(vec3(0.0, 0.0, 1.0)),\n" +

                "   normalize(vec3(+1.0, 1.0, 1.0)),\n" +
                "   normalize(vec3(-1.0, 1.0, 1.0)),\n" +
                "   normalize(vec3(1.0, -1.0, +1.0)),\n" +
                "   normalize(vec3(1.0, +1.0, -1.0)),\n" +

                "   normalize(vec3(0.0, +1.0, PHI+1.0)),\n" +
                "   normalize(vec3(0.0, -1.0, PHI+1.0)),\n" +
                "   normalize(vec3(+PHI+1.0, 0.0, 1.0)),\n" +
                "   normalize(vec3(-PHI-1.0, 0.0, 1.0)),\n" +
                "   normalize(vec3(+1.0, PHI+1.0, 0.0)),\n" +
                "   normalize(vec3(-1.0, PHI+1.0, 0.0)),\n" +

                "   normalize(vec3(0.0, +PHI, 1.0)),\n" +
                "   normalize(vec3(0.0, -PHI, 1.0)),\n" +
                "   normalize(vec3(+1.0, 0.0, PHI)),\n" +
                "   normalize(vec3(-1.0, 0.0, PHI)),\n" +
                "   normalize(vec3(+PHI, 1.0, 0.0)),\n" +
                "   normalize(vec3(-PHI, 1.0, 0.0))\n" +
                ");\n" +
                "float sdRegular(vec3 p, int begin, int end){\n" +
                "   float d = 0.0;\n" +
                "   for (int i = ZERO + begin; i < end; i++)\n" +
                "       d = max(d, abs(dot(p, GDFVectors[i])));\n" +
                "   return d-1.0;\n" +
                "}\n" +
                // slow, but allows for rounding
                "float sdRegular(vec3 p, float e, int begin, int end){\n" +
                "   if(e <= 0.0) return sdRegular(p, begin, end);\n" +
                "   float d = 0.0;\n" +
                "   for (int i = ZERO + begin; i < end; i++)\n" +
                "       d += pow(abs(dot(p, GDFVectors[i])), e);\n" +
                "   return pow(d, 1.0/e)-1.0;\n" +
                "}\n"
    }

}