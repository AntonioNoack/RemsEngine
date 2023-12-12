package me.anno.ecs

import me.anno.ecs.prefab.PrefabSaveable

// todo
//  - appended to the root,
//  - exists only once per tree,
//  - gets notified about new/deleted/changed Components,
//  - keeps a list/set/whatever of them around for fast iteration
//  - gets called once per frame onUpdate
class System : PrefabSaveable() {

}