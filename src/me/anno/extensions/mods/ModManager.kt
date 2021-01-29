package me.anno.extensions.mods

import me.anno.extensions.ExtensionManager
import me.anno.utils.hpc.HeavyProcessing.processStage

object ModManager: ExtensionManager<Mod>("Mod") {

    override fun onEnable(extensions: List<Mod>) {
        extensions.forEach { it.isRunning = true }
        processStage(extensions, true) { it.onPreInit() }
        processStage(extensions, true) { it.onInit() }
        processStage(extensions, true) { it.onPostInit() }
    }

    override fun onDisable(extensions: List<Mod>) {
        processStage(extensions, true) {
            it.isRunning = false
            it.onExit()
        }
    }

}