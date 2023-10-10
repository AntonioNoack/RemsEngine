package me.anno.lua.utils

import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

class SafeFunction(val thread: LuaThread) : ZeroArgFunction() {
    override fun call(): LuaValue {
        return thread.resume(LuaValue.NIL).arg(2)
    }
}