package me.anno.recast

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.systems.OnUpdate

class CrowdUpdateComponent(val data: NavMeshData) : Component(), OnUpdate {

    @DebugProperty
    val numActiveAgents get() = data.crowd.activeAgents.size

    override fun priority(): Int = -1000

    override fun onUpdate() {
        data.crowd.update(Time.deltaTime.toFloat(), null)
    }
}