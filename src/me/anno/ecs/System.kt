package me.anno.ecs

import me.anno.ecs.prefab.PrefabSaveable

// todo
//  - appended to the root,
//  - exists only once per tree,
//  - gets notified about new/deleted/changed Components,
//  - keeps a list/set/whatever of them around for fast iteration
//  - gets called once per frame onUpdate
/**
 * systems are currently in planning/experimenting stage
 * */
abstract class System : PrefabSaveable() {

    open val priority: Int get() = 1

    open fun onEnable(entity: Entity) {}
    open fun onEnable(component: Component) {}

    open fun onDisable(entity: Entity) {}
    open fun onDisable(component: Component) {}

    open fun onUpdate() {}

    open fun clear() {}
}