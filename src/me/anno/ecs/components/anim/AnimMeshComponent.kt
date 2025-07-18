package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.raycast.RaycastSkeletal
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.engine.ui.TextShapes.drawTextMesh
import me.anno.engine.ui.render.MovingGrid
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.Renderers.simpleRenderer
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.ITexture2D
import me.anno.image.thumbs.AssetThumbnails
import me.anno.io.files.InvalidRef
import me.anno.ui.editor.sceneView.Gizmos
import me.anno.utils.Color.black
import me.anno.utils.GFXFeatures
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a skeletal animation / applies animation to a mesh with bones
 * */
open class AnimMeshComponent : MeshComponent(), OnUpdate, OnDrawGUI {

    // maybe not the most efficient way, but it should work :)
    @Docs("Maps time & bone index onto local transform")
    @Type("List<AnimationState>")
    @SerializedProperty
    var animations: List<AnimationState> = emptyList()

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

    init {
        // todo automatically figure out when we can't use instanced rendering because of attributes limit
        isInstanced = false
    }

    open fun onAnimFinished(anim: AnimationState) {
        val instance = AnimationCache.getEntry(anim.source).value
        if (instance != null) {
            val duration = instance.duration
            anim.progress = anim.repeat[anim.progress, duration]
        }
    }

    override fun onUpdate() {
        // update all weights
        val dt = Time.deltaTime.toFloat()
        for (index in animations.indices) {
            val anim = animations[index]
            anim.update(this, dt)
        }
        updateAnimState()
    }

    override fun hasAnimation(mesh: IMesh): Boolean {
        val skeleton = SkeletonCache.getEntry(mesh.skeleton).value
        return skeleton != null && animations.isNotEmpty()
    }

