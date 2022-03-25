package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.editor.stacked.Option
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Vector4f
import kotlin.math.abs

class SDFGroup : SDFComponent() {

    // todo sphere/box/half-space bounds

    override val children = ArrayList<SDFComponent>()

    override fun getOptionsByType(type: Char): List<Option>? {
        return if (type == 'c') getOptionsByClass(this, SDFComponent::class)
        else super.getOptionsByType(type)
    }

    override fun listChildTypes(): String = "c" + super.listChildTypes()
    override fun getChildListNiceName(type: Char) = if (type == 'c') "Children" else super.getChildListNiceName(type)
    override fun getChildListByType(type: Char) = if (type == 'c') children else super.getChildListByType(type)
    override fun getTypeOf(child: PrefabSaveable): Char = if (child is SDFComponent) 'c' else super.getTypeOf(child)
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        if (child is SDFComponent) {
            children.add(index, child)
            child.parent = this
            invalidateShader()
        } else super.addChildByType(index, type, child)
    }

    override fun removeChild(child: PrefabSaveable) {
        if (child is SDFComponent) children.remove(child)
        invalidateShader()
        super.removeChild(child)
    }

    var dynamicSmoothness = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    var smoothness = 0f
        set(value) {
            if (field != value && !dynamicSmoothness) invalidateShader()
            field = value
        }

    // currently this is always dynamic -> no check needed
    var progress = 0.5f

    var type = CombinationMode.INTERPOLATION
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    enum class CombinationMode(val id: Int, val funcName: String, val glslCode: String) {
        UNION(0, "sdMin", sdMin), // A or B
        INTERSECTION(1, "sdMax", sdMax), // A and B
        DIFFERENCE1(2, "sdDiff1", sdDiff1), // A \ B
        DIFFERENCE2(3, "sdDiff2", sdDiff2), // B \ A
        DIFFERENCE_SYM(4, "sdDiff3", sdDiff), // A xor B
        INTERPOLATION(5, "sdInt", sdInt),
    }

    override fun calculateBaseBounds(dst: AABBf) {
        val base = JomlPools.aabbf.create()
        // todo depends on mode... & dynamic/not
        // for now, just use the worst case
        for (childIndex in children.indices) {
            val child = children[childIndex]
            child.calculateBounds(base)
        }
        // transform bounds & union
        transformBounds(base, dst)
        JomlPools.aabbf.sub(1)
    }

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val children = children
        if (children.isNotEmpty()) {
            val type = type
            val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
            val posIndex = trans.posIndex
            if (children.size == 1) {
                // done ^^
                children[0].buildShader(builder, posIndex, nextVariableId, dstName, uniforms, functions)
            } else {
                val useSmoothness = dynamicSmoothness || smoothness > 0f
                val v1 = "res${nextVariableId.next()}"
                builder.append("vec2 ").append(v1)
                builder.append(";\n")
                val p1Name = if (type == CombinationMode.INTERPOLATION) {
                    defineUniform(uniforms, GLSLType.V1F, { progress })
                } else null
                val p2Name = if (useSmoothness && type != CombinationMode.INTERPOLATION) {
                    functions.add(smoothMinCubic)
                    defineUniform(uniforms, GLSLType.V1F, { smoothness })
                } else null
                // helper functions
                when (type) {
                    CombinationMode.DIFFERENCE_SYM -> {
                        functions.add(sdMin)
                        functions.add(sdMax)
                        functions.add(sdDiff1)
                    }
                    CombinationMode.DIFFERENCE1, CombinationMode.DIFFERENCE2 -> {
                        functions.add(sdMax)
                    }
                    else -> {}
                }
                functions.add(type.glslCode)
                val funcName = type.funcName
                // todo only execute child, if weight > 0
                for (childIndex in children.indices) {
                    val child = children[childIndex]
                    val vi = if (childIndex == 0) dstName else v1
                    child.buildShader(builder, posIndex, nextVariableId, vi, uniforms, functions)
                    if (childIndex > 0) {
                        builder.append(dstName)
                        builder.append("=")
                        builder.append(funcName)
                        builder.append('(')
                        builder.append(dstName)
                        // todo instead of using only 2 components at max,
                        // todo we should define a spread and then integrate over a function, e.g. triangle function
                        if (p1Name != null && childIndex == 1) {
                            builder.append("*vec2(max(1.0-abs(")
                            builder.append(p1Name)
                            builder.append("),0.0), 1.0)")
                        }
                        builder.append(',')
                        builder.append(v1)
                        if (p1Name != null) {// interpolation factor
                            builder.append(",")
                            builder.append(p1Name)
                            builder.append('-')
                            builder.append(childIndex)
                            builder.append(".0")
                        } else if (useSmoothness) {
                            builder.append(",")
                            builder.append(p2Name)
                        }
                        builder.append(");\n")
                    }
                }
            }
            // first scale or offset? offset, because it was applied after scaling
            val offsetName = trans.offsetName
            val scaleName = trans.scaleName
            if (offsetName != null) builder.append(dstName).append(".x+=").append(offsetName).append(";\n")
            if (scaleName != null) builder.append(dstName).append(".x*=").append(scaleName).append(";\n")
            buildDMShader(builder, posIndex, dstName, nextVariableId, uniforms, functions)
        } else {
            builder.append(dstName).append("=vec2(1e20,-1.0);\n")
            buildDMShader(builder, posIndex0, dstName, nextVariableId, uniforms, functions)
        }
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        return when (children.size) {
            0 -> Float.POSITIVE_INFINITY
            1 -> {
                applyTransform(pos)
                val pw = pos.w
                pos.w = 0f
                return (children[0].computeSDF(pos) + pw) * scale
            }
            else -> {
                applyTransform(pos)
                val pw = pos.w
                var d0 = Float.POSITIVE_INFINITY
                val px = pos.x
                val py = pos.y
                val pz = pos.z
                val sm = smoothness
                val progress = progress
                val type = type
                for (childIndex in children.indices) {
                    val child = children[childIndex]
                    pos.set(px, py, pz, 0f)
                    val d1 = child.computeSDF(pos)
                    d0 = if (childIndex == 0) d1
                    else when (type) {
                        CombinationMode.UNION -> sMinCubic(d0, d1, sm)
                        CombinationMode.INTERSECTION -> sMaxCubic(d0, d1, sm)
                        CombinationMode.DIFFERENCE1 -> sMaxCubic(+d0, -d1, sm)
                        CombinationMode.DIFFERENCE2 -> sMaxCubic(-d0, +d1, sm)
                        CombinationMode.DIFFERENCE_SYM -> sMinCubic(
                            sMaxCubic(-d0, +d1, sm),
                            sMaxCubic(+d0, -d1, sm),
                            sm
                        )
                        CombinationMode.INTERPOLATION -> {
                            if (childIndex == 1) d0 *= max(1f - abs(progress), 0f)
                            d0 + d1 * max(1f - abs(progress - childIndex), 0f)
                        }
                    }
                }
                return (d0 + pw) * scale
            }
        }
    }

    override fun clone(): SDFGroup {
        val clone = SDFGroup()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFGroup
        clone.smoothness = smoothness
        clone.progress = progress
        clone.type = type
        clone.dynamicSmoothness = dynamicSmoothness
    }

    override val className = "SDFGroup"

    companion object {

        fun sMinCubic(a: Float, b: Float, k: Float): Float {
            val h = max(k - abs(a - b), 0f) / k
            val m = h * h * h * 0.5f
            val s = m * k / 3f
            return min(a, b) - s
        }

        fun sMaxCubic(a: Float, b: Float, k: Float): Float {
            if (k <= 0f) return max(a, b)
            val h = max(k - abs(a - b), 0f) / k
            val m = h * h * h * 0.5f
            val s = m * k / 3f
            return max(a, b) + s
        }

        const val smoothMinCubic = "" +
                // todo when we have material colors, use the first one as mixing parameter
                // inputs: sd.a, sd.b, k
                // outputs: sd.mix, mix factor
                "vec2 sMinCubic(float a, float b, float k){\n" +
                "    float h = max(k-abs(a-b), 0.0)/k;\n" +
                "    float m = h*h*h*0.5;\n" +
                "    float s = m*k*(1.0/3.0); \n" +
                "    return (a<b) ? vec2(a-s,m) : vec2(b-s,1.0-m);\n" +
                "}\n" +
                "vec2 sMaxCubic(float a, float b, float k){\n" +
                "    float h = max(k-abs(a-b), 0.0)/k;\n" +
                "    float m = h*h*h*0.5;\n" +
                "    float s = m*k*(1.0/3.0); \n" +
                "    return (a>b) ? vec2(a+s,m) : vec2(b+s,1.0-m);\n" +
                "}\n" +
                // inputs: sd.a, sd.b, k
                // outputs: sd.mix
                "float sMinCubic1(float a, float b, float k){\n" +
                "    float h = max(k-abs(a-b), 0.0)/k;\n" +
                "    float m = h*h*h*0.5;\n" +
                "    float s = m*k*(1.0/3.0); \n" +
                "    return min(a,b)-s;\n" +
                "}\n" +
                "float sMaxCubic1(float a, float b, float k){\n" +
                "    float h = max(k-abs(a-b), 0.0)/k;\n" +
                "    float m = h*h*h*0.5;\n" +
                "    float s = m*k*(1.0/3.0); \n" +
                "    return max(a,b)+s;\n" +
                "}\n" +
                // inputs: sd/m1, sd/m2, k
                // outputs: sd/m-mix
                "vec2 sMinCubic2(vec2 a, vec2 b, float k){\n" +
                "    float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
                "    float m = h*h*h*0.5;\n" +
                "    float s = m*k*(1.0/3.0); \n" +
                "    return (a.x<b.x) ? vec2(a.x-s,a.y) : vec2(b.x-s,b.y);\n" +
                "}\n" +
                "vec2 sMaxCubic2(vec2 a, vec2 b, float k){\n" +
                "    float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
                "    float m = h*h*h*0.5;\n" +
                "    float s = m*k*(1.0/3.0); \n" +
                "    return (a.x>b.x) ? vec2(a.x+s,a.y) : vec2(b.x+s,b.y);\n" +
                "}\n"
        const val sdMin = "" +
                "float sdMin3(float a, float b, float c){ return min(a,min(b,c)); }\n" +
                "float sdMin3(float a, float b, float c, float k){ return sMinCubic1(a,sMinCubic1(b,c,k),k); }\n" +
                "float sdMin(float d1, float d2){ return min(d1,d2); }\n" +
                "vec2 sdMin(vec2 d1, vec2 d2){ return d1.x < d2.x ? d1 : d2; }\n" +
                "vec2 sdMin(vec2 d1, vec2 d2, float k){ return sMinCubic2(d1,d2,k); }\n"
        const val sdMax = "" +
                "float sdMax(float d1, float d2){ return max(d1,d2); }\n" +
                "vec2 sdMax(vec2 d1, vec2 d2){ return d1.x < d2.x ? d2 : d1; }\n" +
                "float sdMax(float d1, float d2, float k){ return sMaxCubic1(d1,d2,k); }\n" +
                "vec2 sdMax(vec2 d1, vec2 d2, float k){ return sMaxCubic2(d1,d2,k); }\n"
        const val sdDiff = "" +
                "vec2 sdDiff3(vec2 d1, vec2 d2){\n" +
                "  vec2 e1 = sdDiff1(d1,d2);\n" +
                "  vec2 e2 = sdDiff1(d2,d1);\n" +
                "  return sdMin(e1,e1); }\n" +
                "vec2 sdDiff3(vec2 d1, vec2 d2, float k){\n" +
                "  vec2 e1 = sdDiff1(d1,d2,k);\n" +
                "  vec2 e2 = sdDiff1(d2,d1,k);\n" +
                "  return sdMin(e1,e1,k); }\n"
        const val sdDiff1 = "" +
                "float sdDiff1(float d1, float d2){ return max(d1, -d2); }\n" +
                "float sdDiff1(float d1, float d2, float k){ return sdMax(d1, -d2, k); }\n" +
                "vec2 sdDiff1(vec2 d1, vec2 d2){ return sdMax(d1, vec2(-d2.x, d2.y)); }\n" +
                "vec2 sdDiff1(vec2 d1, vec2 d2, float k){ return sdMax(d1, vec2(-d2.x, d2.y), k); }\n"
        const val sdDiff2 = "" +
                "float sdDiff2(float d2, float d1){ return max(d1, -d2); }\n" +
                "float sdDiff2(float d2, float d1, float k){ return sdMax(d1, -d2, k); }\n" +
                "vec2 sdDiff2(vec2 d2, vec2 d1){ return sdMax(d1, vec2(-d2.x, d2.y)); }\n" +
                "vec2 sdDiff2(vec2 d2, vec2 d1, float k){ return sdMax(d1, vec2(-d2.x, d2.y), k); }\n"
        const val sdInt = "vec2 sdInt(vec2 sum, vec2 di, float weight){\n" +
                "weight = 1.0-abs(weight);\n" +
                "if(weight < 0.0) return sum;\n" +
                "return vec2(sum.x + di.x * weight, weight >= 0.5 ? di.y : sum.y); }\n"
    }

}