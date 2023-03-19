package me.anno.ecs.components.mesh.sdf.random

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendVec
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.arrays.SDFArray
import me.anno.ecs.components.mesh.sdf.shapes.SDFBox
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXBase
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

class SDFRandomTranslation : SDFRandom() {

    var minTranslation = Vector3f(0f, -1f, 0f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }
    var maxTranslation = Vector3f(0f, +1f, 0f)
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
        // todo apply transform here (?)
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

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFRandomTranslation
        clone.minTranslation = minTranslation
        clone.maxTranslation = maxTranslation
    }

    override val className get() = "SDFRandomTranslation"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ECSRegistry.init()

            val entity = Entity()

            val array = SDFArray()
            array.cellSize.set(2f)
            array.count.set(10, 1, 10)

            val shape = SDFBox()
            shape.addChild(array)
            shape.addChild(SDFRandomTranslation())
            entity.addChild(shape)

            GFXBase.disableRenderDoc()
            testSceneWithUI(entity)
        }
    }

}