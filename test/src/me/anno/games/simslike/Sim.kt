package me.anno.games.simslike

import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.maths.Maths.clamp
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
        transform.setLocalPosition(nav1.currentPosition)
        invalidateAABB()
        // todo update rotation
    }

    fun updateAnimationState() {
        val anim = getComponent(AnimMeshComponent::class) ?: return
        val nav = getComponent(SimNavAgent::class) ?: return
        val nav1 = nav.crowdAgent ?: return
        val speed = clamp(nav1.actualVelocity.lengthXZ(), 0f, 2f)
        // update animation state/blending
        anim.animations[0].weight = weight(speed)
        anim.animations[1].weight = weight(speed - 1f)
        anim.animations[2].weight = weight(speed - 2f)
    }

    companion object {
        fun weight(v: Float): Float {
            return max(1f - abs(v), 0f)
        }
    }
}