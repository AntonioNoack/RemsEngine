package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.prefab.PrefabSaveable

// this would lie on the root level only...,
// and only if needed...
class Systems {

    val systems = HashSet<System>()
    var world: PrefabSaveable? = null
        set(value) {
            if (field !== value) {
                for (system in systems) system.clear()
                addOrRemoveRecursively(value, true)
                field = value
            }
        }

    fun onUpdate() {
        for (system in systems) {
            system.onUpdate()
        }
    }

    private fun addOrRemove(system: System, element: Component, add: Boolean) {
        if (add) system.onEnable(element)
        else system.onDisable(element)
    }

    private fun addOrRemove(system: System, element: Entity, add: Boolean) {
        if (add) system.onEnable(element)
        else system.onDisable(element)
    }

    fun addOrRemoveRecursively(element: PrefabSaveable?, add: Boolean) {
        val systems = systems.toList()
        when (element) {
            is Component -> {
                for (i in systems.indices) {
                    val system = systems[i]
                    addOrRemove(system, element, add)
                }
            }
            is Entity -> {
                val stack = ArrayList<Entity>()
                stack.add(element)
                while (stack.isNotEmpty()) {
                    val elementI = stack.removeLast()
                    if (!elementI.isEnabled) continue
                    stack.addAll(elementI.children)
                    val components = elementI.components
                    for (i in systems.indices) {
                        val system = systems[i]
                        addOrRemove(system, element, add)
                        for (j in components.indices) {
                            addOrRemove(system, components[j], add)
                        }
                    }
                }
            }
        }
    }
}