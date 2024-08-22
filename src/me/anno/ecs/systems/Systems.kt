package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.utils.structures.Recursion
import me.anno.utils.structures.lists.Lists.sortedAdd
import me.anno.utils.structures.lists.Lists.wrap

// this would lie on the root level only...,
// and only if needed...
// todo make physics into systems
// todo show settings for physics somehow...
// todo when something is enabled/disabled/added/removed, notify the system
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

    init {
        isCollapsed = false
    }

    @NotSerializedProperty
    var world: PrefabSaveable? = null
        set(value) {
            if (field !== value) {
                for (i in systems.indices) {
                    systems[i].clear()
                }
                if (value != null) {
                    addOrRemoveRecursively(value, true)
                }
                field = value
            }
        }

    override fun listChildTypes(): String = "sw"
    override fun getChildListByType(type: Char): List<PrefabSaveable> {
        return if (type == 's') systems else world.wrap()
    }

    fun onUpdate() {
        forAllSystems { it.onUpdate() }
    }

    fun forAllSystems(callback: (System) -> Unit) {
        for (i in systems.indices) {
            callback(systems[i])
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

    fun addOrRemoveRecursively(root: PrefabSaveable, add: Boolean) {
        Recursion.processRecursive(root) { element, remaining ->
            if (element.isEnabled) {
                for (type in element.listChildTypes()) {
                    remaining.addAll(element.getChildListByType(type))
                }
                when (element) {
                    is Entity -> forAllSystems { addOrRemove(it, element, add) }
                    is Component -> forAllSystems { addOrRemove(it, element, add) }
                }
            }
        }
    }
}