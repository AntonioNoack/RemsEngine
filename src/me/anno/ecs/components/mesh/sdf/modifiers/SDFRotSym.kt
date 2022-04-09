package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.quatRot
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.SDFTwist.Companion.twistFunc
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.atan2
import kotlin.math.round

class SDFRotSym : PositionMapper() {

    var rotation = Quaternionf()
        set(value) {
            field.set(value)
            field.normalize()
            // invalidateBounds()
        }

    var offset = Vector3f()
        set(value) {
            field.set(value)
            // invalidateBounds()
        }

    var slices = 1f
        set(value) {
            field = value
            // invalidateBounds()
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        functions.add(twistFunc)
        functions.add(quatRot)
        functions.add(rotSymFunc)
        builder.append("pos").append(posIndex)
        builder.append("=rotSym(pos").append(posIndex)
        val offset = defineUniform(uniforms, offset)
        builder.append('-').append(offset)
        builder.append(',').appendUniform(uniforms, rotation)
        builder.append(',').appendUniform(uniforms, GLSLType.V1F){ slices * invTau }
        builder.append(")+").append(offset).append(";\n")
        return null
    }

    override fun calcTransform(pos: Vector4f) {
        val offset = offset
        pos.sub(offset)
        rotation.transform(pos)
        var angle = atan2(pos.z, pos.x)
        val slices = slices
        val invSlice = slices * invTau
        if (slices < 1e9) {
            angle = round(angle * invSlice) / invSlice
        }
        // todo correct direction?
        pos.rotateY(-angle)
        rotation.transformInverse(pos)
        pos.add(offset)
    }

    fun Vector4f.add(v: Vector3f) {
        x += v.x
        y += v.y
        z += v.z
    }

    fun Vector4f.sub(v: Vector3f) {
        x -= v.x
        y -= v.y
        z -= v.z
    }

    override fun clone(): SDFRotSym {
        val clone = SDFRotSym()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFRotSym
        clone.rotation = rotation
        clone.offset = offset
        clone.slices = slices
    }

    override val className: String = "SDFRotSym"

    companion object {

        const val invTau = (1.0 / (2.0 * Math.PI)).toFloat()

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