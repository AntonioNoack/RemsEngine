package me.anno.lua.coroutinestuff

import me.anno.ecs.Component
import me.anno.lua.ScriptComponent.Companion.defineVM
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaValue

class ScriptedLuaAI : Component() {

    /*
     * -- the interesting, personalizable stuff
     * dynamics[someInstance] = coroutine.create(function()
     *   while true do
     *    action()
     *    coroutine.yield()
     *    action()
     *    coroutine.yield()
     *   end
     * end)
     *
     * -- called from lua first, then from native
     * function issueNextTask(host, someInstance)
     *   dyn = dynamics[someInstance]
     *   if(coroutine.status(dyn ~= 'dead' then
     *      coroutine.resume(dyn, host, someInstance)
     *   end
     * end
     */

    var script = ""

    var routine: LuaThread? = null

    fun getStatus(): Int {
        return routine?.state?.status ?: LuaThread.STATUS_DEAD
    }

    fun create(vm: Globals, func: LuaValue): LuaThread {
        return LuaThread(vm, func)
    }

}