    override fun defineVertexTransform(shader: GPUShader, transform: Transform, mesh: IMesh): Boolean {

        val skeleton = SkeletonCache.getEntry(mesh.skeleton).waitFor()
        if (skeleton == null) {
            lastWarning = "Skeleton missing"
            return false
        }

        shader.use()

        // check whether the shader actually uses bones
        val location = shader[if (useAnimTextures) "animWeights" else "jointTransforms"]

        if (animations.isEmpty()) {
            lastWarning = "No animation is set"
            return false
        }

        if (location <= 0) {
            lastWarning = "Shader '${shader.name}' is missing location"
            return false
        }

        if (useAnimTextures && skeleton.bones.isNotEmpty()) {

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
        val mesh = getMeshOrNull()
        return if (time != prevTime && mesh != null) {
            prevTime = time
            prevWeights.set(currWeights)
            prevIndices.set(currIndices)
            getAnimState(currWeights, currIndices, mesh)
        } else true // mmh...
    }

    /**
     * gets the animation matrices; thread-unsafe, can only be executed on gfx thread
     * */
    fun getMatrices(): List<Matrix4x3f>? {
        var matrices: List<Matrix4x3f>? = null
        var sumWeight = 0f
        val animations = animations
        val skeleton = getMesh()?.skeleton ?: InvalidRef
        for (index in animations.indices) {
            val animSource = animations[index]
            val weight = animSource.weight
            val relativeWeight = weight / (sumWeight + weight)
            val animation = AnimationCache.getEntry(animSource.source).waitFor() ?: continue
            val frameIndex = (animSource.progress * animation.numFrames) / animation.duration
            if (matrices == null) {
                matrices = animation.getMappedMatricesSafely(frameIndex, tmpMapping0, skeleton)
            } else if (relativeWeight > 0f) {
                val matrix = animation.getMappedMatricesSafely(frameIndex, tmpMapping1, skeleton)
                for (j in matrices.indices) {
                    matrices[j].mix(matrix[j], relativeWeight)
                }
            }
            sumWeight += max(0f, weight)
        }
        return matrices
    }

    /**
     * gets the animation matrices; thread-unsafe, can only be executed on gfx thread
     * */
    fun getMatrix(boneIndex: Int): Matrix4x3f? {
        var matrices: Matrix4x3f? = null
        var sumWeight = 0f
        val animations = animations
        val skeleton = getMesh()?.skeleton ?: InvalidRef
        for (index in animations.indices) {
            val animSource = animations[index]
            val weight = animSource.weight
            val relativeWeight = weight / (sumWeight + weight)
            val animation = AnimationCache.getEntry(animSource.source).waitFor() ?: continue
            val frameIndex = (animSource.progress * animation.numFrames) / animation.duration
            if (matrices == null) {
                matrices = animation.getMappedMatrixSafely(frameIndex, boneIndex, tmpMapping0, skeleton)
            } else if (relativeWeight > 0f) {
                val matrix = animation.getMappedMatrixSafely(frameIndex, boneIndex, tmpMapping1, skeleton)
                matrices.mix(matrix, relativeWeight)
            }
            sumWeight += max(0f, weight)
        }
        return matrices
    }

    open fun getAnimTexture(mesh: IMesh): ITexture2D? {
        val skeleton = SkeletonCache.getEntry(mesh.skeleton).waitFor() ?: return null
        if (skeleton.bones.isEmpty()) return null
        return AnimationCache[skeleton].texture
    }

    open fun getAnimState(
        dstWeights: Vector4f,
        dstIndices: Vector4f,
        mesh: IMesh
    ): Boolean {

        val skeleton = SkeletonCache.getEntry(mesh.skeleton).waitFor()
        if (skeleton == null) {
            lastWarning = "Mesh-Skeleton missing"
            return false
        } else if (skeleton.bones.isEmpty()) {
            lastWarning = "Skeleton bones missing"
            return false
        }

        if (animations.isEmpty()) {
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
            if (abs(weight) > abs(dstWeights.min())) {
                val animation = AnimationCache.getEntry(animState.source).waitFor() ?: continue
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

    override fun raycast(query: RayQuery): Boolean {
        val mesh = getMeshOrNull() as? Mesh ?: return false
        if (!mesh.hasBones) return super.raycast(query)
        updateAnimState()
        val matrices = getMatrices()
            ?: return super.raycast(query)
        val original = query.result.distance
        RaycastSkeletal.raycastGlobalBoneMesh(
            query, transform?.globalTransform,
            mesh, matrices
        )
        return if (RaycastMesh.isCloser(query, original)) {
            query.result.mesh = mesh
            true
        } else false
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (all) {
            // draw animated skeleton as debug mesh
            val skeleton = SkeletonCache.getEntry(getMesh()?.skeleton ?: InvalidRef).waitFor() ?: return
            drawAnimatedSkeleton(pipeline, this, skeleton, transform?.getDrawMatrix(), true)
        }
    }

    @DebugAction
    fun openRetargetingUI() {
        Retargetings.openUI(this)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is AnimMeshComponent) return
        dst.animations = animations.map { it.clone() }
        dst.prevIndices.set(prevIndices)
        dst.prevTime = prevTime
        dst.prevWeights.set(prevWeights)
        dst.currIndices.set(currIndices)
        dst.currWeights.set(currWeights)
    }

    companion object {

        val tmpMapping0 = createArrayList(256) { Matrix4x3f() }
        val tmpMapping1 = createArrayList(256) { Matrix4x3f() }

        fun drawAnimatedSkeleton(
            pipeline: Pipeline,
            animMeshComponent: AnimMeshComponent,
            skeleton: Skeleton,
            transform: Matrix4x3?,
            withNames: Boolean,
        ) {
            val matrices = animMeshComponent.getMatrices() ?: return
            if (withNames) {
                // draw bone names where they are
                val pos1 = Vector3d()
                val pos2 = Vector3d()
                MovingGrid.alpha = 1f
                val bones = skeleton.bones
                val scale = bones.sumOf { it.length(bones).toDouble() } / bones.size
                for (i in 0 until min(bones.size, matrices.size)) {
                    val bone = bones[i]
                    pos1.set(bone.bindPosition)

                    matrices[i].transformPosition(pos1)
                    pos1.add(scale, 0.0, 0.0, pos2)

                    drawLine(transform, pos1, pos2, -1)
                    pos2.x += 0.025 * scale
                    pos2.y -= 0.030 * scale
                    drawTextMesh(pipeline, bone.name, pos2, null, 0.1 * scale, transform)
                }
            }

            // draw animated skeleton as debug mesh
            AssetThumbnails.buildAnimatedSkeleton(skeleton, matrices) { mesh ->
                useFrame(simpleRenderer) {
                    Gizmos.drawMesh(
                        pipeline, RenderState.cameraMatrix, transform,
                        Material.defaultMaterial, black or 0xff9999, mesh
                    )
                }
            }
        }

        fun upload(shader: GPUShader, location: Int, matrices: List<Matrix4x3f>) {
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