package me.anno.export

import me.anno.engine.Events.addEvent
import me.anno.engine.projects.GameEngineProject
import me.anno.export.platform.LinuxPlatforms
import me.anno.export.platform.MacOSPlatforms
import me.anno.export.platform.WindowsPlatforms
import me.anno.extensions.events.EventHandler
import me.anno.extensions.plugins.Plugin
import me.anno.gpu.GFX
import me.anno.io.config.ConfigBasics.configFolder
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.ui.editor.OptionBar
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2

class ExportPlugin : Plugin() {

    val configFile get() = configFolder.getChild("Export.json")

    override fun onEnable() {
        super.onEnable()
        registerListener(this)
        registerCustomClass(ExportSettings())
        registerCustomClass(LinuxPlatforms())
        registerCustomClass(WindowsPlatforms())
        registerCustomClass(MacOSPlatforms())
        addEvent(::registerExportMenu)
    }

    override fun onDisable() {
        super.onDisable()
        addEvent(::removeExistingExportButton)
    }

    @EventHandler
    fun onLoadProject(event: GameEngineProject.ProjectLoadedEvent) {
        // before we load a project, there isn't really an OptionBar ->
        // every time a project is loaded, this needs to be called
        registerExportMenu()
    }

    fun removeExistingExportButton() {
        // remove existing export button from main menu
        for (window in GFX.windows) {
            for (window1 in window.windowStack) {
                val bar = window1.panel.listOfAll
                    .firstInstanceOrNull2(OptionBar::class) ?: continue
                bar.removeMajor("Export")
            }
        }
    }

    fun registerExportMenu() {
        removeExistingExportButton()
        // inject export button into main menu
        for (window in GFX.windows) {
            for (window1 in window.windowStack) {
                val bar = window1.panel.listOfAll
                    .firstInstanceOrNull2(OptionBar::class) ?: continue
                bar.addMajor("Export") { ExportMenu(configFile, null) }
            }
        }
    }
}