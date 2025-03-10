package me.anno.sdf.modifiers

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.clamp
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.defineUniform
import me.anno.sdf.VariableCounter
import me.anno.sdf.modifiers.SDFTwist.Companion.twistFunc
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.AABBf
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.atan2
import kotlin.math.round

class SDFRotSym : PositionMapper() {

    var rotation: Quaternionf = Quaternionf()
        set(value) {
            field.set(value)
            field.normalize()
            invalidateBounds()
        }

    var slices: Float = 1f
        set(value) {
            field = value
            invalidateBounds()
        }

    var offset: Vector3f = Vector3f()
        set(value) {
            field.set(value)
            invalidateBounds()
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {
        functions.add(twistFunc)
        functions.add(quatRot)
        functions.add(rotSymFunc)
        builder.append("pos").append(posIndex)
        builder.append("=rotSym(pos").append(posIndex)
        val offset = defineUniform(uniforms, offset)
        builder.append('-').append(offset)
        builder.append(',').appendUniform(uniforms, rotation)
        builder.append(',').appendUniform(uniforms, GLSLType.V1F) { slices * invTau }
        builder.append(")+").append(offset).append(";\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {
        val offset = offset
        val pos3 = JomlPools.vec3f.borrow()
        pos3.set(pos).sub(offset)
        rotation.transform(pos3)
        var angle = atan2(pos3.z, pos3.x)
        val slices = slices
        val invSlice = slices * invTau
        if (slices < 1e9) {
            angle = round(angle * invSlice) / invSlice
        }
        // todo is this the correct direction?
        pos3.rotateY(-angle)
        rotation.transformInverse(pos3)
        pos3.add(offset)
        pos.set(pos3, pos.w)
    }

    override fun applyTransform(bounds: AABBf) {
        // around the axis, apply this transform x times, and union with the points...
        val slices = clamp(slices.roundToIntOr(1), 1, 8)
        val quat = JomlPools.quat4f.create().set(rotation)
        val quatInv = JomlPools.quat4f.create().set(quat).invert()
        val pos = JomlPools.vec3f.create()
        val dst = JomlPools.aabbf.create().clear()
        val offset = offset
        for (i in 0 until slices) {
            val angle = PIf * 2f * i / slices
            for (j in 0 until 8) {
                val ox = if (j.and(1) != 0) bounds.minX else bounds.maxX
                val oy = if (j.and(2) != 0) bounds.minY else bounds.maxY
                val oz = if (j.and(4) != 0) bounds.minZ else bounds.maxZ
                pos.set(ox, oy, oz).sub(offset)
                // surely this could be packed into a single rotation...
                quatInv.transform(pos)
                pos.rotateY(angle)
                quat.transform(pos)
                pos.add(offset)
                dst.union(pos)
            }
        }
        //dst.translate(offset)
        bounds.set(dst)
        JomlPools.vec3f.sub(1)
        JomlPools.aabbf.sub(1)
        JomlPools.quat4f.sub(2)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFRotSym) return
        dst.rotation.set(rotation)
        dst.offset.set(offset)
        dst.slices = slices
    }

    companion object {

        const val invTau = (1.0 / TAU).toFloat()

        const val rotSymFunc = "" +
                "vec3 rotSym(vec3 p, vec4 q, float invSlice){\n" +
                "   p = quatRot(p,q);\n" +
                "   float angle = atan(p.z, p.x);\n" +
                "   if(invSlice < 1e9) angle = round(angle * invSlice) / invSlice;\n" +
                "   float c = cos(angle), s = sin(angle);\n" +
                "   p.xz = mat2(c,-s,s,c) * p.xz;\n" +
                "   p = quatRotInv(p,q);\n" +
                "   return p;\n" +
                "}\n"
    }
}