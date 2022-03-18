package me.anno.ecs.components.shaders.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

open class SDFBox : SDFSmoothShape() {

    var halfExtends = Vector3f(1f)
        set(value) {
            field.set(value)
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
        functions.add(sdBox)
        smartMinBegin(builder, dstName)
        builder.append("sdBox(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize) builder.append(defineUniform(uniforms, halfExtends))
        else writeVec(builder, halfExtends)
        if (dynamicSmoothness || smoothness > 0f) {
            builder.append(',')
            builder.append(defineUniform(uniforms, GLSLType.V1F, { smoothness }))
        }
        builder.append(')')
        smartMinEnd(builder, dstName, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        applyTransform(pos)
        val r = smoothness
        val b = halfExtends
        val qx = abs(pos.x) - b.x + r
        val qy = abs(pos.y) - b.y + r
        val qz = abs(pos.z) - b.z + r
        val outer = length(max(0f, qx), max(0f, qy), max(0f, qz))
        val inner = min(max(qx, max(qy, qz)), 0f)
        return outer + inner - r + pos.w
    }

    override fun clone(): SDFBox {
        val clone = SDFBox()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFBox
        clone.halfExtends.set(halfExtends)
    }

    override val className = "SDFBox"

    companion object {
        // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
        const val sdBox = "" +
                "float sdBox(vec3 p, vec3 b){\n" +
                "  vec3 d = abs(p) - b;\n" +
                "  return min(max(d.x,max(d.y,d.z)),0.0) + length(max(d,0.0));\n" +
                "}\n" +
                "float sdBox(vec3 p, vec3 b, float r){\n" +
                "  vec3 q = abs(p) - b + r;\n" +
                "  return length(max(q,0.0)) + min(max(q.x,max(q.y,q.z)),0.0) - r;\n" +
                "}\n"
    }

}