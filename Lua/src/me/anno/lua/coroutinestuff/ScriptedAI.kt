package me.anno.lua.coroutinestuff

import me.anno.ecs.Component

class ScriptedAI : Component() {

    // it seems kotlin has coroutines as well...
    // todo see whether kotlin coroutines are powerful enough, and would make scripting easy

    var sequence: Iterator<Behaviour>? = null
    var behaviour: Behaviour? = null

    override fun onUpdate(): Int {
        val behaviour = behaviour ?: sequence?.run { if (hasNext()) next() else null }
        if (behaviour != null) {
            behaviour.update(entity!!, entity!!.transform)
            if (behaviour.isDead) this.behaviour = null
        }
        return 1
    }
}