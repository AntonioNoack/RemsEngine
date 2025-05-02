package me.anno.ecs

import me.anno.ecs.prefab.PrefabSaveable

/**
 * Systems are meant for behaviours, on lots and lots of components,
 * without needing to iterate over the scene tree: just cache all active components.
 * This improves predictability for the CPU, and reduces overhead.
 *
 * In most cases, you want to call a function every frame: implement OnUpdate for that.
 * In 2nd most cases, you want to run every nth frame, or run an operation on all elements together per frame - use Updatable for that.
 * OnUpdate is just a specialized implementation of Updatable.
 * */
abstract class System : PrefabSaveable() {

    open val priority: Int get() = 1

    open fun setContains(entity: Entity, contains: Boolean) {}
    open fun setContains(component: Component, contains: Boolean) {}

    open fun clear() {}
}