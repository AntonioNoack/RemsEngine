package me.anno.ecs

import kotlin.reflect.KClass

/**
 * utility functions for finding sibling-, ancestor- or children components
 * */
@Suppress("unused")
object EntityQuery {

    fun <V1 : Any> Entity.hasComponent(clazz: KClass<V1>, includingDisabled: Boolean = false): Boolean {
        return getComponent(clazz, includingDisabled) != null
    }

    fun <V1 : Any> Component.hasComponent(clazz: KClass<V1>, includingDisabled: Boolean = false): Boolean {
        return entity?.hasComponent(clazz, includingDisabled) == true
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

    fun <V : Component> Component.hasComponentInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean = false
    ): Boolean {
        return entity?.hasComponentInChildren(clazz, includingDisabled) == true
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

    fun <V : Any> Component.getComponent(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return entity?.getComponent(clazz, includingDisabled)
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

    fun <V : Any> Component.getComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return entity?.getComponentInChildren(clazz, includingDisabled)
    }

    fun <V : Any> Entity.getComponentInHierarchy(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return getComponent(clazz, includingDisabled) ?: parentEntity?.getComponentInHierarchy(clazz, includingDisabled)
    }

    fun <V : Any> Component.getComponentInHierarchy(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return entity?.getComponentInHierarchy(clazz, includingDisabled)
    }

    fun <V : Any> Entity.getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        @Suppress("unchecked_cast")
        return components.filter { (includingDisabled || it.isEnabled) && clazz.isInstance(it) } as List<V>
    }

    fun <V : Any> Component.getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return entity?.getComponents(clazz, includingDisabled) ?: emptyList()
    }

    fun Entity.allComponents(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if ((includingDisabled || c.isEnabled) && !predicate(c)) {
                return false
            }
        }
        return true
    }

    fun Component.allComponents(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean = entity?.allComponents(includingDisabled, predicate) ?: true

    fun <V : Any> Entity.allComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && !predicate(c as V)) {
                return false
            }
        }
        return true
    }

    fun <V : Any> Component.allComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean = entity?.allComponents(clazz, includingDisabled, predicate) ?: true

    fun Entity.anyComponent(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if ((includingDisabled || c.isEnabled) && predicate(c)) {
                return true
            }
        }
        return false
    }

    fun Component.anyComponent(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean = entity?.anyComponent(includingDisabled, predicate) ?: true

    fun <V : Any> Entity.anyComponent(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && predicate(c as V))
                return true
        }
        return false
    }

    fun <V : Any> Component.anyComponent(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean = entity?.anyComponent(clazz, includingDisabled, predicate) ?: true

    fun <V : Any> Entity.forAllComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) {
        anyComponent(clazz, includingDisabled) {
            callback(it)
            false
        }
    }

    fun <V : Any> Component.forAllComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) = entity?.forAllComponents(clazz, includingDisabled, callback)

    fun <V : Any> Entity.anyComponentInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        if (anyComponent(clazz, includingDisabled, predicate)) {
            return true
        }
        val children = children
        for (index in children.indices) {
            val c = children[index]
            if ((includingDisabled || c.isEnabled) && c.anyComponentInChildren(clazz, includingDisabled, predicate)) {
                return true
            }
        }
        return false
    }

    fun <V : Any> Component.anyComponentInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        return entity?.anyComponentInChildren(clazz, includingDisabled, predicate) ?: false
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

    fun <V : Any> Component.forAllComponentsInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) = entity?.forAllComponentsInChildren(clazz, includingDisabled, callback)

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

    fun <V : Any> Component.sumComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        getCount: (V) -> Long
    ): Long = entity?.sumComponents(clazz, includingDisabled, getCount) ?: 0L

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

    fun <V : Any> Component.sumComponentsInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        getCount: (V) -> Long
    ): Long = entity?.sumComponentsInChildren(clazz, includingDisabled, getCount) ?: 0L

    fun <V : Any> Entity.getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return getComponentsInChildren(clazz, includingDisabled, ArrayList())
    }

    fun <V : Any> Component.getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return entity?.getComponentsInChildren(clazz, includingDisabled, ArrayList()) ?: emptyList()
    }

    fun <V : Any> Entity.getComponentsInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        dst: MutableList<V>
    ): List<V> {
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if ((includingDisabled || component.isEnabled) && clazz.isInstance(component)) {
                @Suppress("unchecked_cast")
                dst.add(component as V)
            }
        }
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (includingDisabled || child.isEnabled) {
                child.getComponentsInChildren(clazz, includingDisabled, dst)
            }
        }
        return dst
    }
}