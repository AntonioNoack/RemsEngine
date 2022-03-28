package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.editor.stacked.Option
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.AABBs.all
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.intersect
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * joins multiple sdf components together, like a folder;
 * can also join them using special operations like subtraction, xor, and logical and (see type)
 * */
class SDFGroup : SDFComponent() {

    enum class CombinationMode(val id: Int, val funcName: String, val glslCode: List<String>) {
        UNION(0, "sdMin", listOf(smoothMinCubic, sdMin)), // A or B
        INTERSECTION(1, "sdMax", listOf(smoothMinCubic, sdMax)), // A and B
        DIFFERENCE1(2, "sdDiff1", listOf(smoothMinCubic, sdMin, sdMax, sdDiff1)), // A \ B
        DIFFERENCE2(3, "sdDiff2", listOf(smoothMinCubic, sdMin, sdMax, sdDiff2)), // B \ A
        DIFFERENCE_SYM(4, "sdDiff3", listOf(smoothMinCubic, sdMin, sdMax, sdDiff1, sdDiff)), // A xor B
        INTERPOLATION(5, "sdInt", listOf(sdInt)),
    }

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

    @Range(0.0, 1e38)
    var smoothness = 0f
        set(value) {
            if (field != value &&
                !dynamicSmoothness &&
                type != CombinationMode.INTERPOLATION
            ) invalidateShader()
            field = value
        }

    // currently this is always dynamic -> no shader check needed
    @Range(0.0, 2e9)
    var progress = 0.5f
        set(value) {
            if (field != value) {
                if (type == CombinationMode.INTERPOLATION) invalidateBounds()
                field = value
            }
        }

    var type = CombinationMode.UNION
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    val numActiveChildren get() = children.count { it.isEnabled }

