package me.anno.lua

import me.anno.config.DefaultConfig
import me.anno.extensions.plugins.Plugin
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.lua.ui.LuaAnimTextPanel

class LuaPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        registerCustomClass(ScriptComponent())
        registerCustomClass(QuickScriptComponent())
        registerCustomClass(QuickInputScriptComponent())
        registerCustomClass { LuaAnimTextPanel(DefaultConfig.style) }
    }
}
