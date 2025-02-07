package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.annotations.DebugProperty
import me.anno.utils.structures.Collections.setContains
import me.anno.utils.structures.sets.FastIteratorSet
import kotlin.reflect.KClass

abstract class UpdateByClassSystem(val isOnUpdate: Boolean) : System() {

    @DebugProperty
    val numRegisteredClasses: Int
        get() = components.count { it.value.isNotEmpty() }

    @DebugProperty
    var numRegisteredInstances: Int = 0
        private set

    private val lock: Any get() = this

    private val components = HashMap<KClass<*>, FastIteratorSet<Component>>() // what is registered
    private val changeSet = HashSet<Component>() // what changes from frame to frame
    private val sortedComponents = ArrayList<Map.Entry<KClass<*>, FastIteratorSet<Component>>>() // sorted entries

    override fun setContains(component: Component, contains: Boolean) {
        if (isInstance(component)) {
            synchronized(lock) {
                val existing = components[component::class]?.contains(component) == true
                // else don't change that instance
                val currentStateNeedsChange = existing != contains
                if (changeSet.setContains(component, currentStateNeedsChange)) {
                    numRegisteredInstances += if (contains) 1 else -1
                }
            }
        }
    }

    override fun clear() {
        components.clear()
        changeSet.clear()
        sortedComponents.clear()
        numRegisteredInstances = 0
    }

    abstract fun isInstance(component: Component): Boolean
    abstract fun update(sample: Component, instances: List<Component>)
    abstract fun getPriority(sample: Component): Int

    override fun onUpdate() {
        if (isOnUpdate) execute()
    }

    override fun onBeforeDrawing() {
        if (!isOnUpdate) execute()
    }

    private fun updateInstances(): Boolean {
        synchronized(lock) {
            val hasEntries = changeSet.isNotEmpty()
            for (component in changeSet) {
                components
                    .getOrPut(component::class, ::FastIteratorSet)
                    .toggleContains(component)
            }
            changeSet.clear()
            return hasEntries
        }
    }

    private fun sortComponentsByPriority() {
        sortedComponents.clear()
        sortedComponents.addAll(components.entries)
        sortedComponents.removeIf { it.value.isEmpty() }
        sortedComponents.sortBy { getPriority(it.value.first()) }
    }

    private fun execute() {
        // "components" is only changed during this, so we're free to iterate over it after that
        val changed = updateInstances()
        if (changed) sortComponentsByPriority()
        for ((_, instances) in sortedComponents) {
            val sample = instances.firstOrNull() ?: continue
            update(sample, instances.asList())
        }
    }
}