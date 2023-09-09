package me.anno.lua

import me.anno.extensions.plugins.Plugin

class LuaPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        LuaRegistry.init()
    }
}
