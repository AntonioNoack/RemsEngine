package me.anno.games.simslike

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.hasComponent
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3f

open class SimAction : Component() {

    val isSimTarget get() = hasComponent(Sim::class)
    val isObjectTarget get() = hasComponent(SimObject::class)

    var offset: Vector3f = Vector3f()
        set(value) {
            field.set(value)
        }

    var angle = 0f

    var startTime = 0L
    var state = ActionState.READY
    var supportRunning = false

    /**
     * execution time in seconds
     * */
    val executionTime get() = (Time.gameTimeN - startTime) * 1e-9

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SimAction) return
        dst.offset = offset
        dst.angle = angle
    }

    /**
     * returns true on completion
     * */
    fun execute(sim: Sim): Boolean {
        return true
    }
}