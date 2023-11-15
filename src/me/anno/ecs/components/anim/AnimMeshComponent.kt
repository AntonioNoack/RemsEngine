package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.raycast.RaycastSkeletal
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.ui.editor.sceneView.Gizmos
import org.joml.Matrix4x3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a skeletal animation
 * */
open class AnimMeshComponent : MeshComponent() {

    // todo in debug mode, we could render the skeleton as well/instead :)

    @Docs("Maps bone indices to names & hierarchy")
    @Type("Skeleton/Reference")
    @SerializedProperty
    var skeleton: FileReference = InvalidRef

    // maybe not the most efficient way, but it should work :)
    @Docs("Maps time & bone index onto local transform")
    @Type("List<AnimationState>")
    @SerializedProperty
    var animations: List<AnimationState> = emptyList()

    @Docs("If no animation is set, use default?")
    var useDefaultAnimation = true

    // animation state for motion vectors
    @NotSerializedProperty
    var prevTime = 0L

    @NotSerializedProperty
    val prevWeights = Vector4f()

    @NotSerializedProperty
    val prevIndices = Vector4f()

    @NotSerializedProperty
    val currWeights = Vector4f()

    @NotSerializedProperty
    val currIndices = Vector4f()

    open fun onAnimFinished(anim: AnimationState) {
        val instance = AnimationCache[anim.source]
        if (instance != null) {
            val duration = instance.duration
            anim.progress = anim.repeat[anim.progress, duration]
        }
    }

    private var lastUpdate = 0L
    override fun onUpdate(): Int {
        // update all weights
        return if (lastUpdate != Time.gameTimeN) {
            lastUpdate = Time.gameTimeN
            val dt = Time.deltaTime.toFloat()
            var anyIsRunning = false
            for (index in animations.indices) {
                val anim = animations[index]
                anim.update(this, dt, true)
                if (anim.speed != 0f) anyIsRunning = true
            }
            updateAnimState()
            if (anyIsRunning) 1 else 10
        } else 1
    }

    override val hasAnimation: Boolean
        get() {
            val skeleton = SkeletonCache[skeleton]
            return skeleton != null && (useDefaultAnimation || animations.isNotEmpty())
        }

    fun addState(state: AnimationState) {
        synchronized(this) {
            val animations = animations
            if (animations is MutableList) {
                animations.add(state)
            } else {
                val newList = ArrayList<AnimationState>(animations.size + 4)
                newList.addAll(animations)
                newList.add(state)
                this.animations = newList
            }
        }
    }

    override fun defineVertexTransform(shader: Shader, entity: Entity, mesh: IMesh): Boolean {

        val skeleton = SkeletonCache[skeleton]
        if (skeleton == null) {
            lastWarning = "Skeleton missing"
            return false
        }

        shader.use()

        // check whether the shader actually uses bones
        val location = shader[if (useAnimTextures) "animWeights" else "jointTransforms"]

        if (useDefaultAnimation && animations.isEmpty() && skeleton.animations.isNotEmpty()) {
            val sample = skeleton.animations.entries.firstOrNull()?.value
            if (sample != null) {
                addState(AnimationState(sample, 0f, 0f, 0f, LoopingState.PLAY_LOOP))
            } else {
                lastWarning = "No animation was found"
                return false
            }
        } else if (animations.isEmpty()) {
            lastWarning = "No animation is set"
            return false
        }

        if (location <= 0) {
            lastWarning = "Shader '${shader.name}' is missing location"
            return false
        }

        if (useAnimTextures) {

            updateAnimState()

            shader.v4f("prevAnimWeights", prevWeights)
            shader.v4f("prevAnimIndices", prevIndices)

            shader.v4f("animWeights", currWeights)
            shader.v4f("animIndices", currIndices)

            val animTexture = AnimationCache[skeleton]
            val animTexture2 = animTexture.texture
            if (animTexture2 == null) {
                if (lastWarning == null) lastWarning = "AnimTexture is invalid"
                return false
            }

            lastWarning = null
            animTexture2.bindTrulyNearest(shader, "animTexture")
            return true
        } else {
            // what if the weight is less than 1? change to T-pose? no, the programmer can define that himself with an animation
            // val weightNormalization = 1f / max(1e-7f, animationWeights.values.sum())
            val matrices = getMatrices() ?: return false
            // upload the matrices
            upload(shader, location, matrices)
            return true
        }
    }

