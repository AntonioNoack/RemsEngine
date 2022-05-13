package me.anno.ecs.components.script.functions

import me.anno.io.ISaveable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction

object SetProperty : ThreeArgFunction() {

    override fun call(instance: LuaValue, name: LuaValue, value: LuaValue): LuaValue {
        val instance1 = instance.touserdata() ?: return FALSE
        val name1 = name.tojstring() ?: return FALSE
        val value1: Any? = toJava(value)
        return if (ISaveable.set(instance1, name1, value1)) TRUE else FALSE
    }

    fun toJava(value: LuaValue) = when {
        value.isboolean() -> value.toboolean()
        value.isint() -> value.toint()
        value.islong() -> value.tolong()
        value.isnumber() -> value.todouble()
        value.isstring() -> value.tostring()
        value.isuserdata() -> value.touserdata()
        // todo add more types
        else -> null
    }

}