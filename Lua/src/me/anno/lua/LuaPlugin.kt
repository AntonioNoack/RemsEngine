package me.anno.lua

import me.anno.config.DefaultConfig
import me.anno.extensions.plugins.Plugin
import me.anno.io.Saveable
import me.anno.lua.ui.LuaAnimTextPanel

class LuaPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        Saveable.registerCustomClass(ScriptComponent())
        Saveable.registerCustomClass(QuickScriptComponent())
        Saveable.registerCustomClass(QuickInputScriptComponent())
        Saveable.registerCustomClass { LuaAnimTextPanel(DefaultConfig.style) }
    }
}
