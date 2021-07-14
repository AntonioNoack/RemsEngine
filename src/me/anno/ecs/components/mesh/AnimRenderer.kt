package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Retargeting
import me.anno.ecs.components.anim.Skeleton
import me.anno.gpu.shader.Shader
import org.joml.Matrix4x3f
import kotlin.math.max

class AnimRenderer : RendererComponent() {

    @Type("Skeleton")
    var skeleton: Skeleton? = null

    // maybe not the most efficient way, but it should work :)
    var animationWeights = ArrayList<Pair<Animation, Float>>()

    override fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {

        // the programmer must be careful.. or does he? idk...
        animationWeights.removeIf { it.second <= 0f }

        if (animationWeights.isEmpty()) {
            shader.v1("hasAnimation", 0f)
            return
        }

        val time = 0f
        // todo find retargeting from the skeleton to the new skeleton...
        // todo if not found, generate it automatically, and try our best to do it perfectly
        // todo retargeting probably needs to include a max/min-angle and angle multiplier and change of base matrices
        // (or all animations need to be defined in some common animation space)
        val retargeting = Retargeting()

        // what if the weight is less than 1? change to T-pose? no, the programmer can define that himself with an animation
        val weightNormalization = 1f / max(1e-7f, animationWeights.map { it.second }.sum())
        val matrices = animationWeights[0].first.getMappedMatricesSafely(entity, time, dst0, retargeting)
        var sumWeight = animationWeights[0].second
        for (i in 1 until animationWeights.size) {
            val weightAnim = animationWeights[i]
            val weight = weightAnim.second
            val relativeWeight = weight / (sumWeight + weight)
            // todo the second animation may have a different time value -> we need to manage that...
            val secondMatrices = weightAnim.first.getMappedMatricesSafely(entity, time, dst1, retargeting)
            for (j in matrices.indices) {
                matrices[j].lerp(secondMatrices[j], relativeWeight)
            }
            sumWeight += weight
        }

        shader.v1("hasAnimation", 1f)

        // todo upload the matrices


        // get skeleton
        // get animation
        // blend the relevant animations together
    }

    override val className: String = "AnimRenderer"

    companion object {
        val dst0 = Array(256) { Matrix4x3f() }
        val dst1 = Array(256) { Matrix4x3f() }
    }

}