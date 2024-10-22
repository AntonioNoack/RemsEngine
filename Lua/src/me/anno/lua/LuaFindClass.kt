package me.anno.lua

import me.anno.io.saveable.Saveable
import me.anno.lua.ScriptComponent.Companion.toLua
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

object LuaFindClass : OneArgFunction() {
    override fun call(arg: LuaValue?): LuaValue {
        return if (arg != null && arg.isstring()) {
            Saveable.getClass(arg.tojstring()).toLua()
        } else LuaValue.NIL
    }
}