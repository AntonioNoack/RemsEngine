package me.anno.extensions.mods

import me.anno.extensions.ExtensionManager
import me.anno.utils.hpc.HeavyProcessing.processStage

object ModManager : ExtensionManager<Mod>("Mod") {

    override fun onEnable(extensions: List<Mod>) {
        for (ext in extensions) ext.isRunning = true
        processStage(extensions, { it.priority }, true) { it.onPreInit() }
        processStage(extensions, { it.priority }, true) { it.onInit() }
        processStage(extensions, { it.priority }, true) { it.onPostInit() }
    }

    override fun onDisable(extensions: List<Mod>) {
        processStage(extensions, { it.priority }, true) {
            it.isRunning = false
            it.onExit()
        }
    }

}