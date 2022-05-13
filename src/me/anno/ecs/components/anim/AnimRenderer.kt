package me.anno.ecs.components.anim

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.config.DefaultStyle.white4
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.assimp.AnimGameItem
import me.anno.utils.pooling.JomlPools
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
            anim.progress = anim.repeat[anim.progress, duration]
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

    override val hasAnimation: Boolean
        get() {
            val skeleton = SkeletonCache[skeleton]
            return skeleton != null && animations.isNotEmpty()
        }

    override fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {

        val skeleton = SkeletonCache[skeleton]
        if (skeleton == null) {
            shader.v1b("hasAnimation", false)
            lastWarning = "Skeleton missing"
            return
        }

        shader.use()

        // check whether the shader actually uses bones
        val location = shader[if (useAnimTextures) "animWeights" else "jointTransforms"]

        // todo remove that; just for debugging
        if (animations.isEmpty() && skeleton.animations.isNotEmpty()) {
            val sample = skeleton.animations.entries.first().value
            animations.add(AnimationState(sample, 0f, 0f, 0f, LoopingState.PLAY_LOOP))
        }

        if (animations.isEmpty()) {
            lastWarning = "No animation was found"
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
        if (useAnimTextures) {

            // find major weights & indices in anim texture
            val animTexture = AnimationCache[skeleton]
            val bestWeights = JomlPools.vec4f.create().set(0f)
            val bestIndices = JomlPools.vec4f.create().set(0f)
            var writeIndex = 0
            for (index in animations.indices) {
                val animState = animations[index]
                if (animState.weight > bestWeights[bestWeights.maxComponent()]) {
                    val animation = AnimationCache[animState.source] ?: continue
                    val frameIndex = animState.progress / animation.duration * animation.numFrames
                    val internalIndex = animTexture.getIndex(animation, retargeting, frameIndex)
                    val weight = animState.weight
                    if (writeIndex < 4) {
                        bestIndices.setComponent(writeIndex, internalIndex)
                        bestWeights.setComponent(writeIndex, weight)
                        writeIndex++
                    } else {
                        val nextIndex = bestWeights.minComponent()
                        bestIndices.setComponent(nextIndex, internalIndex)
                        bestWeights.setComponent(nextIndex, weight)
                    }
                }
            }

            // normalize weights
            bestWeights.div(max(1e-7f, bestWeights.dot(white4)))

            shader.v4f("animWeights", bestWeights)
            shader.v4f("animIndices", bestIndices)
            (animTexture.getTexture() ?: whiteTexture)
                .bind(shader, "animTexture", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)

            JomlPools.vec4f.sub(2)

        } else {

            lateinit var matrices: Array<Matrix4x3f>
            var sumWeight = 0f
            for (index in animations.indices) {
                val anim = animations[index]
                val weight = anim.weight
                val relativeWeight = weight / (sumWeight + weight)
                val time = anim.progress
                val animationI = AnimationCache[anim.source] ?: continue
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

            // upload the matrices
            upload(location, matrices)

        }

        shader.v1b("hasAnimation", true)

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

    override fun onDrawGUI(all: Boolean) {
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