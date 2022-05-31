package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import org.joml.AABBf
import org.joml.Vector4f

/**
 * makes the object thicker, and makes it round by doing so
 * */
class SDFRoundness : DistanceMapper() {

    var roundness = 0.1f
        set(value) {
            if (field != value) {
                if (dynamic || globalDynamic) invalidateBounds()
                else invalidateShader()
                field = value
            }
        }

    var dynamic = false
        set(value) {
            if (field != value) {
                field = value
                if (!globalDynamic) invalidateShader()
            }
        }

    override fun applyTransform(bounds: AABBf) {
        // expand by roundness
        bounds.addMargin(roundness)
    }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        builder.append("res").append(dstIndex).append(".x-=")
        val dynamic = dynamic || globalDynamic
        if (dynamic) builder.appendUniform(uniforms, GLSLType.V1F) { roundness }
        else builder.append(roundness)
        builder.append(";\n")
    }

    override fun calcTransform(pos: Vector4f, distance: Float): Float {
        return distance - roundness
    }

    override fun clone(): SDFRoundness {
        val clone = SDFRoundness()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFRoundness
        clone.roundness = roundness
        clone.dynamic = dynamic
    }
}