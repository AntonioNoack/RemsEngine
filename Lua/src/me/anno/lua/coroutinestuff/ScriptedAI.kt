package me.anno.lua.coroutinestuff

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate

class ScriptedAI : Component(), OnUpdate {

    // it seems kotlin has coroutines as well...
    // todo see whether kotlin coroutines are powerful enough, and would make scripting easy

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