package me.anno.ecs.components.anim.blending

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty

// todo define a multi-dimensional blend-space for animations (idle, walk, run; jump on second dimension):
//  - keys (vectors, 2d?), and values (animations)
//  - auto-define speed by the current animation being played
class AnimBlendSpaceController : Component(), OnUpdate {

    @DebugProperty
    @NotSerializedProperty
    private var renderer: AnimMeshComponent? = null

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        renderer = entity.getComponent(AnimMeshComponent::class)
    }

    override fun onUpdate() {
        val renderer = renderer
        if (renderer == null) { // wait for renderer
            lastWarning = "Renderer missing"
            return
        }
    }
}