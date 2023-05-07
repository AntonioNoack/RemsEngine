package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.sdf.VariableCounter
import me.anno.maths.Maths.length
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f

class SDFSphere : SDFShape() {

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
        functions.add(sdSphere)
        smartMinBegin(builder, dstIndex)
        builder.append("sdSphere2(pos").append(trans.posIndex).append(",dir").append(trans.posIndex).append(",1.0)")
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        return length(pos.x, pos.y, pos.z) - 1f + pos.w
    }

    override val className: String get() = "SDFSphere"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdSphere = "" +
                "float sdSphere(vec3 p, float s){ return length(p)-s; }\n" +
                "float sddSphere(vec3 p, vec3 d, float s){ " +
                "   vec3 q = p - dot(p, d) * d;\n" +
                "   float f = dot(q,q);\n" +
                "   float edge = s*s - f;\n" +
                "   vec3 qp = q-p;\n" +
                "   if(edge < 0.0 || dot(qp,d) <= 0.0) return 1e38;\n" +
                "   return length(qp) - sqrt(edge);\n" +
                "}\n" +
                "float sdSphere2(vec3 p, vec3 d, float s){ float d0 = sdSphere(p,s); return d0 > 0.0 ? min(max(d0*2.0,0.03*s),sddSphere(p,d,s)) : d0; }\n"
    }

}