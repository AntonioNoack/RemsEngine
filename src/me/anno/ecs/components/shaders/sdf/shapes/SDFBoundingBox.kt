package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max

class SDFBoundingBox : SDFBox() {

    var dynamicThickness = false
    var thickness = 0.1f

    override fun createSDFShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextIndex: Ptr<Int>,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val (posIndex, scaleName) = createTransformShader(builder, posIndex0, nextIndex, uniforms, functions)
        functions.add(boundingBoxSDF)
        smartMinBegin(builder, dstName)
        builder.append("sdBoundingBox(pos")
        builder.append(posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, halfExtends))
        else writeVec(builder, halfExtends)
        builder.append(',')
        if (dynamicThickness) builder.append(defineUniform(uniforms, GLSLType.V1F, { thickness }))
        else builder.append(thickness)
        if (dynamicSmoothness || smoothness > 0f) {
            builder.append(',')
            if (dynamicSmoothness) builder.append(defineUniform(uniforms, GLSLType.V1F, { smoothness }))
            else builder.append(smoothness)
        }
        builder.append(')')
        smartMinEnd(builder, scaleName)
    }

    private fun lineSDF(x: Float, y: Float, z: Float): Float {
        return length(max(x, 0f), max(y, 0f), max(z, 0f)) + min(max(x, max(y, z)), 0f)
    }

    override fun computeSDF(pos: Vector3f): Float {
        applyTransform(pos)
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
        return min(lineSDF(px, qy, qz), min(lineSDF(qx, py, qz), lineSDF(qx, qy, pz))) - k
    }

    override fun clone(): SDFBoundingBox {
        val clone = SDFBoundingBox()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFBoundingBox
        clone.thickness = thickness
        clone.dynamicThickness = dynamicThickness
    }

    override val className = "SDFBoundingBox"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        private const val boundingBoxSDF = "" +
                "float sdBoundingBox(vec3 p, vec3 b, float e){\n" +
                "        p = abs(p)-b;\n" +
                "   vec3 q = abs(p+e)-e;\n" +
                "   return min(min(\n" +
                "       length(max(vec3(p.x,q.y,q.z),0.0))+min(max(p.x,max(q.y,q.z)),0.0),\n" +
                "       length(max(vec3(q.x,p.y,q.z),0.0))+min(max(q.x,max(p.y,q.z)),0.0)),\n" +
                "       length(max(vec3(q.x,q.y,p.z),0.0))+min(max(q.x,max(q.y,p.z)),0.0));\n" +
                "}\n" +
                "float sdBoundingBox(vec3 p, vec3 b, float e, float k){\n" +
                "   k *= e;\n" + // smoothness delta is proportional to e
                "   return sdBoundingBox(p,b-k,e-k)-k;\n" +
                "}\n"
    }
}