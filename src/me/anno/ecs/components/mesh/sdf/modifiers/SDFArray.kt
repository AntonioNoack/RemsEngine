package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.mod
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.round

// todo height mapping
// todo dunes
class SDFArray : PositionMapper() {

    // todo all arrays should have the option to evaluate the neighbors for correct sdfs without gaps

    // we could beautify the result when the shapes are overlapping by repeatedly calling the child...
    // would be pretty expensive...

    /**
     * repetition count
     * */
    var count = Vector3i(3)
        set(value) {
            if ((!dynamicX && value.x > 0) || (!dynamicY && value.y > 0) || (!dynamicZ && value.z > 0)) {
                invalidateShader()
            } else invalidateBounds()
            field.set(value)
        }

    /**
     * how large a cell needs to be;
     * should never be zero
     * */
    var cellSize = Vector3f(1f)
        set(value) {
            if (!globalDynamic && ((!dynamicX && value.x > 0) || (!dynamicY && value.y > 0) || (!dynamicZ && value.z > 0))) {
                invalidateShader()
            } else invalidateBounds()
            field.set(value)
        }

    var dynamicX = false
        set(value) {
            if (field != value && !globalDynamic) invalidateShader()
            field = value
        }

    var dynamicY = false
        set(value) {
            if (field != value && !globalDynamic) invalidateShader()
            field = value
        }

    var dynamicZ = false
        set(value) {
            if (field != value && !globalDynamic) invalidateShader()
            field = value
        }

