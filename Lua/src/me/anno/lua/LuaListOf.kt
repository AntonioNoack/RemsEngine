package me.anno.lua

import me.anno.lua.ScriptComponent.Companion.toJava
import me.anno.lua.ScriptComponent.Companion.toLua
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

object LuaListOf : VarArgFunction() {
    override fun onInvoke(args: Varargs): Varargs {
        val size = args.narg() - 1
        return (0 until size).map {
            args.arg(it + 2).toJava()
        }.toLua()
    }
}