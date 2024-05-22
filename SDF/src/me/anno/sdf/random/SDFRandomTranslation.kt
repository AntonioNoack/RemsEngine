package me.anno.sdf.random

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.mix
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.appendVec
import me.anno.sdf.SDFComponent.Companion.globalDynamic
import me.anno.sdf.VariableCounter
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

class SDFRandomTranslation : SDFRandom() {

    var minTranslation: Vector3f = Vector3f(0f, -1f, 0f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    var maxTranslation: Vector3f = Vector3f(0f, +1f, 0f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seed: String
    ): String? {
        builder.append("pos").append(posIndex).append("-=").append("mix(")
        if (dynamic || globalDynamic) {
            builder.appendUniform(uniforms, minTranslation).append(',')
                .appendUniform(uniforms, maxTranslation)
        } else {
            builder.appendVec(minTranslation).append(',')
                .appendVec(maxTranslation)
        }
        builder.append(",nextRandF3(").append(seed).append("));\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seed: Int) {
        val s1 = nextRandI(seed)
        val s2 = nextRandI(s1)
        pos.sub(
            mix(minTranslation.x, maxTranslation.x, nextRandF(seed)),
            mix(minTranslation.y, maxTranslation.y, nextRandF(s1)),
            mix(minTranslation.z, maxTranslation.z, nextRandF(s2)), 0f
        )
    }

    override fun applyTransform(bounds: AABBf) {
        val avgX = (minTranslation.x + maxTranslation.x) * 0.5f
        val avgY = (minTranslation.y + maxTranslation.y) * 0.5f
        val avgZ = (minTranslation.z + maxTranslation.z) * 0.5f
        val dltX = abs(maxTranslation.x - minTranslation.x) * 0.5f
        val dltY = abs(maxTranslation.y - minTranslation.y) * 0.5f
        val dltZ = abs(maxTranslation.z - minTranslation.z) * 0.5f
        bounds.minX += avgX - dltX
        bounds.minY += avgY - dltY
        bounds.minZ += avgZ - dltZ
        bounds.maxX += avgX + dltX
        bounds.maxY += avgY + dltY
        bounds.maxZ += avgZ + dltZ
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFRandomTranslation
        dst.minTranslation = minTranslation
        dst.maxTranslation = maxTranslation
    }
}