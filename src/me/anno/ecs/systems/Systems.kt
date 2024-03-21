package me.anno.ecs.systems

import me.anno.ecs.System

// this would lie on the root level only...,
// and only if needed...
class Systems : System() {

    val systems = HashSet<System>()

    override fun onUpdate() {
        for (s in systems) {
            s.onUpdate()
        }
    }

    override fun onEnable(childSystem: System) {
        for (s in systems) {
            s.onEnable(childSystem)
        }
    }

    override fun onDisable(childSystem: System) {
        for (s in systems) {
            s.onEnable(childSystem)
        }
    }
}