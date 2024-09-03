package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.annotations.DebugProperty
import me.anno.utils.structures.Collections.setContains
import me.anno.utils.structures.Collections.toggleContains
import kotlin.reflect.KClass

abstract class UpdateByClassSystem(val isOnUpdate: Boolean) : System() {

    @DebugProperty
    val numRegisteredClasses: Int
        get() = components.count { it.value.isNotEmpty() }

    @DebugProperty
    var numRegisteredInstances: Int = 0
        private set

    private val lock: Any get() = this

    private val components = HashMap<KClass<*>, HashSet<Component>>()
    private val changeList = HashSet<Component>()

    override fun setContains(component: Component, contains: Boolean) {
        if (isInstance(component)) {
            synchronized(lock) {
                val existing = components[component::class]?.contains(component) == true
                // else don't change that instance
                val currentStateNeedsChange = existing != contains
                if (changeList.setContains(component, currentStateNeedsChange)) {
                    numRegisteredInstances += if (contains) 1 else -1
                }
            }
        }
    }

    override fun clear() {
        components.clear()
        changeList.clear()
        numRegisteredInstances = 0
    }

    abstract fun isInstance(component: Component): Boolean
    abstract fun update(sample: Component, instances: Collection<Component>)

    override fun onUpdate() {
        if (isOnUpdate) execute()
    }

    override fun onBeforeDrawing() {
        if (!isOnUpdate) execute()
    }

    private fun updateInstances() {
        synchronized(lock) {
            for (component in changeList) {
                components
                    .getOrPut(component::class, ::HashSet)
                    .toggleContains(component)
            }
            changeList.clear()
        }
    }

    private fun execute() {
        updateInstances() // "components" is only changed during this, so we're free to iterate over it after that
        for ((_, instances) in components) {
            val sample = instances.firstOrNull() ?: continue
            update(sample, instances)
        }
    }
}