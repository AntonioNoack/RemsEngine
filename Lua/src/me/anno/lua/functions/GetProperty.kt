package me.anno.lua.functions

import me.anno.lua.ScriptComponent.Companion.toLua
import me.anno.io.ISaveable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

object GetProperty : TwoArgFunction() {
    override fun call(instance: LuaValue, name: LuaValue): LuaValue {
        val instance1 = instance.touserdata() ?: return NIL
        val name1 = name.tojstring() ?: return NIL
        return ISaveable.get(instance1, name1).toLua()
    }
}