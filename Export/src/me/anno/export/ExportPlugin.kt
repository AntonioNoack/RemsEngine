package me.anno.export

import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.Events.addEvent
import me.anno.engine.projects.GameEngineProject
import me.anno.extensions.events.EventHandler
import me.anno.extensions.plugins.Plugin
import me.anno.gpu.GFX
import me.anno.io.config.ConfigBasics.configFolder
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.editor.OptionBar
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.TextInput
import me.anno.utils.Color.white
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull

class ExportPlugin : Plugin() {

    val configFile get() = configFolder.getChild("Export.json")

    override fun onEnable() {
        super.onEnable()
        registerListener(this)
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
                    .firstInstanceOrNull<OptionBar>() ?: continue
                bar.removeMajor("Game Export")
            }
        }
    }

    fun registerExportMenu() {
        removeExistingExportButton()
        // inject export button into main menu
        for (window in GFX.windows) {
            for (window1 in window.windowStack) {
                val bar = window1.panel.listOfAll
                    .firstInstanceOrNull<OptionBar>() ?: continue
                bar.addMajor("Game Export", ::openExportMenu)
            }
        }
    }

    fun loadPresets(): List<ExportSettings> {
        return JsonStringReader.read(configFile, workspace, true)
            .filterIsInstance<ExportSettings>()
    }

    fun storePresets(presets: List<ExportSettings>) {
        configFile.writeText(JsonStringWriter.toText(presets, workspace))
    }

    fun openExportMenu() {
        val presets = loadPresets().sortedBy { it.name.lowercase() }
        val ui = PanelListY(style)
        // export setting chooser: a list of saveable presets
        val body = PanelListY(style)
        lateinit var preset: ExportSettings
        ui.add(EnumInput(
            NameDesc("Preset"), NameDesc("Please Choose"),
            listOf(NameDesc("New Preset")) +
                    presets.map { NameDesc(it.name, it.description, "") }, style
        ).setChangeListener { _, index, _ ->
            if (index == 0) {
                // create a new preset -> ask user for name
                askName(
                    GFX.someWindow.windowStack,
                    NameDesc(), "Preset Name", NameDesc("Create"), { white }, {
                        Menu.close(ui)
                        val newList = presets + ExportSettings().apply { name = it.trim() }
                        storePresets(newList)
                        openExportMenu()
                    }
                )
            } else {
                preset = presets[index - 1]
                body.clear()
                // inputs
                body.add(TextInput("Name", "", preset.name, style)
                    .addChangeListener { preset.name = it.trim() })
                body.add(TextInput("Description", "", preset.description, style)
                    .addChangeListener { preset.description = it.trim() })
                body.add(FileInput("Destination", style, preset.dstFile, emptyList(), false)
                    .addChangeListener { preset.dstFile = it })
                body.add(FileInput("Icon Override", style, preset.iconOverride, emptyList(), false)
                    .addChangeListener { preset.iconOverride = it })
                body.add(TextInput("Game Title", "", preset.gameTitle, style)
                    .addChangeListener { preset.gameTitle = it.trim() })
                body.add(TextInput("Config Name", "", preset.configName, style)
                    .addChangeListener { preset.configName = it.trim() })
                body.add(IntInput("Version Number", "", preset.versionNumber, style)
                    .setChangeListener { preset.versionNumber = it.toInt() })
                // todo inputs for all settings
                // buttons
                body.add(TextButton("Export", style)
                    .addLeftClickListener {
                        ExportProcess.execute(GameEngineProject.currentProject!!, preset)
                    })
                body.add(TextButton("Save Preset", style)
                    .addLeftClickListener {
                        storePresets(presets)
                        // todo show message that it was saved
                    })
                body.add(TextButton("Delete Preset", style)
                    .addLeftClickListener {
                        Menu.ask(body.windowStack, NameDesc("Delete ${preset.name}?")) {
                            storePresets(presets.filter { it !== preset })
                            Menu.close(body)
                            openExportMenu()
                        }
                    })
            }
        })
        ui.add(body)
        // todo open quick export: most recent 3 export settings
        val ws = GFX.someWindow.windowStack
        ws.add(Window(ui, false, ws))
    }
}