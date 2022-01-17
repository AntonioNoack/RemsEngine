package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.anim.Retargeting
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.assimp.AnimGameItem
import org.joml.Matrix4x3f
import org.lwjgl.opengl.GL21
import kotlin.math.min

class AnimRenderer : MeshComponent() {

    @Type("Skeleton/Reference")
    @SerializedProperty
    var skeleton: FileReference = InvalidRef

    // maybe not the most efficient way, but it should work :)
    @Type("Map<Animation/Reference,Float>")
    @SerializedProperty
    var animationWeights = HashMap<FileReference, Float>()

    override fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {

        val skeleton = SkeletonCache[skeleton]
        if (skeleton == null) {
            shader.v1b("hasAnimation", false)
            return
        }

        shader.use()

        val location = shader["jointTransforms"]

        // the programmer must be careful.. or does he? idk...
        animationWeights.values.removeIf { it <= 0f }
        // animationWeights.removeIf { it.second <= 0f }

        // todo remove that; just for debugging
        if (animationWeights.isEmpty() && skeleton.animations.isNotEmpty()) {
            val sample = skeleton.animations.entries.first().value
            animationWeights[sample] = 1f
        }

        if (animationWeights.isEmpty() || location <= 0) {
            shader.v1b("hasAnimation", false)
            return
        }

        val time = GFX.gameTime / 1e9f
        // todo find retargeting from the skeleton to the new skeleton...
        // todo if not found, generate it automatically, and try our best to do it perfectly
        // todo retargeting probably needs to include a max/min-angle and angle multiplier and change of base matrices
        // (or all animations need to be defined in some common animation space)
        val retargeting = Retargeting()

        // what if the weight is less than 1? change to T-pose? no, the programmer can define that himself with an animation
        // val weightNormalization = 1f / max(1e-7f, animationWeights.values.sum())
        val keys = animationWeights.keys.iterator()
        val values = animationWeights.values.iterator()
        val key0 = keys.next()
        val animation0 = AnimationCache[key0]!!
        val matrices = animation0.getMappedMatricesSafely(entity, time, dst0, retargeting)
        var sumWeight = values.next()
        while (keys.hasNext()) {
            val weightAnim = keys.next()
            val weight = values.next()
            val relativeWeight = weight / (sumWeight + weight)
            // todo the second animation may have a different time value -> we need to manage that...
            val animationI = AnimationCache[weightAnim] ?: continue
            val secondMatrices = animationI.getMappedMatricesSafely(entity, time, dst1, retargeting)
            for (j in matrices.indices) {
                matrices[j].lerp(secondMatrices[j], relativeWeight)
            }
            sumWeight += weight
        }

        shader.v1b("hasAnimation", true)

        // upload the matrices
        upload(location, matrices)
        /*val boneCount = min(matrices.size, AnimGameItem.maxBones)
        AnimGameItem.matrixBuffer.limit(AnimGameItem.matrixSize * boneCount)
        for (index in 0 until boneCount) {
            val matrix0 = matrices[index]
            AnimGameItem.matrixBuffer.position(index * AnimGameItem.matrixSize)
            AnimGameItem.get(matrix0, AnimGameItem.matrixBuffer)
        }
        AnimGameItem.matrixBuffer.position(0)
        GL21.glUniformMatrix4x3fv(location, false, AnimGameItem.matrixBuffer)*/

        // get skeleton
        // get animation
        // blend the relevant animations together

    }

    override fun clone(): AnimRenderer {
        val clone = AnimRenderer()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as AnimRenderer
        clone.skeleton = skeleton
        clone.animationWeights = animationWeights
    }

    override fun onDrawGUI() {
        val skeleton = SkeletonCache[skeleton]
        if (skeleton != null) {
            val shader = pbrModelShader.value
            skeleton.draw(shader, Matrix4x3f(), null)
        }
    }

    override val className: String = "AnimRenderer"

    companion object {

        val dst0 = Array(256) { Matrix4x3f() }
        val dst1 = Array(256) { Matrix4x3f() }

        fun upload(location: Int, matrices: Array<Matrix4x3f>) {
            val boneCount = min(matrices.size, AnimGameItem.maxBones)
            AnimGameItem.matrixBuffer.limit(AnimGameItem.matrixSize * boneCount)
            for (index in 0 until boneCount) {
                val matrix0 = matrices[index]
                AnimGameItem.matrixBuffer.position(index * AnimGameItem.matrixSize)
                AnimGameItem.get(matrix0, AnimGameItem.matrixBuffer)
            }
            AnimGameItem.matrixBuffer.position(0)
            GL21.glUniformMatrix4x3fv(location, false, AnimGameItem.matrixBuffer)
        }

    }

}