package me.anno.sdf.modifiers

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.appendVec
import me.anno.sdf.SDFComponent.Companion.globalDynamic
import me.anno.sdf.VariableCounter
import me.anno.maths.Maths
import me.anno.maths.Maths.TAUf
import org.joml.AABBf
import org.joml.Vector4f
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sin

class SDFColumn : DistanceMapper() {

    val params = Vector4f(0.05f, 12f, 0f, 2f)

    // todo make axis flexible, e.g. by rotation
    var rotary = true
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var amplitude
        get() = params.x
        set(value) {
            if (params.x != value) {
                if (!dynamic && !globalDynamic) invalidateShader()
                else invalidateBounds()
                params.x = value
            }
        }

    var frequency
        get() = params.y
        set(value) {
            if (params.y != value) {
                if (!dynamic && !globalDynamic) invalidateShader()
                params.y = value
            }
        }

    @Range(0.0, Maths.TAU)
    var phaseOffset
        get() = params.z
        set(value) {
            if (params.z != value) {
                if (!dynamic && !globalDynamic) invalidateShader()
                params.z = value
            }
        }

    @Docs("Like sharpness")
    @Range(1e-7, 1e7)
    var power
        get() = params.w
        set(value) {
            if (params.w != value) {
                if (!dynamic && !globalDynamic) invalidateShader()
                params.w = value
            }
        }

    var dynamic = false

    var fixDiscontinuity = false
        set(value) {
            if (field != value && rotary) invalidateShader()
            field = value
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        functions.add(sdColumn)
        builder.append("res").append(dstIndex).append(".x-=")
        if (rotary && fixDiscontinuity) {
            builder.append("min(1.0,length(pos").append(posIndex)
            builder.append(".xz))*")
        }
        builder.append("sdColumn(")
        val dynamic = dynamic || globalDynamic
        if (rotary) {
            builder.append("atan(pos").append(posIndex).append(".z,pos").append(posIndex).append(".x),")
        } else {
            builder.append("pos").append(posIndex).append(".y,")
        }
        if (dynamic) builder.appendUniform(uniforms, params)
        else builder.appendVec(params)
        builder.append(");\n")
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        val base = if (rotary) atan2(pos.z, pos.x) else pos.y
        val angle = (base * frequency + phaseOffset) * TAUf
        val value = amplitude * (0.5f * sin(angle) + 0.5f).pow(power)
        return distance - value
    }

    override fun applyTransform(bounds: AABBf) {
        val delta = amplitude
        if (delta > 0f) {
            // not the most accurate, but probably good enough
            bounds.addMargin(delta)
        }
    }

    override val className: String get() = "SDFColumn"

    companion object {
        // inspired by Greek Temple (https://www.shadertoy.com/view/ldScDh), by Inigo Quilez
        const val sdColumn = "" +
                "float sdColumn(float a, vec4 params){\n" +
                "   return params.x * pow(0.5+0.5*sin(params.y * a + params.z), params.w);\n" +
                "}\n"
    }

}