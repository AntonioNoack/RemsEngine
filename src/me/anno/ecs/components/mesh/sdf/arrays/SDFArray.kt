package me.anno.ecs.components.mesh.sdf.arrays

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.mod
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.components.mesh.sdf.random.SDFRandom.Companion.randLib
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.round

class SDFArray : PositionMapper() {

    // todo all arrays should have the option to evaluate the neighbors for correct sdfs without gaps
    // -> for this array, use SDFArray2 :)

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
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {
        functions.add(sdArray)
        functions.add(randLib)
        val cellSize = cellSize
        val count = count
        val globalDynamic = globalDynamic
        val dynamicX = dynamicX || globalDynamic
        val dynamicY = dynamicY || globalDynamic
        val dynamicZ = dynamicZ || globalDynamic
        val rnd = nextVariableId.next()
        builder.append("vec3 tmp").append(rnd).append("=vec3(0.0);\n")
        if (dynamicX || dynamicY || dynamicZ) {
            val cellSizeUniform = defineUniform(uniforms, cellSize)
            val countUniform = defineUniform(uniforms, count)
            if (dynamicX) repeat(builder, posIndex, cellSizeUniform, countUniform, 'x', rnd, mirrorX)
            else repeat(builder, posIndex, cellSize.x, count.x, 'x', rnd, mirrorX)
            if (dynamicY) repeat(builder, posIndex, cellSizeUniform, countUniform, 'y', rnd, mirrorY)
            else repeat(builder, posIndex, cellSize.y, count.y, 'y', rnd, mirrorY)
            if (dynamicZ) repeat(builder, posIndex, cellSizeUniform, countUniform, 'z', rnd, mirrorZ)
            else repeat(builder, posIndex, cellSize.z, count.z, 'z', rnd, mirrorZ)
        } else {
            repeat(builder, posIndex, cellSize.x, count.x, 'x',rnd, mirrorX)
            repeat(builder, posIndex, cellSize.y, count.y, 'y',rnd, mirrorY)
            repeat(builder, posIndex, cellSize.z, count.z, 'z',rnd, mirrorZ)
        }
        val seed = "seed" + nextVariableId.next()
        builder.append("int ").append(seed).append("=threeInputRandom(int(floor(tmp")
            .append(rnd).append(".x)),int(floor(tmp")
            .append(rnd).append(".y)),int(floor(tmp")
            .append(rnd).append(".z)));\n")
        seeds.add(seed)
        return null
    }

    fun repeat(
        builder: StringBuilder,
        posIndex: Int,
        size: Float,
        count: Int,
        component: Char,
        nameIdx: Int,
        mirror: Boolean
    ) {
        when {
            count == 1 || size <= 0f -> {} // done
            count <= 0 -> {// this is ok
                builder.append("pos").append(posIndex).append(".").append(component)
                builder.append(if (mirror) "=mod2M(pos" else "=mod2(pos")
                builder.append(posIndex).append(".").append(component).append(",")
                builder.append(size).append(",tmp")
                    .append(nameIdx).append(".").append(component).append(");\n")
            }
            else -> {
                builder.append("pos").append(posIndex).append(".").append(component)
                builder.append(if (mirror) "=mod2M(pos" else "=mod2(pos")
                builder.append(posIndex).append(".").append(component).append(",")
                builder.append(size).append(",")
                builder.append((count - 1) * 0.5f).append(",")
                builder.append(count.and(1) * 0.5f).append(",tmp")
                    .append(nameIdx).append(".").append(component).append(");\n")
            }
        }
    }

    fun repeat(
        builder: StringBuilder,
        posIndex: Int,
        size: String,
        count: String,
        component: Char,
        nameIdx: Int,
        mirror: Boolean
    ) {
        builder.append("pos").append(posIndex).append(".").append(component)
        builder.append(if (mirror) "=mod2M(pos" else "=mod2(pos")
        builder.append(posIndex).append(".").append(component).append(",")
        builder.append(size).append(".").append(component).append(",")
        builder.append(count).append(".").append(component).append(",tmp")
            .append(nameIdx).append(".").append(component).append(");\n")
    }

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {
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

    override val className get() = "SDFArray"

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
            return if (c <= 0) p - s * round(p / s)
            else mod2(p, s, (c - 1) * 0.5f, c.and(1) * 0.5f)
        }

        private fun mod2M(p: Float, s: Float, c: Int): Float {
            return if (c <= 0) {
                val c2 = round(p / s)
                (p - s * c2) * mirror(c2)
            } else mod2M(p, s, (c - 1) * 0.5f, c.and(1) * 0.5f)
        }

        const val sdArray = "" +
                "float mirror(float f){return 1.0-2.0*mod(f,2.0);}\n" +
                "vec2 mirror(vec2 f){return 1.0-2.0*mod(f,2.0);}\n" +
                "vec3 mirror(vec3 f){return 1.0-2.0*mod(f,2.0);}\n" +
                "vec4 mirror(vec4 f){return 1.0-2.0*mod(f,2.0);}\n" +
                "float mod2M(float p, float s, out float c){\n" +
                "   c = round(p/s);\n" +
                "   return (p-s*c)*mirror(c);\n" +
                "}\n" +
                "float mod2(float p, float s, out float c){\n" +
                "   c = round(p/s);\n" +
                "   return p-s*c;\n" +
                "}\n" +
                "float mod2C(float p, float s, float l, float h){ return clamp(floor(p/s+h)+.5-h,-l,l); }\n" +
                "vec3 mod2C(vec3 p, vec3 s, vec3 l, vec3 h){ return clamp(floor(p/s+h)+.5-h,-l,l); }\n" +
                "float mod2M(float p, float s, float l, float h, out float c){\n" +
                "   c = mod2C(p,s,l,h);\n" +
                "   return (p-s*c)*mirror(c);\n" +
                "}\n" +
                "float mod2(float p, float s, float l, float h, out float c){\n" +
                "   c = mod2C(p,s,l,h);\n" +
                "   return p-s*c;\n" +
                "}\n" +
                "float mod2M(float p, float s, int c, out float c2){\n" +
                "   if(c <= 0) return mod2M(p,s,c2);\n" + // unlimited
                "   return mod2M(p,s,float(c-1)*0.5,((c&1)==1)?0.5:0.0,c2);\n" +
                "}\n" +
                "float mod2(float p, float s, int c, out float c2){\n" +
                "   if(c <= 0) return mod2(p,s,c2);\n" + // unlimited
                "   return mod2(p,s,float(c-1)*0.5,((c&1)==1)?0.5:0.0,c2);\n" +
                "}\n"
    }

}