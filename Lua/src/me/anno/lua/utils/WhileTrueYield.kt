package me.anno.lua.utils

import me.anno.utils.ShutdownException
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaThread.STATUS_DEAD
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction

class WhileTrueYield(
    val globals: Globals,
    val func: LuaValue,
    val setHook: LuaValue,
    val instructionLimit: Int
) : LibFunction() {
    lateinit var thread: LuaThread
    override fun invoke(varargs: Varargs?): Varargs {
        while (thread.state.status != STATUS_DEAD) {
            val limit = instructionLimit
            // reset instruction limit
            thread.state.bytecodes = 0
            setHook.invoke(
                LuaValue.varargsOf(
                    arrayOf(
                        thread,
                        ErrorFunction,
                        LuaValue.EMPTYSTRING,
                        LuaValue.valueOf(limit)
                    )
                )
            )
            val ret = func.call()
            globals.yield(ret)
        }
        throw ShutdownException()
    }
}