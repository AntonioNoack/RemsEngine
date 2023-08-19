package me.anno.sdf.shapes

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import me.anno.sdf.VariableCounter
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max

class SDFHyperBBox() : SDFHyperCube() {

    var dynamicThickness = false
        set(value) {
            if (field != value) {
                field = value
                if (!globalDynamic) invalidateShader()
            }
        }

    @Range(0.0, 1e300)
    var thickness = 0.1f
        set(value) {
            if (field != value) {
                if (dynamicThickness || globalDynamic) invalidateBounds()
                else invalidateShader()
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
        functions.add(hyperProjection)
        functions.add(boundingBoxSDF4)
        smartMinBegin(builder, dstIndex)
        builder.append("sdBoundingBox4(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        builder.appendUniform(uniforms, rotation4d)
        builder.append(',')
        builder.appendUniform(uniforms, GLSLType.V1F) { w }
        builder.append(',')
        val dynamicSize = dynamicSize || globalDynamic
        if (dynamicSize) builder.appendUniform(uniforms, halfExtends)
        else builder.appendVec(halfExtends)
        builder.append(',')
        val dynamicThickness = dynamicThickness || globalDynamic
        if (dynamicThickness) builder.appendUniform(uniforms, GLSLType.V1F) { thickness }
        else builder.append(thickness)
        appendSmoothnessParameter(builder, uniforms)
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    private fun lineSDF(x: Float, y: Float, z: Float): Float {
        return length(max(x, 0f), max(y, 0f), max(z, 0f)) + min(max(x, max(y, z)), 0f)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        // todo not correct, just 3d
        val thickness = thickness
        val b = halfExtends
        val k = smoothness * thickness
        val bx = b.x - k
        val by = b.y - k
        val bz = b.z - k
        val e = thickness - k
        val px = abs(pos.x) - bx
        val py = abs(pos.y) - by
        val pz = abs(pos.z) - bz
        val qx = abs(px + e) - e
        val qy = abs(py + e) - e
        val qz = abs(pz + e) - e
        return min(lineSDF(px, qy, qz), min(lineSDF(qx, py, qz), lineSDF(qx, qy, pz))) - k + pos.w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFHyperBBox
        dst.thickness = thickness
        dst.dynamicThickness = dynamicThickness
    }

    override val className: String get() = "SDFHyperBBox"

    companion object {
        const val boundingBoxSDF4 = "" +
                "float sdBoundingBox4(vec3 k, vec3 r, float w, vec4 b, float e){\n" +
                "   vec4 p = invProject(k,w,r);\n" +
                "        p = abs(p)-b;\n" +
                "   vec4 q = abs(p+e)-e;\n" +
                "   return min(min(min(\n" +
                "       length(max(vec4(p.x,q.y,q.z,q.w),0.0))+min(max(p.x,max(q.y,max(q.z,q.w))),0.0),\n" +
                "       length(max(vec4(q.x,p.y,q.z,q.w),0.0))+min(max(q.x,max(p.y,max(q.z,q.w))),0.0)),\n" +
                "       length(max(vec4(q.x,q.y,p.z,q.w),0.0))+min(max(q.x,max(q.y,max(p.z,q.w))),0.0)),\n" +
                "       length(max(vec4(q.x,q.y,q.z,p.w),0.0))+min(max(q.x,max(q.y,max(q.z,p.w))),0.0));\n" +
                "}\n" +
                "float sdBoundingBox4(vec3 p, vec3 r, float w, vec4 b, float e, float k){\n" +
                "   k *= e;\n" + // smoothness delta is proportional to e
                "   return sdBoundingBox4(p,r,w,b-k,e-k)-k;\n" +
                "}\n"
    }
}