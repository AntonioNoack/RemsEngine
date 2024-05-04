package me.anno.sdf

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.clamp
import me.anno.sdf.SDFCombiningFunctions.hgFunctions
import me.anno.sdf.SDFCombiningFunctions.smoothMinCubic
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.structures.lists.Lists.first2
import me.anno.utils.structures.lists.Lists.last2
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * joins multiple sdf components together, like a folder;
 * can also join them using special operations like subtraction, xor, and logical and (see type)
 *
 * todo this class is quite long, it would be nice if we could move more stuff into CombinationMode/JoiningStyle
 * */
open class SDFGroup : SDFComponent() {

    var combinationMode = CombinationMode.UNION
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var joiningStyle = JoiningStyle.DEFAULT
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
    override fun getValidTypesForChild(child: PrefabSaveable): String =
        if (child is SDFComponent) "c" else super.getValidTypesForChild(child)

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
                combinationMode != CombinationMode.INTERPOLATION
            ) invalidateShader()
            field = value
        }

    // currently this is always dynamic -> no shader check needed
    @Range(0.0, 2e9)
    var progress = 0.5f
        set(value) {
            if (field != value) {
                if (combinationMode == CombinationMode.INTERPOLATION) invalidateBounds()
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

    private val numActiveChildren: Int get() = children.count2 { it.isEnabled }

    override fun calculateBaseBounds(dst: AABBf) {
        // for now, just use the worst case
        calculateBaseBounds(dst, children)
    }

    fun calculateBaseBounds(dst: AABBf, children: List<SDFComponent>) {
        when (val size = children.count2 { it.isEnabled }) {
            0 -> {}
            1 -> children.first2 { it.isEnabled }.calculateBounds(dst)
            else -> {
                val tmp = JomlPools.aabbf.create()
                when (combinationMode) {
                    // we could try to subtract one bbx from another
                    // we also probably should merge all others first, so the subtraction is maximal
                    // -> no, a bbx is a BOUNDING box: the actually subtracted amount may be smaller
                    // ... we could set a flag, whether we're allowed to subtract :)
                    CombinationMode.DIFFERENCE1,
                    CombinationMode.ENGRAVE,
                    CombinationMode.GROOVE -> { // use first only, the rest is cut off
                        children.first2 { it.isEnabled }.calculateBounds(dst)
                        if (combinationMode == CombinationMode.GROOVE) dst.addMargin(smoothness)
                    }
                    CombinationMode.TONGUE -> { // use first only, then widen
                        children.first2 { it.isEnabled }.calculateBounds(dst)
                        dst.addMargin(smoothness)
                    }
                    CombinationMode.DIFFERENCE2 -> {// use last only, the rest is cut off
                        children.last2 { it.isEnabled }.calculateBounds(dst)
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
                        if (combinationMode == CombinationMode.PIPE) dst.addMargin(smoothness)
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
        buildShader1(builder, posIndex0, nextVariableId, dstIndex, uniforms, functions, seeds, children, true)
    }

    fun buildShader1(
        builder: StringBuilder,
        posIndex0: Int, nextVariableId: VariableCounter, dstIndex: Int,
        uniforms: HashMap<String, TypeValue>, functions: HashSet<String>,
        seeds: ArrayList<String>, children: List<SDFComponent>,
        applyTransform: Boolean
    ) {
        val enabledChildCount = children.count2 { it.isEnabled }
        if (enabledChildCount > 0) {
            val type = combinationMode
            val transform = if (applyTransform) buildTransform(
                builder,
                posIndex0,
                nextVariableId,
                uniforms,
                functions,
                seeds
            ) else null
            val posIndex = transform?.posIndex ?: posIndex0
            if (enabledChildCount == 1) {
                // done ^^
                children.first2 { it.isEnabled }
                    .buildShader(builder, posIndex, nextVariableId, dstIndex, uniforms, functions, seeds)
            } else {
                val tmpIndex = nextVariableId.next()
                builder.append("vec4 res").append(tmpIndex).append(";\n")
                if (type == CombinationMode.INTERPOLATION) {
                    appendInterpolation(
                        builder, posIndex, tmpIndex, nextVariableId,
                        dstIndex, uniforms, functions, seeds,
                        children
                    )
                } else {
                    val (funcName, smoothness, groove, stairs) = appendGroupHeader(
                        functions,
                        uniforms,
                        type,
                        joiningStyle
                    )
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
            if (transform != null) buildTransformCorrection(builder, transform, dstIndex, uniforms)
            buildDistanceMapperShader(builder, posIndex, dstIndex, nextVariableId, uniforms, functions, seeds)
        } else {
            builder.append("res").append(dstIndex).append("=vec4(Infinity,-1.0,0.0,0.0);\n")
            buildDistanceMapperShader(builder, posIndex0, dstIndex, nextVariableId, uniforms, functions, seeds)
        }
    }

    fun buildTransformCorrection(
        builder: StringBuilder,
        trans: SDFTransform,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>
    ) {
        val offsetName = trans.offsetName
        val scaleName = trans.scaleName
        if (offsetName != null)
            builder.append("res").append(dstIndex).append(".x+=").append(offsetName).append(";\n")
        if (scaleName != null)
            builder.append("res").append(dstIndex).append(".x*=").append(scaleName).append(";\n")
        if (localReliability != 1f)
            builder.append("res").append(dstIndex).append(".x*=")
                .appendUniform(uniforms, GLSLType.V1F) { localReliability }.append(";\n")
    }

    fun appendInterpolation(
        builder: StringBuilder,
        posIndex: Int, tmpIndex: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>,
        children: List<SDFComponent>
    ) {
        val param1 = defineUniform(uniforms, GLSLType.V1F) {
            clamp(progress, 0f, children.size - 1f)
        }
        // helper functions
        functions.addAll(combinationMode.glslCode)
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
            // assign material if this is the most dominant
            val needsCondition = activeIndex > 0
            if (needsCondition) builder.append("if(w").append(weightIndex).append(" >= 0.5){\n")
            builder.append("res").append(dstIndex).append(".yzw=res").append(tmpIndex).append(".yzw;\n")
            if (needsCondition) builder.append("}\n")
            builder.append("res").append(dstIndex)
                .append(if (needsCondition) ".x+=w" else ".x=w").append(weightIndex)
                .append("*res").append(tmpIndex)
                .append(".x;\n")
            activeIndex++
        }
    }

    data class GroupHeader(
        val funcName: String, val smoothness: String?,
        val groove: String?, val stairs: String?
    )

    fun appendGroupHeader(
        functions: HashSet<String>,
        uniforms: HashMap<String, TypeValue>,
        type: CombinationMode,
        style: JoiningStyle,
    ): GroupHeader {
        functions.add(smoothMinCubic)
        val useSmoothness = dynamicSmoothness || smoothness > 0f
                || (style != JoiningStyle.DEFAULT && type.isStyleable) ||
                when (type) {// types that require smoothness
                    CombinationMode.PIPE,
                    CombinationMode.ENGRAVE,
                    CombinationMode.GROOVE,
                    CombinationMode.TONGUE -> true
                    else -> false
                }
        val smoothness = if (useSmoothness) defineUniform(
            uniforms,
            GLSLType.V1F
        ) { smoothness } else null
        // helper functions
        functions.addAll(type.glslCode)
        val funcName = when (type) {
            // if is styleable type, apply style
            CombinationMode.UNION -> {
                if (style != JoiningStyle.DEFAULT) functions.add(hgFunctions)
                style.unionName
            }
            CombinationMode.INTERSECTION,
            CombinationMode.DIFFERENCE1,
            CombinationMode.DIFFERENCE2 -> {
                if (style != JoiningStyle.DEFAULT) functions.add(hgFunctions)
                style.interName
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
        return GroupHeader(funcName, smoothness, groove, stairs)
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
        if (combinationMode == CombinationMode.DIFFERENCE2) {
            builder.appendMinus(dstIndex)
        } else {
            builder.append("res").append(dstIndex)
        }
        builder.append(',')
        if (combinationMode == CombinationMode.DIFFERENCE1) builder.appendMinus(tmpIndex)
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
        return when (children.count2 { it.isEnabled }) {
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
                val combinationMode = combinationMode

                var activeIndex = -1
                for (index in children.indices) {

                    val child = children[index]
                    if (!child.isEnabled) continue
                    activeIndex++

                    if (combinationMode == CombinationMode.INTERPOLATION) {
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
                        else combinationMode.combine(d0, d1, k, this)
                    }
                }
                return (d0 + pw) * scale
            }
        }
    }

    override fun findClosestComponent(pos: Vector4f, seeds: IntArrayList): SDFComponent {
        val children = children
        return when (children.count2 { it.isEnabled }) {
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
                when (combinationMode) {
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

    override fun findDrawnSubject(searchedId: Int): Any? {
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled) {
                // LOGGER.debug("[S] ${child.clickId.toString(16)} vs ${searchedId.toString(16)}")
                if (child.clickId == searchedId) return child
                if (child is SDFGroup) {
                    val found = child.findDrawnSubject(searchedId)
                    if (found != null) return found
                }
            }
        }
        return null
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

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFGroup
        dst.smoothness = smoothness
        dst.progress = progress
        dst.combinationMode = combinationMode
        dst.joiningStyle = joiningStyle
        dst.groove.set(groove)
        dst.numStairs = numStairs
        dst.dynamicSmoothness = dynamicSmoothness
        dst.children.clear()
        dst.children.addAll(children.map {
            val child = it.clone() as SDFComponent
            child.parent = dst
            child
        })
    }
}