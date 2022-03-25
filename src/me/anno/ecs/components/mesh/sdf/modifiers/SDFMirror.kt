package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.writeVec
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.SDFHalfSpace.Companion.dot
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.sq
import org.joml.Planef
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.sqrt

class SDFMirror() : PositionMapper() {

    constructor(position: Vector3f) : this(position, position)

    constructor(position: Vector3f, normal: Vector3f) : this() {
        plane.set(normal.x, normal.y, normal.z, -normal.dot(position))
        plane.normalize3()
    }

    // proper smoothness would require two sdf evaluations
    // considering this effect probably would be stacked, it would get too expensive
    // (+ our pipeline currently does not support that)

    @Suppress("SetterBackingFieldAssignment")
    var plane = Planef(0f, 1f, 0f, 0f)
        set(value) {
            if (!dynamicPlane) invalidateShader()
            field.set(value.a, value.b, value.c, value.d)
            field.normalize3()
        }

    var dynamicPlane = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    // idk how performance behaves, try it yourself ^^
    var useBranch = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        // reflect(I,N): I - 2.0 * dot(N, I) * N
        val tmpIndex = nextVariableId.next()
        val normal = if (dynamicPlane) defineUniform(uniforms, plane) else {
            val name = "nor${nextVariableId.next()}"
            builder.append("vec4 ").append(name)
            builder.append("=")
            writeVec(builder, plane)
            builder.append(";\n")
            name
        }
        builder.append("float tmp").append(tmpIndex).append("=dot(vec4(")
        builder.append("pos").append(posIndex)
        builder.append(",1.0),").append(normal)
        builder.append(");\n")
        if (useBranch) {
            builder.append("if(tmp").append(tmpIndex).append("<0.0) pos").append(posIndex)
            builder.append("-=2.0*tmp").append(tmpIndex).append("*").append(normal).append(".xyz;\n")
        } else {
            builder.append("pos").append(posIndex)
            builder.append("-=((tmp").append(tmpIndex).append(" < 0.0 ? 2.0 : 0.0)*tmp")
            builder.append(tmpIndex).append(")*").append(normal).append(".xyz;\n")
        }
        return null
    }

    override fun calcTransform(pos: Vector4f) {
        val normal = plane
        val dot = 2f * normal.dot(pos)
        if (dot < 0f) pos.sub(dot * normal.a, dot * normal.b, dot * normal.c, 0f)
    }

    override fun clone(): SDFMirror {
        val clone = SDFMirror()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFMirror
        clone.plane = plane
        clone.dynamicPlane = dynamicPlane
    }

    override val className = "SDFMirror"

    companion object {
        fun Planef.normalize3(): Planef {
            val sq = sq(a, b, c)
            if (sq > 0f) {
                val factor = 1f / sqrt(sq)
                a *= factor
                b *= factor
                c *= factor
                d *= factor
            } else {
                a = 0f
                b = 1f
                c = 0f
            }
            if (d.isNaN()) d = 0f
            return this
        }
    }

}