    /**
     * returns whether it is animated
     * */
    fun updateAnimState(): Boolean {
        val time = Time.gameTimeN
        return if (time != prevTime) {
            prevTime = time
            prevWeights.set(currWeights)
            prevIndices.set(currIndices)
            getAnimState(currWeights, currIndices)
        } else true // mmh...
    }

    /**
     * gets the animation matrices; thread-unsafe, can only be executed on gfx thread
     * */
    fun getMatrices(): Array<Matrix4x3f>? {
        var matrices: Array<Matrix4x3f>? = null
        var sumWeight = 0f
        val animations = animations
        val skeleton = skeleton
        for (index in animations.indices) {
            val animSource = animations[index]
            val weight = animSource.weight
            val relativeWeight = weight / (sumWeight + weight)
            val animation = AnimationCache[animSource.source] ?: continue
            val frameIndex = (animSource.progress * animation.numFrames) / animation.duration
            if (matrices == null) {
                matrices = animation.getMappedMatricesSafely(frameIndex, tmpMapping0, skeleton)
            } else if (relativeWeight > 0f) {
                val matrix = animation.getMappedMatricesSafely(frameIndex, tmpMapping1, skeleton)
                for (j in matrices.indices) {
                    matrices[j].lerp(matrix[j], relativeWeight)
                }
            }
            sumWeight += max(0f, weight)
        }
        return matrices
    }


    /**
     * gets the animation matrices; thread-unsafe, can only be executed on gfx thread
     * */
    fun getMatrix(boneId: Int): Matrix4x3f? {
        var matrices: Matrix4x3f? = null
        var sumWeight = 0f
        val animations = animations
        val skeleton = skeleton
        for (index in animations.indices) {
            val animSource = animations[index]
            val weight = animSource.weight
            val relativeWeight = weight / (sumWeight + weight)
            val animation = AnimationCache[animSource.source] ?: continue
            val frameIndex = (animSource.progress * animation.numFrames) / animation.duration
            if (matrices == null) {
                matrices = animation.getMappedMatrixSafely(frameIndex, boneId, tmpMapping0, skeleton)
            } else if (relativeWeight > 0f) {
                val matrix = animation.getMappedMatrixSafely(frameIndex, boneId, tmpMapping1, skeleton)
                matrices.lerp(matrix, relativeWeight)
            }
            sumWeight += max(0f, weight)
        }
        return matrices
    }

