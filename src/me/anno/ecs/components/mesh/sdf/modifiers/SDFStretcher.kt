package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendVec
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import org.joml.Vector3f
import org.joml.Vector4f

class SDFStretcher() : PositionMapper() {

    constructor(hx: Float, hy: Float, hz: Float) : this() {
        halfExtends.set(max(0f, hx), max(hy, 0f), max(hz, 0f))
    }

    constructor(halfExtends: Vector3f) : this() {
        this.halfExtends = halfExtends
    }

    var dynamicExtends = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    var halfExtends = Vector3f()
        set(value) {
            if (dynamicExtends) invalidateBounds()
            else invalidateShader()
            field.set(value)
            field.x = max(field.x, 0f)
            field.y = max(field.y, 0f)
            field.z = max(field.z, 0f)
        }

    var accurateInsides = true
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
        // p=p-clamp(p,-h,h)
        builder.append("pos").append(posIndex)
        builder.append("-=clamp(pos").append(posIndex)
        builder.append(",")
        if (dynamicExtends) {
            val uniform = defineUniform(uniforms, halfExtends)
            builder.append("-").append(uniform)
            builder.append(",").append(uniform)
            builder.append(");\n")
            return if (accurateInsides) {
                val name = "tmp${nextVariableId.next()}"
                builder.append("float ")
                builder.append(name)
                builder.append("=min(0.0,max(")
                builder.append(uniform).append(".x,max(")
                builder.append(uniform).append(".y,")
                builder.append(uniform).append(".z)));\n")
                name
            } else null
        } else {
            val h = halfExtends
            builder.append("-")
            builder.appendVec(h)
            builder.append(",")
            builder.appendVec(h)
            builder.append(");\n")
            return if (accurateInsides) {
                val name = "tmp${nextVariableId.next()}"
                builder.append("float ")
                builder.append(name)
                builder.append("=")
                builder.append(min(0f, max(h.x, max(h.y, h.z))))
                builder.append(";\n")
                name
            } else null
        }
    }

    override fun calcTransform(pos: Vector4f) {
        val e = halfExtends
        val ex = e.x
        val ey = e.y
        val ez = e.z
        pos.x = pos.x - clamp(pos.x, -ex, ex)
        pos.y = pos.y - clamp(pos.y, -ey, ey)
        pos.z = pos.z - clamp(pos.z, -ez, ez)
        // correction for inside
        if (accurateInsides) {
            pos.w += min(max(ex, max(ey, ez)), 0f)
            /*pos.x = max(pos.x, 0f)
            pos.y = max(pos.y, 0f)
            pos.z = max(pos.z, 0f)*/
        }
    }

    override fun clone(): SDFStretcher {
        val clone = SDFStretcher()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFStretcher
        clone.dynamicExtends = dynamicExtends
        clone.halfExtends.set(halfExtends)
        clone.accurateInsides = accurateInsides
    }

    override val className: String = "SDFStretcher"

}