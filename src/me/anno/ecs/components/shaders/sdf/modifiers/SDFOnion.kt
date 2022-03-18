package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.shaders.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.fract
import org.joml.Vector4f
import kotlin.math.abs

@Suppress("unused")
class SDFOnion() : DistanceMapper() {

    constructor(thickness: Float, rings: Int = 1) : this() {
        this.thickness = thickness
        this.rings = rings
    }

    var thickness = 0.1f
    var dynamicThickness = false

    var rings = 1
    var dynamicRings = false

    val slices get() = rings * 2 - 1

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstName: String,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        functions.add(sdRings)
        builder.append(dstName).append(".x=sdRings(")
        builder.append(dstName).append(".x,")
        if (dynamicThickness) builder.append(defineUniform(uniforms, GLSLType.V1F, { thickness }))
        else builder.append(thickness)
        if (dynamicRings || rings != 1) {
            builder.append(',')
            if (dynamicRings) builder.append(defineUniform(uniforms, GLSLType.V1F, { slices.toFloat() }))
            else builder.append(slices).append(".0")
        }
        builder.append(");\n")
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        var t = thickness
        val rings = rings
        return if (rings == 1) {
            t *= 0.5f
            abs(distance + t) - t
        } else {
            if (distance >= 0f) return distance
            val s = 2f * rings + 1f
            val ts = t * s
            if (-distance >= ts) return -(distance + ts)
            val ri = distance / t
            (0.5f - abs(fract(ri) - 0.5f)) * ((ri.toInt().and(1) * 2) - 1) * t
        }
    }

    override fun clone(): SDFOnion {
        val clone = SDFOnion()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFOnion
        clone.rings = rings
        clone.thickness = thickness
        clone.dynamicThickness = dynamicThickness
    }

    override val className: String = "SDFOnion"

    companion object {
        const val sdRings = "" +
                "float sdRings(float d, float t){\n" +
                "   t *= 0.5;\n" +
                "   return abs(d+t)-t;\n" +
                "}\n" +
                "float sdRings(float d, float t, float s){\n" +
                "   if( d >= 0.0) return d;\n" +
                "   if(-d >= t*s) return -(d+t*s);\n" +
                "   float ri = d/t;\n" +
                "   return (0.5-abs(fract(ri)-0.5)) * float(((int(ri)&1)<<1)-1) * t;\n" +
                "}\n"
    }

}