package me.anno.ecs

import me.anno.io.NamedSaveable

abstract class Component : NamedSaveable() {

    var entity: Entity? = null

    open fun onCreate() {}

    open fun onDestroy() {}

    open fun onBeginPlay() {}

    open fun onUpdate() {}

    open fun onPhysicsUpdate() {}

    override fun getApproxSize(): Int = 1000
    override fun isDefaultValue(): Boolean = false

    // todo automatic property inspector by reflection
    // todo property inspector annotations, e.g. Range, ExecuteInEditMode, HideInInspector, GraphicalValueTracker...

}