package me.anno.ecs

import kotlin.reflect.KClass

object EntityQuery {

    fun <V1 : Any> Entity.hasComponent(clazz: KClass<V1>, includingDisabled: Boolean = false): Boolean {
        return getComponent(clazz, includingDisabled) != null
    }

    fun <V : Component> Entity.hasComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
        if (hasComponent(clazz, includingDisabled)) return true
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (includingDisabled || child.isEnabled) {
                if (child.hasComponent(clazz, includingDisabled)) {
                    return true
                }
            }
        }
        return false
    }

    fun <V : Any> Entity.getComponent(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        // elegant:
        // return components.firstOrNull { clazz.isInstance(it) && (includingDisabled || it.isEnabled) } as V?
        // without damn iterator:
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if ((includingDisabled || component.isEnabled) && clazz.isInstance(component)) {
                @Suppress("unchecked_cast")
                return component as V
            }
        }
        return null
    }

    fun <V : Any> Entity.getComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        var comp = getComponent(clazz, includingDisabled)
        if (comp != null) return comp
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (includingDisabled || child.isEnabled) {
                comp = child.getComponentInChildren(clazz, includingDisabled)
                if (comp != null) return comp
            }
        }
        return null
    }

    fun <V : Any> Entity.getComponentInHierarchy(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return getComponent(clazz, includingDisabled) ?: parentEntity?.getComponentInHierarchy(clazz, includingDisabled)
    }

    fun <V : Any> Entity.getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        @Suppress("unchecked_cast")
        return components.filter { (includingDisabled || it.isEnabled) && clazz.isInstance(it) } as List<V>
    }

    fun <V : Any> Entity.allComponents(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        lambda: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && !lambda(c as V))
                return false
        }
        return true
    }

    fun Entity.anyComponent(
        includingDisabled: Boolean = false,
        test: (Component) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if ((includingDisabled || c.isEnabled) && test(c))
                return true
        }
        return false
    }

    fun <V : Any> Entity.anyComponent(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        test: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && test(c as V))
                return true
        }
        return false
    }

    fun <V : Any> Entity.forAllComponents(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) {
        anyComponent(clazz, includingDisabled) {
            callback(it)
            false
        }
    }

    fun <V : Any> Entity.anyComponentInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        test: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && test(c as V))
                return true
        }
        val children = children
        for (index in children.indices) {
            val c = children[index]
            if ((includingDisabled || c.isEnabled) && c.anyComponentInChildren(clazz, includingDisabled, test)) {
                return true
            }
        }
        return false
    }

    fun <V : Any> Entity.forAllComponentsInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) {
        anyComponentInChildren(clazz, includingDisabled) {
            callback(it)
            false
        }
    }

    fun <V : Any> Entity.sumComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        getCount: (V) -> Long
    ): Long {
        var sum = 0L
        forAllComponents(clazz, includingDisabled) { comp ->
            sum += getCount(comp)
        }
        return sum
    }

    fun <V : Any> Entity.sumComponentsInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        getCount: (V) -> Long
    ): Long {
        var sum = 0L
        forAllComponentsInChildren(clazz, includingDisabled) { comp ->
            sum += getCount(comp)
        }
        return sum
    }

    fun <V : Any> Entity.getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return getComponentsInChildren(clazz, includingDisabled, ArrayList())
    }

    fun <V : Any> Entity.getComponentsInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean,
        dst: MutableList<V>
    ): List<V> {
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if (clazz.isInstance(component)) {
                @Suppress("unchecked_cast")
                dst.add(component as V)
            }
        }
        val children = children
        for (i in children.indices) {
            val child = children[i]
            child.getComponentsInChildren(clazz, includingDisabled, dst)
        }
        return dst
    }

    fun <V : Any> Entity.firstComponentInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        filter: (V) -> Boolean
    ): V? {
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if (clazz.isInstance(component)) {
                @Suppress("unchecked_cast")
                component as V
                if (filter(component)) return component
            }
        }
        val children = children
        for (i in children.indices) {
            val child = children[i]
            val v = child.firstComponentInChildren(clazz, includingDisabled, filter)
            if (v != null) return v
        }
        return null
    }
}