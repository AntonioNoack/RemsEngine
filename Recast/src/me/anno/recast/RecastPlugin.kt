package me.anno.recast

import me.anno.extensions.plugins.Plugin
import me.anno.io.saveable.Saveable.Companion.registerCustomClass

class RecastPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        registerCustomClass(AgentType::class)
    }
}
