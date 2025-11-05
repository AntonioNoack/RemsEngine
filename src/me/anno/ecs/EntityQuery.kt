package me.anno.ecs

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.saveable.Saveable
import me.anno.utils.algorithms.Recursion
import me.anno.utils.structures.Collections.filterIsInstance2
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
        return entity?.hasComponent(clazz, includingDisabled)
            ?: (checkInstance(includingDisabled, this) && clazz.isInstance(this))
    }

    fun <V : Component> Entity.hasComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
        return getComponentInChildren(clazz, includingDisabled) != null
    }

    fun <V : Component> Component.hasComponentInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false
    ): Boolean {
        return entity?.hasComponentInChildren(clazz, includingDisabled)
            ?: (checkInstance(includingDisabled, this) && clazz.isInstance(this))
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
        val entity = entity
        return when {
            entity != null -> entity.getComponent(clazz, includingDisabled)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                this as V
            }
            else -> null
        }
    }

    fun <V : Any> Entity.getComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return Recursion.findRecursive(this) { entity, remaining ->
            if (checkInstance(includingDisabled, entity)) {
                remaining.addAll(entity.children)
                entity.getComponent(clazz, includingDisabled)
            } else null
        }
    }

    fun <V : Any> Component.getComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        val entity = entity
        return when {
            entity != null -> entity.getComponentInChildren(clazz, includingDisabled)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                this as V
            }
            else -> null
        }
    }

    fun <V : Any> Entity.getComponentInHierarchy(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        var self = this
        while (true) { // prefer loops to recursion
            val v = self.getComponent(clazz, includingDisabled)
            if (v != null) return v
            self = self.parentEntity ?: break
        }
        return null
    }

    fun <V : Any> Component.getComponentInHierarchy(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        val entity = entity
        return when {
            entity != null -> entity.getComponentInHierarchy(clazz, includingDisabled)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                this as V
            }
            else -> null
        }
    }

    fun <V : Any> Entity.getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return components.filter { (includingDisabled || it.isEnabled) }.filterIsInstance2(clazz)
    }

    fun <V : Any> Component.getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        val entity = entity
        return when {
            entity != null -> entity.getComponents(clazz, includingDisabled)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                listOf(this as V)
            }
            else -> emptyList()
        }
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

    inline fun <V : Any> Entity.allComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean = !anyComponent(clazz, includingDisabled) { v -> !predicate(v) }

    inline fun <V : Any> Component.allComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean = !anyComponent(clazz, includingDisabled) { v -> !predicate(v) }

    inline fun <V : Any> Entity.anyComponent(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val anyComponent = components[index]
            if (checkInstance(includingDisabled, anyComponent)) {
                val component = clazz.safeCast(anyComponent)
                if (component != null && predicate(component)) {
                    return true
                }
            }
        }
        return false
    }

    inline fun <V : Any> Component.anyComponent(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        return entity?.anyComponent(clazz, includingDisabled, predicate)
            ?: (checkInstance(includingDisabled, this) && clazz.isInstance(this) && predicate(this as V))
    }

    inline fun <V : Any> Entity.forAllComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) {
        anyComponent(clazz, includingDisabled) { v ->
            callback(v)
            false
        }
    }

    fun <V : Any> Entity.forAllComponentsInChildrenAndBounds(
        clazz: KClass<V>, bounds: AABBd, includingDisabled: Boolean,
        callback: (V) -> Unit
    ) {
        anyComponentInChildrenAndBounds(clazz, bounds, includingDisabled) { v ->
            callback(v)
            false
        }
    }

    fun <V : Any> Component.forAllComponents(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) {
        val entity = entity
        return when {
            entity != null -> entity.forAllComponents(clazz, includingDisabled, callback)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                callback(this as V)
            }
            else -> {}
        }
    }

    fun <V : Any> Entity.anyComponentInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ) = anyComponentInChildrenAndBounds(clazz, null, includingDisabled, predicate)

    fun <V : Any> Entity.anyComponentInChildrenAndBounds(
        clazz: KClass<V>, bounds: AABBd?, includingDisabled: Boolean = false, predicate: (V) -> Boolean
    ): Boolean = anyComponentInChildrenFiltered(clazz, includingDisabled, predicate) {
        bounds == null || bounds.testAABB(it.getGlobalBounds())
    }

    fun <V : Any> Entity.anyComponentInChildrenFiltered(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean, hierarchicalFilter: (Entity) -> Boolean
    ): Boolean = Recursion.anyRecursive(this) { entity, remaining ->
        if (checkInstance(includingDisabled, entity) && hierarchicalFilter(entity)) {
            if (entity.anyComponent(clazz, includingDisabled, predicate)) {
                true
            } else {
                remaining.addAll(entity.children)
                false
            }
        } else false
    }

    fun <V : Any> Component.anyComponentInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        predicate: (V) -> Boolean
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        return entity?.anyComponentInChildren(clazz, includingDisabled, predicate)
            ?: (checkInstance(includingDisabled, this) && clazz.isInstance(this) && predicate(this as V))
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

    /**
     * Calls the callback on any components of class clazz within this component's entity.
     * If includingDisabled=false, recursion will stop at any disabled entity or component.
     * */
    fun <V : Any> Component.forAllComponentsInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        callback: (V) -> Unit
    ) {
        val entity = entity
        return when {
            entity != null -> entity.forAllComponentsInChildren(clazz, includingDisabled, callback)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                callback(this as V)
            }
            else -> {}
        }
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
    ): Long {
        val entity = entity
        return when {
            entity != null -> entity.sumComponents(clazz, includingDisabled, getCount)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                getCount(this as V)
            }
            else -> 0L
        }
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

    fun <V : Any> Component.sumComponentsInChildren(
        clazz: KClass<V>, includingDisabled: Boolean = false,
        getCount: (V) -> Long
    ): Long {
        val entity = entity
        return when {
            entity != null -> entity.sumComponentsInChildren(clazz, includingDisabled, getCount)
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                getCount(this as V)
            }
            else -> 0L
        }
    }

    fun <V : Any> Entity.getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return getComponentsInChildren(clazz, includingDisabled, ArrayList())
    }

    fun <V : Any> Component.getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        val entity = entity
        return when {
            entity != null -> entity.getComponentsInChildren(clazz, includingDisabled, ArrayList())
            checkInstance(includingDisabled, this) && clazz.isInstance(this) -> {
                @Suppress("UNCHECKED_CAST")
                listOf(this as V)
            }
            else -> emptyList()
        }
    }

    fun <ComponentType : Any, ListType : MutableList<ComponentType>> Entity.getComponentsInChildren(
        clazz: KClass<ComponentType>, includingDisabled: Boolean = false, dst: ListType
    ): ListType {
        anyComponentInChildren(clazz, includingDisabled) { v ->
            dst.add(v)
            false
        }
        return dst
    }
}