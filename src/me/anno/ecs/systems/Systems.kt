package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.utils.structures.lists.Lists.sortedAdd
import me.anno.utils.structures.lists.Lists.wrap

// this would lie on the root level only...,
// and only if needed...
// todo make physics into systems
class Systems : PrefabSaveable() {

    companion object {
        private val registeredIDs = HashSet<String>()
        private val systems = ArrayList<System>()

        // do we need unregistering?
        fun registerSystem(id: String, instance: System) {
            synchronized(registeredIDs) {
                if (registeredIDs.add(id)) {
                    systems.sortedAdd(instance, Comparator.comparingInt(System::priority), true)
                }
            }
        }

        init {
            registerSystem("Update", UpdateSystem())
        }
    }

    var world: PrefabSaveable? = null
        set(value) {
            if (field !== value) {
                for (i in systems.indices) {
                    systems[i].clear()
                }
                addOrRemoveRecursively(value, true)
                field = value
            }
        }

    override fun listChildTypes(): String = "sw"
    override fun getChildListByType(type: Char): List<PrefabSaveable> {
        return if (type == 's') systems else world.wrap()
    }

    fun onUpdate() {
        for (i in systems.indices) {
            systems[i].onUpdate()
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
        val systems = systems
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