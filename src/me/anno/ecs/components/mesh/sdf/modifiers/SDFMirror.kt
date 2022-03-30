package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.writeVec
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.SDFHalfSpace.Companion.dot
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.sq
import me.anno.utils.types.AABBs.clear
import org.joml.AABBf
import org.joml.Planef
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.sqrt

class SDFMirror() : PositionMapper() {

    // todo edit planes using gizmos
    // todo also mark vectors as potential positions

    constructor(position: Vector3f) : this(position, position)

    constructor(position: Vector3f, normal: Vector3f) : this() {
        plane.set(normal.x, normal.y, normal.z, -normal.dot(position))
        plane.normalize3()
    }

    override fun applyTransform(bounds: AABBf) {
        // first: intersect bounds with plane
        // then: mirror remaining points onto other side
        // effectively, we just test all border points:
        // if they are on the active side, they will get added on both sides,
        // if they are on the inactive side, just discard them
        val imx = bounds.minX
        val imy = bounds.minY
        val imz = bounds.minZ
        val ixx = bounds.maxX
        val ixy = bounds.maxY
        val ixz = bounds.maxZ
        bounds.clear()
        val normal = plane
        for (i in 0 until 8) {
            val x = if (i.and(1) == 0) imx else ixx
            val y = if (i.and(2) == 0) imy else ixy
            val z = if (i.and(4) == 0) imz else ixz
            val dot = 2f * (normal.a * x + normal.b * y + normal.c * z + normal.d)
            if (dot >= 0f) {
                bounds.union(x, y, z)
                bounds.union(x - dot * normal.a, y - dot * normal.b, z - dot * normal.c)
            }
        }
    }

    // proper smoothness would require two sdf evaluations
    // considering this effect probably would be stacked, it would get too expensive
    // (+ our pipeline currently does not support that)

    @Suppress("SetterBackingFieldAssignment")
    var plane = Planef(0f, 1f, 0f, 0f)
        set(value) {
            if (dynamicPlane) invalidateBounds()
            else invalidateShader()
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
        clone.plane.set(plane.a, plane.b, plane.c, plane.d)
        clone.dynamicPlane = dynamicPlane
        clone.useBranch = useBranch
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