package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.shader.GLSLType
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.debug.TestStudio.Companion.testUI
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

open class SDFHyperCube : SDFSmoothShape() {

    var halfExtends = Vector4f(1f)
        set(value) {
            if (dynamicSize || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var rotation4d = Vector3f()
        set(value) {
            field.set(value)
        }

    @Range(-2.0, 2.0)
    var w = 0f

    override fun calculateBaseBounds(dst: AABBf) {
        val h = halfExtends
        val s = h.length()
        dst.setMin(-s, -s, -s)
        dst.setMax(+s, +s, +s)
    }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
        functions.add(hyperProjection)
        functions.add(sdBox4)
        smartMinBegin(builder, dstIndex)
        builder.append("sdBox4(pos")
        builder.append(trans.posIndex)
        builder.append(',')
        if (dynamicSize || globalDynamic) builder.appendUniform(uniforms, halfExtends)
        else builder.appendVec(halfExtends)
        builder.append(',')
        builder.appendUniform(uniforms, rotation4d)
        builder.append(',')
        builder.appendUniform(uniforms, GLSLType.V1F) { w }
        if (dynamicSmoothness || globalDynamic || smoothness > 0f) {
            builder.append(',')
            builder.appendUniform(uniforms, GLSLType.V1F) { smoothness }
        }
        builder.append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, trans)
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        // todo not correct, just 3d
        val r = smoothness
        val b = halfExtends
        val qx = abs(pos.x) - b.x + r
        val qy = abs(pos.y) - b.y + r
        val qz = abs(pos.z) - b.z + r
        val outer = length(max(0f, qx), max(0f, qy), max(0f, qz))
        val inner = min(max(qx, max(qy, qz)), 0f)
        return outer + inner - r + pos.w
    }

    override fun clone(): SDFHyperCube {
        val clone = SDFHyperCube()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFHyperCube
        clone.halfExtends.set(halfExtends)
        clone.rotation4d.set(rotation4d)
        clone.w = w
    }

    override val className = "SDFHyperCube"

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            testUI {
                val e = SDFHyperCube()
                registerCustomClass(e)
                testScene(Entity().apply { add(e) })
            }
        }

        const val hyperProjection = "" +
                "mat2 rot(float angle){\n" +
                "   float c = cos(angle), s = sin(angle);\n" +
                "   return mat2(c,-s,+s,c);\n" +
                "}\n" +
                // rotate point from 3d into 4d
                "vec4 invProject(vec3 p3, float w, vec3 r){\n" +
                "   vec4 p = vec4(p3, w);\n" +
                "   p.yw *= rot(r.y);\n" +
                "   p.xw *= rot(r.x);\n" +
                "   p.zw *= rot(r.z);\n" +
                "   return p;\n" +
                "}\n"

        const val sdBox4 = "" +
                "float sdBox4(vec3 p, vec4 b, float w, vec3 r){\n" +
                "  vec4 d = abs(invProject(p, w, r)) - b;\n" +
                "  return min(max(max(d.x,d.y),max(d.z,d.w)),0.0) + length(max(d,0.0));\n" +
                "}\n" +
                "float sdBox4(vec3 p, vec4 b, vec3 r, float w, float c){\n" +
                "  vec4 q = abs(invProject(p, w, r)) - b + c;\n" +
                "  return min(max(max(q.x,q.y),max(q.z,q.w)),0.0) + length(max(q,0.0)) - c;\n" +
                "}\n"
    }

}