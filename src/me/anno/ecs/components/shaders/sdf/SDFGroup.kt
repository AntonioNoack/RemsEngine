package me.anno.ecs.components.shaders.sdf

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import org.joml.Vector3f

class SDFGroup : SDFComponent() {

    override val children = ArrayList<SDFComponent>()
    fun add(c: SDFComponent) {
        children.add(c)
    }

    var smoothness = 0f

    var progress = 0.5f

    var type = CombinationMode.INTERPOLATION
    var dynamicSmoothness = false

    enum class CombinationMode(val id: Int, val funcName: String, val glslCode: String) {
        UNION(0, "sdMin", sdMin), // A or B
        INTERSECTION(1, "sdMax", sdMax), // A and B
        DIFFERENCE1(2, "sdDiff1", sdDiff1), // A \ B
        DIFFERENCE2(3, "sdDiff2", sdDiff2), // B \ A
        DIFFERENCE_SYM(4, "sdDiff3", sdDiff), // A xor B
        INTERPOLATION(5, "sdInt", sdInt),
    }

    override fun createSDFShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextIndex: Ptr<Int>,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val children = children
        if (children.isNotEmpty()) {
            val type = type
            val (posIndex, scaleName) = createTransformShader(builder, posIndex0, nextIndex, uniforms, functions)
            if (children.size == 1) {
                // done ^^
                children[0].createSDFShader(builder, posIndex, nextIndex, dstName, uniforms, functions)
            } else {
                val useSmoothness = dynamicSmoothness || smoothness > 0f
                val v1 = "res${nextIndex.value++}"
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
                for (childIndex in children.indices) {
                    val child = children[childIndex]
                    val vi = if (childIndex == 0) dstName else v1
                    child.createSDFShader(builder, posIndex, nextIndex, vi, uniforms, functions)
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
            if (scaleName != null) builder.append(dstName).append(".x*=").append(scaleName).append(";\n")
        }
    }

    override fun computeSDF(pos: Vector3f): Float {
        TODO("combine sdfs based on type")
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