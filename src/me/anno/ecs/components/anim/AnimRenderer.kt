package me.anno.ecs.components.anim

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.assimp.AnimGameItem
import org.joml.Matrix4x3f
import org.lwjgl.opengl.GL21
import kotlin.math.max
import kotlin.math.min

open class AnimRenderer : MeshComponent() {

    @Docs("Maps bone indices to names & hierarchy")
    @Type("Skeleton/Reference")
    @SerializedProperty
    var skeleton: FileReference = InvalidRef

    // maybe not the most efficient way, but it should work :)
    @Docs("Maps time & bone index onto local transform")
    @Type("List<AnimationState>")
    @SerializedProperty
    var animations = ArrayList<AnimationState>()

    open fun onAnimFinished(anim: AnimationState) {
        val instance = AnimationCache[anim.source]
        if (instance != null) {
            val duration = instance.duration
            anim.progress = anim.repeat[anim.progress.toDouble(), duration].toFloat()
        }
    }

    override fun onUpdate(): Int {
        // update all weights
        val dt = Engine.deltaTime
        var anyIsRunning = false
        for (index in animations.indices) {
            val anim = animations[index]
            anim.update(this, dt, true)
            if (anim.speed != 0f) anyIsRunning = true
        }
        return if (anyIsRunning) 1 else 10
    }

    override fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {

        val skeleton = SkeletonCache[skeleton]
        if (skeleton == null) {
            shader.v1b("hasAnimation", false)
            return
        }

        shader.use()

        val location = shader["jointTransforms"]

        // todo remove that; just for debugging
        if (animations.isEmpty() && skeleton.animations.isNotEmpty()) {
            val sample = skeleton.animations.entries.first().value
            animations.add(AnimationState(sample, 0f, 0f, 0f, LoopingState.PLAY_LOOP))
        }

        if (animations.isEmpty() || location <= 0) {
            shader.v1b("hasAnimation", false)
            return
        }

        // todo find retargeting from the skeleton to the new skeleton...
        // todo if not found, generate it automatically, and try our best to do it perfectly
        // todo retargeting probably needs to include a max/min-angle and angle multiplier and change of base matrices
        // (or all animations need to be defined in some common animation space)
        val retargeting = Retargeting()

        // what if the weight is less than 1? change to T-pose? no, the programmer can define that himself with an animation
        // val weightNormalization = 1f / max(1e-7f, animationWeights.values.sum())
        val animations = animations
        lateinit var matrices: Array<Matrix4x3f>
        var sumWeight = 0f
        for (index in animations.indices) {
            val anim = animations[index]
            val weightAnim = anim.source
            val weight = anim.weight
            val relativeWeight = weight / (sumWeight + weight)
            val time = anim.progress
            val animationI = AnimationCache[weightAnim] ?: continue
            if (index == 0) {
                matrices = animationI.getMappedMatricesSafely(entity, time, dst0, retargeting)
            } else if (relativeWeight > 0f) {
                val matrix = animationI.getMappedMatricesSafely(entity, time, dst1, retargeting)
                for (j in matrices.indices) {
                    matrices[j].lerp(matrix[j], relativeWeight)
                }
            }
            sumWeight += max(0f, weight)
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
        clone.animations = animations
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