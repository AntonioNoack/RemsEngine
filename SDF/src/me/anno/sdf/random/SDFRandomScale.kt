package me.anno.sdf.random

import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.appendVec
import me.anno.sdf.SDFComponent.Companion.globalDynamic
import me.anno.sdf.VariableCounter
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

class SDFRandomScale : SDFRandom() {

    @Docs("Should be less or equal to 1.0")
    var minScale: Vector3f = Vector3f(0.7f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    @Docs("Should stay near 1.0 for stability reasons")
    var maxScale: Vector3f = Vector3f(1f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    @Docs("Whether all axes are affected the same on each instance")
    var uniformScaling = true
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seed: String
    ): String? {
        builder.append("pos").append(posIndex).append("*=").append("mix(")
        if (dynamic || globalDynamic) {
            builder.appendUniform(uniforms, minScale).append(',')
                .appendUniform(uniforms, maxScale)
        } else {
            builder.appendVec(minScale).append(',')
                .appendVec(maxScale)
        }
        if (uniformScaling) {
            builder.append(",vec3(nextRandF1(")
        } else {
            builder.append(",(nextRandF3(")
        }
        builder.append(seed).append(")));\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seed: Int) {
        // todo apply transform here (?)
    }

    override fun applyTransform(bounds: AABBf) {
        val avgX = (minScale.x + maxScale.x) * 0.5f
        val avgY = (minScale.y + maxScale.y) * 0.5f
        val avgZ = (minScale.z + maxScale.z) * 0.5f
        val dltX = abs(maxScale.x - minScale.x) * 0.5f
        val dltY = abs(maxScale.y - minScale.y) * 0.5f
        val dltZ = abs(maxScale.z - minScale.z) * 0.5f
        bounds.minX += avgX - dltX
        bounds.minY += avgY - dltY
        bounds.minZ += avgZ - dltZ
        bounds.maxX += avgX + dltX
        bounds.maxY += avgY + dltY
        bounds.maxZ += avgZ + dltZ
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFRandomScale
        dst.minScale = minScale
        dst.maxScale = maxScale
    }
}