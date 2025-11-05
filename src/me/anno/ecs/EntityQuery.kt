package me.anno.ecs

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.saveable.Saveable
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.algorithms.Recursion
import org.joml.AABBd
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * utility functions for finding sibling-, ancestor- or children components
 * */
@Suppress("unused")
object EntityQuery {

    fun <V : Any> Entity.hasComponent(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
        return getComponent(clazz, includingDisabled) != null
    }

    fun <V : Any> Component.hasComponent(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
        return entity?.hasComponent(clazz, includingDisabled) == true
    }

    fun <V : Component> Entity.hasComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
        if (hasComponent(clazz, includingDisabled)) return true
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (checkInstance(includingDisabled, child) && child.hasComponent(clazz, includingDisabled)) {
                return true
            }
        }
        return false
    }

    fun <V : Component> Component.hasComponentInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false
    ): Boolean {
        return entity?.hasComponentInChildren(clazz, includingDisabled) == true
    }

    fun <V : Any> Entity.getComponent(clazzName: String, includingDisabled: Boolean = false): V? {
        val clazz = Saveable.getClass(clazzName)
        @Suppress("UNCHECKED_CAST")
        return getComponent(clazz as KClass<V>, includingDisabled)
    }

    fun <V : Any> Entity.getComponent(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        // elegant:
        // return components.firstOrNull { clazz.isInstance(it) && (includingDisabled || it.isEnabled) } as V?
        // without damn iterator:
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if (checkInstance(includingDisabled, component) && clazz.isInstance(component)) {
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
            if (checkInstance(includingDisabled, child)) {
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
        return components.filter { (includingDisabled || it.isEnabled) }.filterIsInstance2(clazz)
    }

    fun <V : Any> Component.getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return entity?.getComponents(clazz, includingDisabled) ?: emptyList()
    }

    inline fun Entity.allComponents(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if (checkInstance(includingDisabled, c) && !predicate(c)) {
                return false
            }
        }
        return true
    }

    fun checkInstance(includingDisabled: Boolean, instance: PrefabSaveable): Boolean {
        return includingDisabled or instance.isEnabled
    }

    inline fun Entity.forAllComponents(includingDisabled: Boolean, callback: (Component) -> Unit) {
        val components = components
        for (i in components.indices) {
            val comp = components[i]
            if (checkInstance(includingDisabled, comp)) {
                callback(comp)
            }
        }
    }

    inline fun Entity.forAllChildren(includingDisabled: Boolean, callback: (Entity) -> Unit) {
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (checkInstance(includingDisabled, child)) {
                callback(child)
            }
        }
    }

    /**
     * Processes this entity, and all its grand*children recursively.
     * If includingDisabled=false, recursion stops at any disabled entity.
     * If it stops at the root, not a single callback will be executed.
     * */
    fun Entity.forAllEntitiesInChildren(includingDisabled: Boolean, callback: (Entity) -> Unit) {
        Recursion.processRecursive(this) { entity, remaining ->
            if (checkInstance(includingDisabled, entity)) {
                remaining.addAll(entity.children)
                callback(entity)
            }
        }
    }

    inline fun Component.allComponents(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean = entity?.allComponents(includingDisabled, predicate) ?: true

    inline fun <V : Any> Entity.allComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        return !anyComponent(clazz, includingDisabled) { !predicate(it) }
    }

    inline fun <V : Any> Component.allComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean = entity?.allComponents(clazz, includingDisabled, predicate) ?: true

    inline fun Entity.anyComponent(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if (checkInstance(includingDisabled, c) && predicate(c)) {
                return true
            }
        }
        return false
    }

    inline fun Component.anyComponent(
        includingDisabled: Boolean = false,
        predicate: (Component) -> Boolean
    ): Boolean = entity?.anyComponent(includingDisabled, predicate) ?: true

    inline fun <V : Any> Entity.anyComponent(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if (checkInstance(includingDisabled, c)) {
                val cv = clazz.safeCast(c)
                if (cv != null && predicate(cv)) {
                    return true
                }
            }
        }
        return false
    }

    inline fun <V : Any> Component.anyComponent(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean = entity?.anyComponent(clazz, includingDisabled, predicate) ?: true

    inline fun <V : Any> Entity.forAllComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) {
        anyComponent(clazz, includingDisabled) {
            callback(it)
            false
        }
    }

    fun <V : Any> Entity.forAllComponentsInChildrenAndBounds(
        clazz: KClass<V>, bounds: AABBd, includingDisabled: Boolean,
        callback: (V) -> Unit
    ) {
        anyComponentInChildrenAndBounds(clazz, bounds, includingDisabled) {
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
    ) = anyComponentInChildrenAndBounds(clazz, null, includingDisabled, predicate)

    fun <V : Any> Entity.anyComponentInChildrenAndBounds(
        clazz: KClass<V>, bounds: AABBd?, includingDisabled: Boolean = false, predicate: (V) -> Boolean
    ): Boolean {
        return anyComponentInChildrenFiltered(clazz, includingDisabled, predicate) {
            bounds == null || bounds.testAABB(it.getGlobalBounds())
        }
    }

    fun <V : Any> Entity.anyComponentInChildrenFiltered(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean, hierarchicalFilter: (Entity) -> Boolean
    ): Boolean {
        return Recursion.anyRecursive(this) { entity, remaining ->
            if (checkInstance(includingDisabled, entity) && hierarchicalFilter(entity)) {
                if (entity.anyComponent(clazz, includingDisabled, predicate)) {
                    true
                } else {
                    remaining.addAll(entity.children)
                    false
                }
            } else false
        }
    }

    fun <V : Any> Component.anyComponentInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        return entity?.anyComponentInChildren(clazz, includingDisabled, predicate) ?: false
    }

    /**
     * Calls callback on all components of the specified type within entity, or its grand*children recursively.
     * If includingDisabled=false, recursion will stop at any disabled entity or component.
     * */
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
    ) {
        entity?.forAllComponentsInChildren(clazz, includingDisabled, callback)
    }

    inline fun <V : Any> Entity.sumComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        getCount: (V) -> Long
    ): Long {
        var sum = 0L
        forAllComponents(clazz, includingDisabled) { comp ->
            sum += getCount(comp)
        }
        return sum
    }

    inline fun <V : Any> Component.sumComponents(
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

    fun <ComponentType : Any, ListType : MutableList<ComponentType>> Entity.getComponentsInChildren(
        clazz: KClass<ComponentType>, includingDisabled: Boolean = false, dst: ListType
    ): ListType {
        anyComponentInChildren(clazz, includingDisabled) {
            dst.add(it)
            false
        }
        return dst
    }
}