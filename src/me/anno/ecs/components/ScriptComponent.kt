package me.anno.ecs.components

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable

class ScriptComponent: Component() {

    // todo src or content? both?
    // todo languages supported?
    // todo lua from Java/Kotlin?
    // todo JavaScript from Java/Kotlin?

    override fun clone(): Component {
        TODO("Not yet implemented")
    }

    override val className get() = "ScriptComponent"

}