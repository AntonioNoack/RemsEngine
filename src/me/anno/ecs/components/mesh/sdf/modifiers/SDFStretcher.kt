package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendVec
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
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
                if (!globalDynamic) invalidateShader()
            }
        }

    @Range(0.0, 1e300)
    var halfExtends = Vector3f()
        set(value) {
            if (dynamicExtends || globalDynamic) invalidateBounds()
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
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {
        // p=p-clamp(p,-h,h)
        builder.append("pos").append(posIndex)
        builder.append("-=clamp(pos").append(posIndex)
        builder.append(",")
        val dynamicExtends = dynamicExtends || globalDynamic
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

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {
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

    override fun applyTransform(bounds: AABBf) {
        val x0 = bounds.minX
        val x1 = bounds.maxX
        val y0 = bounds.minY
        val y1 = bounds.maxY
        val z0 = bounds.minZ
        val z1 = bounds.maxZ
        val e = halfExtends
        val ex = e.x
        val ey = e.y
        val ez = e.z
        // ex may be negative, so the sides might flip
        val x2 = x0 - ex
        val x3 = x1 + ex
        bounds.minX = min(x2, x3)
        bounds.maxX = max(x2, x3)
        val y2 = y0 - ey
        val y3 = y1 + ey
        bounds.minY = min(y2, y3)
        bounds.maxY = max(y2, y3)
        val z2 = z0 - ez
        val z3 = z1 + ez
        bounds.minZ = min(z2, z3)
        bounds.maxZ = max(z2, z3)
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
        clone.halfExtends = halfExtends
        clone.accurateInsides = accurateInsides
    }

    override val className get() = "SDFStretcher"

}