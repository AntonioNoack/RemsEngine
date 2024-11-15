package me.anno.games.simslike

import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.systems.OnUpdate

class Sim : Component(), OnUpdate {
    val actions = ArrayList<SimAction>()
    var male = false
    var age = 18

    override fun onUpdate() {
        updateTransform()
        updateAnimationState()
    }

    fun updateTransform() {
        val transform = transform ?: return
        // todo update position, rotation
    }

    fun updateAnimationState() {
        val anim = getComponent(AnimMeshComponent::class) ?: return
        // todo update animation state/blending
    }
}