package me.anno.lua.utils

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

object ErrorFunction : ZeroArgFunction() {
    override fun call(): LuaValue {
        throw Error("Instruction limit exceeded")
    }
}