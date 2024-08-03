package me.anno.lua.coroutinestuff

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate

class ScriptedAI : Component(), OnUpdate {

    // it seems kotlin has coroutines as well...
    // see whether kotlin coroutines are powerful enough, and would make scripting easy
    //  -> yes, they could, but idk if I want that 2MB dependency
    //  -> we probably should replace that with our own implementation again, like with reflections

    var sequence: Iterator<Behaviour>? = null
    var behaviour: Behaviour? = null

    override fun onUpdate() {
        val behaviour = behaviour ?: sequence?.run { if (hasNext()) next() else null }
        val entity = entity
        if (behaviour != null && entity != null) {
            behaviour.update(entity, entity.transform)
            if (behaviour.isDead) this.behaviour = null
        }
    }
}