    open fun getAnimTexture(): Texture2D? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        return AnimationCache[skeleton].texture
    }

    open fun getAnimState(
        dstWeights: Vector4f,
        dstIndices: Vector4f
    ): Boolean {

        val skeleton = SkeletonCache[skeleton]
        if (skeleton == null) {
            lastWarning = "Skeleton missing"
            return false
        }

        if (useDefaultAnimation && animations.isEmpty() && skeleton.animations.isNotEmpty()) {
            val sample = skeleton.animations.entries.firstOrNull()?.value
            if (sample != null) {
                addState(AnimationState(sample, 0f, 0f, 0f, LoopingState.PLAY_LOOP))
            } else {
                lastWarning = "No animation was found"
                return false
            }
        } else if (animations.isEmpty()) {
            lastWarning = "No animation is set"
            return false
        }


        // what if the weight is less than 1? change to T-pose? no, the programmer can define that himself with an animation
        // val weightNormalization = 1f / max(1e-7f, animationWeights.values.sum())
        val animations = animations

        dstWeights.set(1f, 0f, 0f, 0f)
        dstIndices.set(0f)

        // find major weights & indices in anim texture
        val animTexture = AnimationCache[skeleton]
        var writeIndex = 0
        for (index in animations.indices) {
            val animState = animations[index]
            val weight = animState.weight
            if (abs(weight) > abs(dstWeights[dstWeights.minComponent()])) {
                val animation = AnimationCache[animState.source] ?: continue
                val frameIndex = animState.progress / animation.duration * animation.numFrames
                val internalIndex = animTexture.getIndex(animation, frameIndex)
                if (writeIndex < 4) {
                    dstIndices.setComponent(writeIndex, internalIndex)
                    dstWeights.setComponent(writeIndex, weight)
                    writeIndex++
                } else {
                    val nextIndex = dstWeights.minComponent()
                    dstIndices.setComponent(nextIndex, internalIndex)
                    dstWeights.setComponent(nextIndex, weight)
                }
            }
        }

        return true
    }

    override fun raycastClosestHit(query: RayQuery): Boolean {
        val mesh = getMeshOrNull() ?: return false
        if (!mesh.hasBones) return super.raycastClosestHit(query)
        updateAnimState()
        val matrices = getMatrices()
            ?: return super.raycastClosestHit(query)
        val original = query.result.distance
        RaycastSkeletal.raycastGlobalBoneMeshClosestHit(
            query, transform?.globalTransform,
            mesh, matrices
        )
        return if (RaycastMesh.isCloser(query, original)) {
            query.result.mesh = mesh
            true
        } else false
    }

    override fun raycastAnyHit(query: RayQuery): Boolean {
        val mesh = getMeshOrNull() ?: return false
        if (!mesh.hasBones) return super.raycastClosestHit(query)
        updateAnimState()
        val matrices = getMatrices()
            ?: return super.raycastClosestHit(query)
        val original = query.result.distance
        RaycastSkeletal.raycastGlobalBoneMeshAnyHit(
            query, transform?.globalTransform,
            mesh, matrices
        )
        return if (RaycastMesh.isCloser(query, original)) {
            query.result.mesh = mesh
            true
        } else false
    }

    override fun onDrawGUI(all: Boolean) {
        if (all) {
            // draw animated skeleton as debug mesh
            val skeleton = SkeletonCache[skeleton] ?: return
            val matrices = getMatrices() ?: return
            Thumbs.buildAnimatedSkeleton(skeleton, matrices) { mesh ->
                useFrame(simpleNormalRenderer) {
                    Gizmos.drawMesh(
                        RenderState.cameraMatrix,
                        transform?.getDrawMatrix(),
                        Material.defaultMaterial, -1,
                        mesh
                    )
                }
            }
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AnimMeshComponent
        dst.skeleton = skeleton
        dst.animations = animations.map { it.clone() }
        dst.useDefaultAnimation = useDefaultAnimation
        dst.prevIndices.set(prevIndices)
        dst.prevTime = prevTime
        dst.prevWeights.set(prevWeights)
        dst.currIndices.set(currIndices)
        dst.currWeights.set(currWeights)
    }

    override val className: String get() = "AnimMeshComponent"

    companion object {

        private val tmpMapping0 = Array(256) { Matrix4x3f() }
        private val tmpMapping1 = Array(256) { Matrix4x3f() }

        fun upload(shader: Shader, location: Int, matrices: Array<Matrix4x3f>) {
            val boneCount = min(matrices.size, BoneData.maxBones)
            val buffer = BoneData.matrixBuffer
            buffer.limit(BoneData.matrixSize * boneCount)
            for (index in 0 until boneCount) {
                val matrix0 = matrices[index]
                buffer.position(index * BoneData.matrixSize)
                BoneData.get(matrix0, buffer)
            }
            buffer.position(0)
            shader.m4x3Array(location, buffer)
        }
    }
}