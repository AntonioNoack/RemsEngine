package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.gpu.shader.GLSLType
import org.joml.AABBf
import org.joml.Vector4f

class SDFRegular : SDFSmoothShape() {

    enum class Type(val id: Int, val start: Int, val end: Int) {
        OCTAHEDRON(0, 3, 7),
        DODECAHEDRON(1, 13, 19),
        ICOSAHEDRON(2, 3, 13),
    }

    var type = Type.OCTAHEDRON
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
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(sdRegular)
        smartMinBegin(builder, dstName)
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
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        TODO()
    }

    override fun clone(): SDFRegular {
        val clone = SDFRegular()
        copy(clone)
        return clone
    }

    override val className = "SDFRegular"

    companion object {
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