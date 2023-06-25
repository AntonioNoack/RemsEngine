package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.maths.Maths.length
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.sqrt

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

        fun distanceToSphere(pos: Vector3f, dir: Vector3f, radius: Float): Float {
            val t = dir.dot(pos.x, pos.y, pos.z)
            val q = pos.lengthSquared() - radius * radius
            val disc = t * t - q
            if (disc < 0f) return Float.POSITIVE_INFINITY
            return -t - sqrt(disc)
        }

        fun distanceToSphere(pos: Vector4f, dir: Vector3f, radius: Float): Float {
            val t = dir.dot(pos.x, pos.y, pos.z)
            val q = pos.lengthSquared() - radius * radius
            val disc = t * t - q
            if (disc < 0f) return Float.POSITIVE_INFINITY
            return -t - sqrt(disc)
        }

        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdSphere = "" +
                "float sdSphere(vec3 p, float s){ return length(p)-s; }\n" +
                "float sddSphere(vec3 p, vec3 d, float s){\n" +
                "   float t = dot(p,d);\n" +
                "   float q = dot(p,p)-s*s;\n" +
                "   float disc = t*t-q;\n" +
                "   if(disc < 0.0) return 1e38;\n" +
                "   return -t - sqrt(disc);\n" +
                "}\n" +
                "float sdSphere2(vec3 p, vec3 d, float s){ float d0 = sdSphere(p,s); return d0 > 0.0 ? min(max(d0*2.0,0.03*s),sddSphere(p,d,s)) : d0; }\n"
    }

}