    override fun calculateBaseBounds(dst: AABBf) {
        // for now, just use the worst case
        val children = children
        when (val size = numActiveChildren) {
            0 -> {}
            1 -> children.first { it.isEnabled }.calculateBounds(dst)
            else -> {
                val tmp = JomlPools.aabbf.create()
                when (type) {
                    // we could try to subtract one bbx from another
                    // we also probably should merge all others first, so the subtraction is maximal
                    // -> no, a bbx is a BOUNDING box: the actually subtracted amount may be smaller
                    // ... we could set a flag, whether we're allowed to subtract :)
                    CombinationMode.DIFFERENCE1 -> { // use first only, the rest is cut off
                        children.first { it.isEnabled }.calculateBounds(dst)
                    }
                    CombinationMode.DIFFERENCE2 -> {// use last only, the rest is cut off
                        children.last { it.isEnabled }.calculateBounds(dst)
                    }
                    // for interpolation only use meshes with weight > 0f
                    // is it possible to interpolate bounds? maybe
                    CombinationMode.INTERPOLATION -> {
                        dst.setMin(0f, 0f, 0f)
                        dst.setMax(0f, 0f, 0f)
                        var activeIndex = -1
                        for (childIndex in 0 until size) {
                            val child = children[childIndex]
                            if (child.isEnabled) {
                                activeIndex++
                                val weight = 1f - abs(progress - activeIndex)
                                if (weight > 0f) {
                                    tmp.clear()
                                    child.calculateBounds(tmp)
                                    dst.minX += tmp.minX * weight
                                    dst.minY += tmp.minY * weight
                                    dst.minZ += tmp.minZ * weight
                                    dst.maxX += tmp.maxX * weight
                                    dst.maxY += tmp.maxY * weight
                                    dst.maxZ += tmp.maxZ * weight
                                }
                            }
                        }
                    }
                    // difference sym-s bounds are only smaller, if the two objects are identical,
                    // so they get cancelled out ... why would sb want this? idk -> ignore that case
                    CombinationMode.UNION,
                    CombinationMode.DIFFERENCE_SYM -> {
                        dst.clear()
                        for (childIndex in children.indices) {
                            val child = children[childIndex]
                            if (child.isEnabled) {
                                tmp.clear()
                                child.calculateBounds(tmp)
                                dst.union(tmp)
                            }
                        }
                    }
                    CombinationMode.INTERSECTION -> {
                        dst.all()
                        for (childIndex in children.indices) {
                            val child = children[childIndex]
                            if (child.isEnabled) {
                                tmp.clear()
                                child.calculateBounds(tmp)
                                dst.intersect(tmp)
                            }
                        }
                    }
                }
                JomlPools.aabbf.sub(1)
            }
        }
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
        if (children.any2 { it.isEnabled }) {
            val type = type
            val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions)
            val posIndex = trans.posIndex
            if (numActiveChildren == 1) {
                // done ^^
                children.first { it.isEnabled }
                    .buildShader(builder, posIndex, nextVariableId, dstName, uniforms, functions)
            } else {
                val useSmoothness = dynamicSmoothness || smoothness > 0f
                val v1 = "res${nextVariableId.next()}"
                builder.append("vec2 ").append(v1)
                builder.append(";\n")
                val p1Name = if (type == CombinationMode.INTERPOLATION) {
                    defineUniform(uniforms, GLSLType.V1F) { progress }
                } else null
                functions.add(smoothMinCubic)
                val p2Name = if (useSmoothness && type != CombinationMode.INTERPOLATION) {
                    defineUniform(uniforms, GLSLType.V1F) { smoothness }
                } else null
                // helper functions
                functions.addAll(type.glslCode)
                val funcName = type.funcName
                // todo only execute child, if weight > 0
                var activeIndex = -1
                for (index in children.indices) {
                    val child = children[index]
                    if (!child.isEnabled) continue
                    activeIndex++
                    val vi = if (activeIndex == 0) dstName else v1
                    child.buildShader(builder, posIndex, nextVariableId, vi, uniforms, functions)
                    if (activeIndex > 0) {
                        builder.append(dstName)
                        builder.append("=")
                        builder.append(funcName)
                        builder.append('(')
                        builder.append(dstName)
                        // todo instead of using only 2 components at max,
                        // todo we should define a spread and then integrate over a function, e.g. triangle function
                        if (p1Name != null && activeIndex == 1) {
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
                            builder.append(activeIndex)
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
            if (localReliability != 1f) builder.append(dstName).append(".x*=")
                .appendUniform(uniforms, GLSLType.V1F) { localReliability }.append(";\n")
            buildDMShader(builder, posIndex, dstName, nextVariableId, uniforms, functions)
        } else {
            builder.append(dstName).append("=vec2(1e20,-1.0);\n")
            buildDMShader(builder, posIndex0, dstName, nextVariableId, uniforms, functions)
        }
    }

    override fun computeSDFBase(pos: Vector4f): Float {
        return when (numActiveChildren) {
            0 -> Float.POSITIVE_INFINITY
            1 -> {
                applyTransform(pos)
                val pw = pos.w
                pos.w = 0f
                return (children.first { it.isEnabled }.computeSDF(pos) + pw) * scale
            }
            else -> {
                applyTransform(pos)
                val pw = pos.w
                var d0 = Float.POSITIVE_INFINITY
                val px = pos.x
                val py = pos.y
                val pz = pos.z
                val k = smoothness
                val progress = progress
                val type = type

                var activeIndex = -1
                for (index in children.indices) {
                    val child = children[index]
                    if (!child.isEnabled) continue
                    activeIndex++

                    if (type == CombinationMode.INTERPOLATION) {

                        if (activeIndex == 0) d0 = 0f
                        val weight = 1f - abs(progress - activeIndex)
                        if (weight > 0f) {
                            pos.set(px, py, pz, 0f)
                            val d1 = child.computeSDF(pos)
                            d0 += d1 * weight
                        }// we could maybe exit early...

                    } else {

                        pos.set(px, py, pz, 0f)
                        val d1 = child.computeSDF(pos)
                        d0 = if (activeIndex == 0) d1
                        else when (type) {
                            CombinationMode.UNION -> sMinCubic(d0, d1, k)
                            CombinationMode.INTERSECTION -> sMaxCubic(d0, d1, k)
                            CombinationMode.DIFFERENCE1 -> sMaxCubic(+d0, -d1, k)
                            CombinationMode.DIFFERENCE2 -> sMaxCubic(-d0, +d1, k)
                            CombinationMode.DIFFERENCE_SYM -> sMaxCubic(
                                +sMinCubic(d0, d1, k),
                                -sMinCubic(d0, d1, k), k
                            )
                            CombinationMode.INTERPOLATION -> {
                                if (activeIndex == 1) d0 *= max(1f - abs(progress), 0f)
                                d0 + d1 * max(1f - abs(progress - activeIndex), 0f)
                            }
                        }
                    }
                }
                return (d0 + pw) * scale
            }
        }
    }

    override fun findClosestComponent(pos: Vector4f): SDFComponent {
        val children = children
        return when (numActiveChildren) {
            0 -> this
            1 -> {
                applyTransform(pos)
                children.first { it.isEnabled }.findClosestComponent(pos)
            }
            else -> {
                applyTransform(pos)
                val px = pos.x
                val py = pos.y
                val pz = pos.z
                val progress = progress
                val type = type
                // todo best match the GPU implementation, so we get equal results
                when (type) {
                    CombinationMode.UNION -> {
                        var bestDistance = 0f
                        var bestComp: SDFComponent? = null
                        val k = smoothness
                        for (index in children.indices) {
                            val child = children[index]
                            if (child.isEnabled) {
                                pos.set(px, py, pz, 0f)
                                val distance = child.computeSDF(pos)
                                if (bestComp == null || (k <= 0f && distance < bestDistance)) {
                                    bestDistance = distance
                                    pos.set(px, py, pz, 0f)
                                    bestComp = child.findClosestComponent(pos)
                                } else if (distance < bestDistance) {
                                    bestDistance = sMinCubic(bestDistance, distance, k)
                                    pos.set(px, py, pz, 0f)
                                    bestComp = child.findClosestComponent(pos)
                                }
                            }
                        }
                        return bestComp!!
                    }
                    CombinationMode.INTERSECTION -> {
                        // find closest child
                        val bestChild = children
                            .filter { it.isEnabled }
                            .minByOrNull {
                                pos.set(px, py, pz, 0f)
                                it.computeSDF(pos)
                            }!!
                        pos.set(px, py, pz, 0f)
                        return bestChild.findClosestComponent(pos)
                    }
                    CombinationMode.DIFFERENCE1 -> {
                        // todo use child A, except where B is cutting it off
                    }
                    CombinationMode.DIFFERENCE2 -> {
                        // todo like diff1, but opposite
                    }
                    CombinationMode.DIFFERENCE_SYM -> {
                        // todo child which is closest
                    }
                    CombinationMode.INTERPOLATION -> {
                        // child with largest weight
                        pos.set(px, py, pz, 0f)
                        val enabled = children.filter { it.isEnabled }
                        return enabled[clamp(progress.roundToInt(), 0, enabled.lastIndex)]
                            .findClosestComponent(pos)
                    }
                }
                LOGGER.warn("$type hasn't been implemented yet")
                return this
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
        clone.children.clear()
        clone.children.addAll(children.map {
            val child = it.clone()
            child.parent = clone
            child
        })
    }

    override val className = "SDFGroup"

    companion object {

        private val LOGGER = LogManager.getLogger(SDFGroup::class)

        fun sMinCubic(a: Float, b: Float, k: Float): Float {
            if (k <= 0f) return min(a, b)
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
                "   if(k <= 0.0) return min(a,b);\n" +
                "   float h = max(k-abs(a-b), 0.0)/k;\n" +
                "   float m = h*h*h*0.5;\n" +
                "   float s = m*k*(1.0/3.0); \n" +
                "   return min(a,b)-s;\n" +
                "}\n" +
                "float sMaxCubic1(float a, float b, float k){\n" +
                "   if(k <= 0.0) return max(a,b);\n" +
                "   float h = max(k-abs(a-b), 0.0)/k;\n" +
                "   float m = h*h*h*0.5;\n" +
                "   float s = m*k*(1.0/3.0); \n" +
                "   return max(a,b)+s;\n" +
                "}\n" +
                // inputs: sd/m1, sd/m2, k
                // outputs: sd/m-mix
                "vec2 sMinCubic2(vec2 a, vec2 b, float k){\n" +
                "   if(k <= 0.0) return (a.x<b.x) ? a : b;\n" +
                "   float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
                "   float m = h*h*h*0.5;\n" +
                "   float s = m*k*(1.0/3.0); \n" +
                "   return (a.x<b.x) ? vec2(a.x-s,a.y) : vec2(b.x-s,b.y);\n" +
                "}\n" +
                "vec2 sMaxCubic2(vec2 a, vec2 b, float k){\n" +
                "   if(k <= 0.0) return (a.x>b.x) ? a : b;\n" +
                "   float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
                "   float m = h*h*h*0.5;\n" +
                "   float s = m*k*(1.0/3.0); \n" +
                "   return (a.x>b.x) ? vec2(a.x+s,a.y) : vec2(b.x+s,b.y);\n" +
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
                "  vec2 e1 = sdMin(d1,d2);\n" +
                "  vec2 e2 = sdMax(d1,d2);\n" +
                "  return sdDiff1(e1,e2); }\n" +
                "vec2 sdDiff3(vec2 d1, vec2 d2, float k){\n" +
                "  vec2 e1 = sdMin(d1,d2,k);\n" +
                "  vec2 e2 = sdMax(d1,d2,k);\n" +
                "  return sdDiff1(e1,e2,k); }\n"
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