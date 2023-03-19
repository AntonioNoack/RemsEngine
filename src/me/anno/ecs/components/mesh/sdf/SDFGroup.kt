package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.SQRT1_2f
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.first2
import me.anno.utils.structures.tuples.Quad
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.roundToInt

// todo the sdf things have become quite a lot of code, and probably won't be in that many games,
//  therefore they probably should be a mod/plugin

/**
 * joins multiple sdf components together, like a folder;
 * can also join them using special operations like subtraction, xor, and logical and (see type)
 * */
open class SDFGroup : SDFComponent() {

    enum class CombinationMode(
        val id: Int,
        val funcName: String,
        val glslCode: List<String>,
        val isStyleable: Boolean
    ) {
        UNION(0, "sdMin", listOf(smoothMinCubic, sdMin), true), // A or B
        INTERSECTION(1, "sdMax", listOf(smoothMinCubic, sdMax), true), // A and B
        DIFFERENCE1(2, "sdMax", listOf(smoothMinCubic, sdMax), true), // A \ B
        DIFFERENCE2(3, "sdMax", listOf(smoothMinCubic, sdMax), true), // B \ A
        DIFFERENCE_SYM(4, "sdDiff3", listOf(smoothMinCubic, sdMin, sdMax, sdDiff1, sdDiff), false), // A xor B
        INTERPOLATION(5, "sdInt", listOf(sdInt), false),
        PIPE(10, "sdPipe", listOf(hgFunctions), false),
        ENGRAVE(11, "sdEngrave", listOf(hgFunctions), false),
        GROOVE(12, "sdGroove", listOf(hgFunctions), false),
        TONGUE(13, "sdTongue", listOf(hgFunctions), false),
    }

    enum class Style(val id: Int) {
        DEFAULT(0),
        ROUND(1),
        COLUMNS(2),
        STAIRS(3),
        CHAMFER(4),
        SOFT(5),
    }

    var style = Style.DEFAULT
        set(value) {
            // probably should check whether the type is applied at all,
            // whether we really need to invalidate the shader
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override val children = ArrayList<SDFComponent>()

    override fun getOptionsByType(type: Char) =
        if (type == 'c') getOptionsByClass(this, SDFComponent::class)
        else super.getOptionsByType(type)

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

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        val clickId1 = super.fill(pipeline, entity, clickId)
        return assignClickIds(this, clickId1)
    }

