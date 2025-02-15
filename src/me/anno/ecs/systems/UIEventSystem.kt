package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.ui.UIEvent
import me.anno.ecs.interfaces.InputListener
import me.anno.utils.structures.Collections.setContains
import java.util.TreeSet

object UIEventSystem : System() {

    @DebugProperty
    val numRegisteredInstances: Int
        get() = components.size

    private val components = TreeSet<InputListener>()
    override fun setContains(component: Component, contains: Boolean) {
        if (component is InputListener) {
            components.setContains(component, contains)
        }
    }

    fun onUIEvent(event: UIEvent): Boolean {
        return components.any { component ->
            event.call(component)
        }
    }

    override fun clear() {
        components.clear()
    }
}