package me.anno.sdf

import me.anno.extensions.plugins.Plugin

class SDFPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        SDFRegistry.init()
    }
}