    private fun assignClickIds(sdf: SDFGroup, clickId0: Int): Int {
        var clickId = clickId0
        val children = sdf.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled) {
                child.clickId = clickId++
                if (child is SDFGroup) {
                    clickId = assignClickIds(child, clickId)
                }
            }
        }
        return clickId
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

    @Range(0.0, 1e38)
    var numStairs = 5f

    @Range(0.0, 1e38)
    var groove = Vector2f(0.1f)
        set(value) {
            field.set(value)
        }

    private val numActiveChildren get() = children.count { it.isEnabled }

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
                    CombinationMode.DIFFERENCE1,
                    CombinationMode.ENGRAVE,
                    CombinationMode.GROOVE -> { // use first only, the rest is cut off
                        children.first { it.isEnabled }.calculateBounds(dst)
                        if (type == CombinationMode.GROOVE) dst.addMargin(smoothness)
                    }
                    CombinationMode.TONGUE -> { // use first only, then widen
                        children.first { it.isEnabled }.calculateBounds(dst)
                        dst.addMargin(smoothness)
                    }
                    CombinationMode.DIFFERENCE2 -> {// use last only, the rest is cut off
                        children.last { it.isEnabled }.calculateBounds(dst)
                    }
                    // for interpolation only use meshes with weight > 0f
                    // is it possible to interpolate bounds? maybe
                    CombinationMode.INTERPOLATION -> {
                        dst.setMin(0f, 0f, 0f)
                        dst.setMax(0f, 0f, 0f)
                        val progress = clamp(progress, 0f, children.size - 1f)
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
                    CombinationMode.PIPE,
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
                        if (type == CombinationMode.PIPE) dst.addMargin(smoothness)
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
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val children = children
        if (children.any2 { it.isEnabled }) {
            val type = type
            val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
            val posIndex = trans.posIndex
            if (numActiveChildren == 1) {
                // done ^^
                children.first2 { it.isEnabled }
                    .buildShader(builder, posIndex, nextVariableId, dstIndex, uniforms, functions, seeds)
            } else {
                val tmpIndex = nextVariableId.next()
                builder.append("vec4 res").append(tmpIndex).append(";\n")
                if (type == CombinationMode.INTERPOLATION) {
                    appendInterpolation(
                        builder, posIndex, tmpIndex, nextVariableId,
                        dstIndex, uniforms, functions, seeds
                    )
                } else {
                    val (funcName, smoothness, groove, stairs) = appendGroupHeader(functions, uniforms, type, style)
                    var activeIndex = -1
                    for (index in children.indices) {
                        val child = children[index]
                        if (!child.isEnabled) continue
                        activeIndex++
                        child.buildShader(
                            builder, posIndex, nextVariableId,
                            if (activeIndex == 0) dstIndex else tmpIndex,
                            uniforms, functions, seeds
                        )
                        if (activeIndex > 0) {
                            appendMerge(builder, dstIndex, tmpIndex, funcName, smoothness, groove, stairs)
                        }
                    }
                }
            }
            // first scale or offset? offset, because it was applied after scaling
            val offsetName = trans.offsetName
            val scaleName = trans.scaleName
            if (offsetName != null)
                builder.append("res").append(dstIndex).append(".x+=").append(offsetName).append(";\n")
            if (scaleName != null)
                builder.append("res").append(dstIndex).append(".x*=").append(scaleName).append(";\n")
            if (localReliability != 1f)
                builder.append("res").append(dstIndex).append(".x*=")
                    .appendUniform(uniforms, GLSLType.V1F) { localReliability }.append(";\n")
            buildDMShader(builder, posIndex, dstIndex, nextVariableId, uniforms, functions, seeds)
        } else {
            builder.append("res").append(dstIndex).append("=vec4(Infinity,-1.0,0.0,0.0);\n")
            buildDMShader(builder, posIndex0, dstIndex, nextVariableId, uniforms, functions, seeds)
        }
    }

    fun appendInterpolation(
        builder: StringBuilder,
        posIndex: Int, tmpIndex: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val param1 = defineUniform(uniforms, GLSLType.V1F) { clamp(progress, 0f, children.size - 1f) }
        // helper functions
        functions.addAll(type.glslCode)
        val weightIndex = nextVariableId.next()
        builder.append("res").append(dstIndex).append("=vec4(Infinity,-1.0,0.0,0.0);\n")
        builder.append("float w").append(weightIndex).append(";\n")
        var activeIndex = 0
        for (index in children.indices) {
            val child = children[index]
            if (!child.isEnabled) continue
            // calculate weight and only compute child if weight > 0
            builder.append('w').append(weightIndex)
            builder.append("=1.0-abs(").append(param1).append('-').append(activeIndex).append(".0);\n")
            //builder.append("if(w").append(weightIndex).append(" > 0.0){\n")
            child.buildShader(builder, posIndex, nextVariableId, tmpIndex, uniforms, functions, seeds)
            builder.append("if(w").append(weightIndex)
                .append(" >= 0.5){\n") // assign material if this is the most dominant
            builder.append("res").append(dstIndex).append(".y=res").append(tmpIndex).append(".y;\n")
            builder.append("}\n")
            builder.append("res").append(dstIndex)
                .append(".x+=w").append(weightIndex)
                .append("*res").append(tmpIndex)
                .append(".x;\n")
            //builder.append("}\n")
            activeIndex++
        }
    }

    fun appendGroupHeader(
        functions: HashSet<String>,
        uniforms: HashMap<String, TypeValue>,
        type: CombinationMode,
        style: Style,
    ): Quad<String, String?, String?, String?> {
        functions.add(smoothMinCubic)
        val useSmoothness = dynamicSmoothness || smoothness > 0f
                || (style != Style.DEFAULT && type.isStyleable) ||
                when (type) {// types that require smoothness
                    CombinationMode.PIPE,
                    CombinationMode.ENGRAVE,
                    CombinationMode.GROOVE,
                    CombinationMode.TONGUE -> true
                    else -> false
                }
        val smoothness = if (useSmoothness) defineUniform(uniforms, GLSLType.V1F) { smoothness } else null
        // helper functions
        functions.addAll(type.glslCode)
        val funcName = when (type) {
            // if is styleable type, apply style
            CombinationMode.UNION -> {
                if (style != Style.DEFAULT) functions.add(hgFunctions)
                when (style) {
                    Style.COLUMNS -> "unionColumn"
                    Style.CHAMFER -> "unionChamfer"
                    Style.ROUND -> "unionRound"
                    Style.SOFT -> "unionSoft"
                    Style.STAIRS -> "unionStairs"
                    else -> "sdMin"
                }
            }
            CombinationMode.INTERSECTION,
            CombinationMode.DIFFERENCE1,
            CombinationMode.DIFFERENCE2 -> {
                if (style != Style.DEFAULT) functions.add(hgFunctions)
                when (style) {
                    Style.COLUMNS -> "interColumn"
                    Style.CHAMFER -> "interChamfer"
                    Style.ROUND -> "interRound"
                    Style.STAIRS -> "interStairs"
                    // no other types are supported
                    else -> "sdMax"
                }
            }
            else -> type.funcName
        }
        val groove = if (type == CombinationMode.GROOVE || type == CombinationMode.TONGUE) {
            defineUniform(uniforms, groove)
        } else null
        val stairs = if (
            funcName.contains("column", true) ||
            funcName.contains("stairs", true)
        ) {
            defineUniform(uniforms, GLSLType.V1F) { numStairs + 1f }
        } else null
        return Quad(funcName, smoothness, groove, stairs)
    }

    fun appendMerge(
        builder: StringBuilder,
        dstIndex: Int,
        tmpIndex: Int,
        funcName: String,
        smoothness: String?,
        groove: String?,
        stairs: String?
    ) {
        // we need to merge two values
        builder.append("res").append(dstIndex)
        builder.append('=')
        builder.append(funcName)
        builder.append('(')
        if (type == CombinationMode.DIFFERENCE2) {
            builder.appendMinus(dstIndex)
        } else {
            builder.append("res").append(dstIndex)
        }
        builder.append(',')
        if (type == CombinationMode.DIFFERENCE1) builder.appendMinus(tmpIndex)
        else builder.append("res").append(tmpIndex)
        when {
            groove != null -> builder.append(',').append(groove)
            smoothness != null -> builder.append(',').append(smoothness)
        }
        if (stairs != null) {
            builder.append(',').append(stairs)
        }
        builder.append(");\n")
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        return when (numActiveChildren) {
            0 -> Float.POSITIVE_INFINITY
            1 -> {
                val pw = pos.w
                pos.w = 0f
                return (children.first { it.isEnabled }.computeSDF(pos, seeds) + pw) * scale
            }
            else -> {
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
                            val d1 = child.computeSDF(pos, seeds)
                            d0 += d1 * weight
                        }// we could maybe exit early...

                    } else {

                        pos.set(px, py, pz, 0f)
                        val d1 = child.computeSDF(pos, seeds)
                        d0 = if (activeIndex == 0) d1
                        else when (type) {
                            CombinationMode.UNION -> sMinCubic(d0, d1, k)
                            CombinationMode.INTERSECTION -> sMaxCubic(d0, d1, k)
                            CombinationMode.DIFFERENCE1 -> sMaxCubic(+d0, -d1, k)
                            CombinationMode.DIFFERENCE2 -> sMaxCubic(-d0, +d1, k)
                            CombinationMode.DIFFERENCE_SYM -> sMaxCubic(sMinCubic(d0, d1, k), -sMaxCubic(d0, d1, k), k)
                            CombinationMode.INTERPOLATION -> {
                                if (activeIndex == 1) d0 *= max(1f - abs(progress), 0f)
                                d0 + d1 * max(1f - abs(progress - activeIndex), 0f)
                            }
                            CombinationMode.PIPE -> length(d0, d1) - smoothness
                            CombinationMode.TONGUE -> {
                                val g = groove
                                min(d0, max(d0 - g.x, abs(d1) - g.y))
                            }
                            CombinationMode.GROOVE -> {
                                val g = groove
                                max(d0, min(d0 + g.x, g.y - abs(d1)))
                            }
                            CombinationMode.ENGRAVE -> {
                                max(d0, (d0 + smoothness - abs(d1)) * SQRT1_2f)
                            }
                        }
                    }
                }
                return (d0 + pw) * scale
            }
        }
    }

    override fun findClosestComponent(pos: Vector4f, seeds: IntArrayList): SDFComponent {
        val children = children
        return when (numActiveChildren) {
            0 -> this
            1 -> {
                applyTransform(pos, seeds)
                children.first { it.isEnabled }.findClosestComponent(pos, seeds)
            }
            else -> {
                applyTransform(pos, seeds)
                val px = pos.x
                val py = pos.y
                val pz = pos.z
                val progress = progress
                // to do best match the GPU implementation, so we get equal results
                when (type) {
                    CombinationMode.UNION,
                    CombinationMode.PIPE,
                    CombinationMode.INTERSECTION,
                    CombinationMode.DIFFERENCE1,
                    CombinationMode.DIFFERENCE2,
                    CombinationMode.DIFFERENCE_SYM -> {
                        // find the closest child
                        val bestChild = children
                            .filter { it.isEnabled }
                            .minByOrNull {
                                pos.set(px, py, pz, 0f)
                                abs(it.computeSDF(pos, seeds))
                            }!!
                        pos.set(px, py, pz, 0f)
                        return bestChild.findClosestComponent(pos, seeds)
                    }
                    CombinationMode.INTERPOLATION -> {
                        // child with the largest weight
                        pos.set(px, py, pz, 0f)
                        val enabled = children.filter { it.isEnabled }
                        return enabled[clamp(progress.roundToInt(), 0, enabled.lastIndex)]
                            .findClosestComponent(pos, seeds)
                    }
                    CombinationMode.ENGRAVE,
                    CombinationMode.GROOVE,
                    CombinationMode.TONGUE -> {
                        var bestComp: SDFComponent? = null
                        val k = groove.y
                        for (index in children.indices) {
                            val child = children[index]
                            if (child.isEnabled) {
                                pos.set(px, py, pz, 0f)
                                val distance = child.computeSDF(pos, seeds)
                                if (bestComp == null || abs(distance) < k) {
                                    pos.set(px, py, pz, 0f)
                                    bestComp = child.findClosestComponent(pos, seeds)
                                }
                            }
                        }
                        return bestComp!!
                    }
                }
            }
        }
    }

    override fun onVisibleUpdate(): Boolean {
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (child.isEnabled) child.onVisibleUpdate()
        }
        return true
    }

    override fun onUpdate(): Int {
        super.onUpdate()
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (child.isEnabled) child.onUpdate()
        }
        return 1
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFGroup
        clone.smoothness = smoothness
        clone.progress = progress
        clone.type = type
        clone.style = style
        clone.groove.set(groove)
        clone.numStairs = numStairs
        clone.dynamicSmoothness = dynamicSmoothness
        clone.children.clear()
        clone.children.addAll(children.map {
            val child = it.clone() as SDFComponent
            child.parent = clone
            child
        })
    }

    override val className get() = "SDFGroup"

    companion object {

        // private val LOGGER = LogManager.getLogger(SDFGroup::class)

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

        const val hgFunctions = "" +
                // some of these types could use an additional material index for the intersection...
                // but we'll change the material system anyway
                "void pR45(inout vec2 p) {\n" +
                "   p = (p + vec2(p.y, -p.x))*sqrt(0.5);\n" +
                "}\n" +
                "float pMod1(inout float p, float size) {\n" +
                "   float halfSize = size * 0.5;\n" +
                "   float c = round(p/size);\n" +
                "   p = mod(p + halfSize, size) - halfSize;\n" +
                "   return c;\n" +
                "}\n" +
                "float unionChamfer(float a, float b, float r){\n" +
                "   return min(min(a,b),(a+b-r)*sqrt(0.5));\n" +
                "}\n" +
                "vec2 unionChamfer(vec2 a, vec2 b, float r){\n" +
                "   return vec2(unionChamfer(a.x,b.x,r),a.x+r<b.x?a.y:b.y);\n" +
                "}\n" +
                "float interChamfer(float a, float b, float r){\n" +
                "   return max(max(a,b),(a+b+r)*sqrt(0.5));\n" +
                "}\n" +
                "vec2 interChamfer(vec2 a, vec2 b, float r){\n" +
                "   return vec2(interChamfer(a.x,b.x,r),a.x>b.x?a.y:b.y);\n" +
                "}\n" +
                "float diffChamfer(float a, float b, float r){\n" +
                "   return interChamfer(a,-b,r);\n" +
                "}\n" +
                // quarter circle
                "float unionRound(float a, float b, float r){\n" +
                "   vec2 u = max(r-vec2(a,b), vec2(0.0));\n" +
                "   return max(r,min(a,b)) - length(u);\n" +
                "}\n" +
                "vec2 unionRound(vec2 a, vec2 b, float r){\n" +
                "   return vec2(unionRound(a.x,b.x,r),a.x<b.x?a.y:b.y);\n" +
                "}\n" +
                "float interRound(float a, float b, float r){\n" +
                "   vec2 u = max(r+vec2(a,b), vec2(0.0));\n" +
                "   return min(-r,max(a,b)) + length(u);" +
                "}\n" +
                "vec2 interRound(vec2 a, vec2 b, float r){\n" +
                "   return vec2(interRound(a.x,b.x,r),a.x>b.x?a.y:b.y);\n" +
                "}\n" +
                "float diffRound(float a, float b, float r){\n" +
                "   return interRound(a,-b,r);\n" +
                "}\n" +
                // n-1 circular columns at 45° angle
                "float unionColumn(float a, float b, float r, float n){\n" +
                "   if ((a < r) && (b < r)) {\n" +// todo is there a way to make this smooth over n?
                "      vec2 p = vec2(a, b);\n" +
                "      float columnRadius = r*sqrt(2.0)/((n-1.0)*2.0+sqrt(2.0));\n" +
                "      pR45(p);\n" +
                "      p.x -= sqrt(0.5)*r;\n" +
                "      p.x += columnRadius*sqrt(2.0);\n" +
                "      if (mod(n,2.0) >= 1.0) {\n" + // mmh..
                "         p.y += columnRadius;\n" +
                "      }\n" +
                // At this point, we have turned 45 degrees and moved at a point on the
                // diagonal that we want to place the columns on.
                // Now, repeat the domain along this direction and place a circle.
                "      pMod1(p.y, columnRadius*2.0);\n" +
                "      return min(length(p) - columnRadius, min(p.x, a));\n" +
                "   } else return min(a, b);\n" + // saving computations
                "}\n" +
                "vec2 unionColumn(vec2 a, vec2 b, float r, float n){\n" +
                // +r*(1.0-1.0/n)
                "   return vec2(unionColumn(a.x,b.x,r,n),a.x<b.x?a.y:b.y);\n" +
                "}\n" +
                // todo inter-column would need to have it's sign reversed...
                "float interColumn(float a, float b, float r, float n){\n" +
                "   return -unionColumn(-a,-b,r,n);\n" +
                "}\n" +
                "vec2 interColumn(vec2 a, vec2 b, float r, float n){\n" +
                "   return vec2(interColumn(a.x,b.x,r,n),a.x>b.x?a.y:b.y);\n" +
                "}\n" +
                "float unionStairs(float a, float b, float r, float n){\n" +
                "   float s = r/n;\n" +
                "   float u = b-r;\n" +
                "   return min(min(a,b), 0.5*(u+a+abs((mod(u-a+s, 2.0*s))-s)));" +
                "}\n" +
                "vec2 unionStairs(vec2 a, vec2 b, float r, float n){\n" +
                // +r*(1.0-1.0/n)
                "   return vec2(unionStairs(a.x,b.x,r,n),a.x<b.x?a.y:b.y);\n" +
                "}\n" +
                "float interStairs(float a, float b, float r, float n){\n" +
                "   return -unionStairs(-a,-b,r,n);\n" +
                "}\n" +
                "vec2 interStairs(vec2 a, vec2 b, float r, float n){\n" +
                "   return vec2(interStairs(a.x,b.x,r,n),a.x>b.x?a.y:b.y);\n" +
                "}\n" +
                "float unionSoft(float a, float b, float r){\n" +
                "   if(r <= 0.0) return min(a,b);\n" +
                "   float e = max(r-abs(a-b),0.0);\n" +
                "   return min(a,b)-e*e*0.25/r;\n" +
                "}\n" +
                "vec2 unionSoft(vec2 a, vec2 b, float r){\n" +
                "   return vec2(unionSoft(a.x,b.x,r),a.x<b.x?a.y:b.y);\n" +
                "}\n" +
                "float sdEngrave(float a, float b, float r){\n" +
                "   return max(a,(a+r-abs(b))*sqrt(0.5));\n" +
                "}\n" +
                "vec2 sdEngrave(vec2 a, vec2 b, float r){\n" +
                "   return vec2(sdEngrave(a.x,b.x,r),abs(b.x)<r?b.y:a.y);\n" +
                "}\n" +
                "float sdGroove(float a, float b, vec2 r){\n" +
                "   return max(a,min(a+r.x,r.y-abs(b)));\n" +
                "}\n" +
                "vec2 sdGroove(vec2 a, vec2 b, vec2 r){\n" +
                "   return vec2(sdGroove(a.x,b.x,r),abs(b.x)<r.y?b.y:a.y);\n" +
                "}\n" +
                "float sdTongue(float a, float b, vec2 r){\n" +
                "   return min(a,max(a-r.x,abs(b)-r.y));\n" +
                "}\n" +
                "vec2 sdTongue(vec2 a, vec2 b, vec2 r){\n" +
                "   return vec2(sdTongue(a.x,b.x,r),abs(b.x)<r.y?b.y:a.y);\n" +
                "}\n" +
                "float sdPipe(float a, float b, float r){\n" +
                "   return length(vec2(a,b))-r;\n" +
                "}\n" +
                "vec2 sdPipe(vec2 a, vec2 b, float r){\n" +
                "   return vec2(sdPipe(a.x,b.x,r),a.x<b.x?a.y:b.y);\n" +
                "}\n"

        const val smoothMinCubic = "" +
                // to do when we have material colors, use the first one as mixing parameter
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
                "vec4 sMinCubic2(vec4 a, vec4 b, float k){\n" +
                "   if(k <= 0.0) return (a.x<b.x) ? a : b;\n" +
                "   float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
                "   float m = h*h*h*0.5;\n" +
                "   float s = m*k*(1.0/3.0); \n" +
                "   return (a.x<b.x) ? vec4(a.x-s,a.yzw) : vec4(b.x-s,b.yzw);\n" +
                "}\n" +
                "vec4 sMaxCubic2(vec4 a, vec4 b, float k){\n" +
                "   if(k <= 0.0) return (a.x>b.x) ? a : b;\n" +
                "   float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
                "   float m = h*h*h*0.5;\n" +
                "   float s = m*k*(1.0/3.0); \n" +
                "   return (a.x>b.x) ? vec4(a.x+s,a.yzw) : vec4(b.x+s,b.yzw);\n" +
                "}\n"
        const val sdMin = "" +
                "float sdMin3(float a, float b, float c){ return min(a,min(b,c)); }\n" +
                "float sdMin3(float a, float b, float c, float k){ return sMinCubic1(a,sMinCubic1(b,c,k),k); }\n" +
                "float sdMin(float d1, float d2){ return min(d1,d2); }\n" +
                "vec4 sdMin(vec4 d1, vec4 d2){ return d1.x < d2.x ? d1 : d2; }\n" +
                "vec4 sdMin(vec4 d1, vec4 d2, float k){ return sMinCubic2(d1,d2,k); }\n"
        const val sdMax = "" +
                "float sdMax(float d1, float d2){ return max(d1,d2); }\n" +
                "vec4 sdMax(vec4 d1, vec4 d2){ return d1.x < d2.x ? d2 : d1; }\n" +
                "float sdMax(float d1, float d2, float k){ return sMaxCubic1(d1,d2,k); }\n" +
                "vec4 sdMax(vec4 d1, vec4 d2, float k){ return sMaxCubic2(d1,d2,k); }\n"
        const val sdDiff = "" +
                "vec4 sdDiff3(vec4 d1, vec4 d2){\n" +
                "  vec4 e1 = sdMin(d1,d2);\n" +
                "  vec4 e2 = sdMax(d1,d2);\n" +
                "  return sdDiff1(e1,e2); }\n" +
                "vec4 sdDiff3(vec4 d1, vec4 d2, float k){\n" +
                "  vec4 e1 = sdMin(d1,d2,k);\n" +
                "  vec4 e2 = sdMax(d1,d2,k);\n" +
                "  return sdDiff1(e1,e2,k); }\n"
        const val sdDiff1 = "" + // max(+-)
                "float sdDiff1(float d1, float d2){ return max(d1, -d2); }\n" +
                "float sdDiff1(float d1, float d2, float k){ return sdMax(d1, -d2, k); }\n" +
                "vec4 sdDiff1(vec2 d1, vec2 d2){ return sdMax(d1, vec4(-d2.x, d2.yzw)); }\n" +
                "vec4 sdDiff1(vec2 d1, vec2 d2, float k){ return sdMax(d1, vec4(-d2.x, d2.yzw), k); }\n"
        const val sdInt = "vec4 sdInt(vec4 sum, vec4 di, float weight){\n" +
                "weight = 1.0-abs(weight);\n" +
                "if(weight < 0.0) return sum;\n" +
                "return vec4(sum.x + di.x * weight, weight >= 0.5 ? di.yzw : sum.yzw); }\n"
    }

}