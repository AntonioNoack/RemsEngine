package me.anno.lua

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnEnable
import me.anno.ecs.systems.OnUpdate
import me.anno.lua.ScriptComponent.Companion.getFunction
import me.anno.lua.ScriptComponent.Companion.toLua
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue

@Suppress("MemberVisibilityCanBePrivate")
open class QuickScriptComponent : Component(), OnUpdate, OnEnable {

    @Type("Lua/Code")
    var enableScript = ""

    @Type("Lua/Code")
    var updateScript = ""

    @Type("Lua/Code")
    var destroyScript = ""

    override fun onEnable() {
        callFunction(enableScript)
    }

    @DebugAction
    override fun onUpdate() {
        callFunction(updateScript)
    }

    @DebugAction
    override fun destroy() {
        super.destroy()
        callFunction(destroyScript)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is QuickScriptComponent) return
        dst.enableScript = enableScript
        dst.updateScript = updateScript
        dst.destroyScript = destroyScript
    }

    fun getFunction1(code: String, init: ((scope: LuaValue) -> Unit)? = null): LuaValue {
        return getFunction(code) { globals ->
            globals.set("entity", entity.toLua())
            globals.set("transform", entity?.transform.toLua())
            globals.set("parent", parent.toLua())
            globals.set("dt", LuaValue.valueOf(Time.deltaTime))
            globals.set("t", LuaValue.valueOf(Time.gameTime))
            globals.set("player", LocalPlayer.currentLocalPlayer.toLua())
            init?.invoke(globals)
        }
    }

    fun callFunction(code: String) {
        val func = getFunction1(code) {}
        if (func.isfunction()) {
            try {
                val value = func.call()
                if (!value.isnil()) {
                    LOGGER.info("Return value: {}", value)
                }
            } catch (e: LuaError) {
                LOGGER.warn(e)
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(QuickScriptComponent::class)
    }
}