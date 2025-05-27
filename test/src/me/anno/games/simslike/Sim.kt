package me.anno.games.simslike

import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.systems.OnUpdate
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.posMod
import org.joml.Matrix4x3f
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max

class Sim : Component(), OnUpdate {

    val actions = ArrayList<SimAction>()
    var male = false
    var age = 18

    override fun priority(): Int = 110 // >100 = shall run after AnimComponent

    override fun onUpdate() {
        updateAnimationState()
        updateTransform()
    }

    fun updateTransform() {
        val transform = transform ?: return
        val nav0 = getComponent(SimNavAgent::class) ?: return
        val nav1 = nav0.crowdAgent ?: return
        val pos = nav1.currentPosition
        transform.setLocalPosition(
            pos.x - offset.x,
            pos.y - offset.y,
            pos.z - offset.z
        )
        invalidateAABB()
        // todo update rotation
    }

    // todo remove root offset from animation
    //   offset: mix(offset0, offset1, relativeAnimationTime)

    // todo or alternatively, we could use these animations properly, and teleport on each restart...

    private val offset = Vector3f()

    fun updateAnimationState() {
        val anim = getComponent(AnimMeshComponent::class) ?: return
        val nav = getComponent(SimNavAgent::class) ?: return
        val nav1 = nav.crowdAgent ?: return
        val speed = clamp(nav1.actualVelocity.lengthXZ(), 0f, 2f)
        // update animation state/blending
        anim.animations[0].weight = weight(speed)
        anim.animations[1].weight = weight(speed - 1f)
        anim.animations[2].weight = weight(speed - 2f)
        calculateOffset(anim)
    }

    private fun calculateOffset(anim: AnimMeshComponent): Vector3f {
        offset.set(0f)
        for (state in anim.animations) {
            val animation = AnimationCache[state.source] ?: continue
            val t = posMod(state.progress / animation.duration, 1f)
            val offsetI = calculateOffset(animation, t)
            offsetI.mulAdd(state.weight, offset, offset)
        }
        return offset
    }

    private fun calculateOffset(anim: Animation, t: Float): Vector3f {
        val offset0 = Vector3f()
        val offset1 = Vector3f()
        val tmpMatrix = Matrix4x3f()
        anim.getMatrix(0, 0, listOf(tmpMatrix))?.getTranslation(offset0)
        anim.getMatrix(anim.numFrames - 1, 0, listOf(tmpMatrix))?.getTranslation(offset1)
        return offset0.mix(offset1, t)
    }

    companion object {
        fun weight(v: Float): Float {
            return max(1f - abs(v), 0f)
        }
    }
}