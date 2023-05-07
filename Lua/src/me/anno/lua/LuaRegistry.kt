package me.anno.lua

import me.anno.config.DefaultConfig.style
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.lua.ui.LuaAnimTextPanel

@Suppress("unused")
object LuaRegistry {
    @JvmStatic
    fun init() {
        registerCustomClass(ScriptComponent())
        registerCustomClass(QuickScriptComponent())
        registerCustomClass(QuickInputScriptComponent())
        registerCustomClass { LuaAnimTextPanel(style) }
    }
}