    var mirrorX = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var mirrorY = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var mirrorZ = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        functions.add(sdArray)
        val cellSize = cellSize
        val count = count
        val globalDynamic = globalDynamic
        val dynamicX = dynamicX || globalDynamic
        val dynamicY = dynamicY || globalDynamic
        val dynamicZ = dynamicZ || globalDynamic
        if (dynamicX || dynamicY || dynamicZ) {
            val cellSizeUniform = defineUniform(uniforms, cellSize)
            val countUniform = defineUniform(uniforms, count)
            if (dynamicX) repeat(builder, posIndex, cellSizeUniform, countUniform, 'x', mirrorX)
            else repeat(builder, posIndex, cellSize.x, count.x, 'x', mirrorX)
            if (dynamicY) repeat(builder, posIndex, cellSizeUniform, countUniform, 'y', mirrorY)
            else repeat(builder, posIndex, cellSize.y, count.y, 'y', mirrorY)
            if (dynamicZ) repeat(builder, posIndex, cellSizeUniform, countUniform, 'z', mirrorZ)
            else repeat(builder, posIndex, cellSize.z, count.z, 'z', mirrorZ)
        } else {
            repeat(builder, posIndex, cellSize.x, count.x, 'x', mirrorX)
            repeat(builder, posIndex, cellSize.y, count.y, 'y', mirrorY)
            repeat(builder, posIndex, cellSize.z, count.z, 'z', mirrorZ)
        }
        return null
    }

    fun repeat(
        builder: StringBuilder,
        posIndex: Int,
        size: Float,
        count: Int,
        component: Char,
        mirror: Boolean
    ) {
        when {
            count == 1 || size <= 0f -> {} // done
            count <= 0 -> {// this is ok
                builder.append("pos").append(posIndex).append(".").append(component)
                builder.append("=")
                builder.append("mod2")
                if (mirror) builder.append('M')
                builder.append("(pos").append(posIndex).append(".").append(component)
                builder.append(",")
                builder.append(size)
                builder.append(");\n")
            }
            else -> {
                builder.append("pos").append(posIndex).append(".").append(component)
                builder.append("=")
                builder.append("mod2")
                if (mirror) builder.append('M')
                builder.append("(pos").append(posIndex).append(".").append(component)
                builder.append(",")
                builder.append(size)
                builder.append(",")
                builder.append((count - 1) * 0.5f)
                builder.append(",").append(count.and(1) * 0.5f).append(");\n")
            }
        }
    }

    fun repeat(
        builder: StringBuilder,
        posIndex: Int,
        size: String,
        count: String,
        component: Char,
        mirror: Boolean
    ) {
        builder.append("pos").append(posIndex).append(".").append(component)
        builder.append("=")
        builder.append("mod2")
        if (mirror) builder.append('M')
        builder.append("(pos").append(posIndex).append(".").append(component)
        builder.append(",")
        builder.append(size).append(".").append(component)
        builder.append(",")
        builder.append(count).append(".").append(component)
        builder.append(");\n")
    }

    override fun calcTransform(pos: Vector4f) {
        val rep = cellSize
        val lim = count
        if (count.x != 1 && rep.x > 0f) pos.x = if (mirrorX) mod2M(pos.x, rep.x, lim.x) else mod2(pos.x, rep.x, lim.x)
        if (count.y != 1 && rep.y > 0f) pos.y = if (mirrorY) mod2M(pos.y, rep.y, lim.y) else mod2(pos.y, rep.y, lim.y)
        if (count.z != 1 && rep.z > 0f) pos.z = if (mirrorZ) mod2M(pos.z, rep.z, lim.z) else mod2(pos.z, rep.z, lim.z)
    }

    override fun applyTransform(bounds: AABBf) {
        val rep = cellSize
        val lim = count
        if (rep.x > 0f) {
            bounds.minX = minMod2(bounds.minX, rep.x, lim.x)
            bounds.maxX = maxMod2(bounds.maxX, rep.x, lim.x)
        }
        if (rep.y > 0f) {
            bounds.minY = minMod2(bounds.minY, rep.y, lim.y)
            bounds.maxY = maxMod2(bounds.maxY, rep.y, lim.y)
        }
        if (rep.z > 0f) {
            bounds.minZ = minMod2(bounds.minZ, rep.z, lim.z)
            bounds.maxZ = maxMod2(bounds.maxZ, rep.z, lim.z)
        }
    }

    override fun clone(): SDFArray {
        val clone = SDFArray()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFArray
        clone.dynamicX = dynamicX
        clone.dynamicY = dynamicY
        clone.dynamicZ = dynamicZ
        clone.mirrorX = mirrorX
        clone.mirrorY = mirrorY
        clone.mirrorZ = mirrorZ
        clone.count = count
        clone.cellSize = cellSize
    }

    override val className: String = "SDFArray"

    companion object {

        fun maxMod2(p: Float, s: Float, c: Int): Float {
            return -minMod2(-p, s, c)
        }

        fun minMod2(p: Float, s: Float, c: Int): Float {
            if (c == 1 && s <= 0f) return p
            if (c <= 0) return Float.NEGATIVE_INFINITY
            // -1, because 1 is included by default
            // *0.5f, because it's half only (min or max)
            return p - s * (c - 1) * 0.5f
        }

        private fun mod2(p: Float, s: Float, l: Float, h: Float): Float {
            return p - s * clamp(floor(p / s + h) + .5f - h, -l, +l)
        }

        fun mirror(c: Float): Float {
            return mod(c, 2f) * 2f - 1f
        }

        private fun mod2M(p: Float, s: Float, l: Float, h: Float): Float {
            val c = clamp(floor(p / s + h) + .5f - h, -l, +l)
            return (p - s * c) * mirror(c)
        }

        private fun mod2(p: Float, s: Float, c: Int): Float {
            if (c == 1 || s <= 0f) return p
            if (c <= 0) return p - s * round(p / s)
            return mod2(p, s, (c - 1) * 0.5f, c.and(1) * 0.5f)
        }

        private fun mod2M(p: Float, s: Float, c: Int): Float {
            if (c == 1 || s <= 0f) return p
            if (c <= 0) {
                val c2 = round(p / s)
                return (p - s * c2) * mirror(c2)
            }
            return mod2M(p, s, (c - 1) * 0.5f, c.and(1) * 0.5f)
        }

        const val sdArray = "" +
                "float mirror(float f){\n" +
                "   return 1.0-2.0*mod(f,2.0);\n" +
                "}\n" +
                "float mod2M(float p, float s){\n" +
                "   float c = round(p/s);\n" +
                "   return (p-s*c)*mirror(c);\n" +
                "}\n" +
                "float mod2(float p, float s){\n" +
                "   float c = round(p/s);\n" +
                "   return p-s*c;\n" +
                "}\n" +
                "float mod2M(float p, float s, float l, float h){\n" +
                "   float c = clamp(floor(p/s+h)+.5-h,-l,l);\n" +
                "   return (p-s*c)*mirror(c);\n" +
                "}\n" +
                "float mod2(float p, float s, float l, float h){\n" +
                "   float c = clamp(floor(p/s+h)+.5-h,-l,l);\n" +
                "   return p-s*c;\n" +
                "}\n" +
                "float mod2M(float p, float s, int c){\n" +
                "   if(c == 1 || s <= 0.0) return p;\n" +
                "   if(c <= 0) return mod2M(p,s);\n" + // unlimited
                "   return mod2M(p,s,float(c-1)*0.5,((c&1)==1)?0.5:0.0);\n" +
                "}\n" +
                "float mod2(float p, float s, int c){\n" +
                "   if(c == 1 || s <= 0.0) return p;\n" +
                "   if(c <= 0) return mod2(p,s);\n" + // unlimited
                "   return mod2(p,s,float(c-1)*0.5,((c&1)==1)?0.5:0.0);\n" +
                "}\n"
    }

}