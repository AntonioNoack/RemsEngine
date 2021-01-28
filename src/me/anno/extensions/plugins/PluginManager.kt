package me.anno.extensions.plugins

import me.anno.extensions.ExtensionManager
import me.anno.utils.processStage

object PluginManager: ExtensionManager<Plugin>("Plugin") {

    override fun onEnable(extensions: List<Plugin>) {
        processStage(extensions, true) {
            it.isRunning = true
            it.onEnable()
        }
    }

    override fun onDisable(extensions: List<Plugin>) {
        processStage(extensions, true) {
            it.isRunning = false
            it.onDisable()
        }
    }

}