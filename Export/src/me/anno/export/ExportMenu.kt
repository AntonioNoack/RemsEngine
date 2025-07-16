package me.anno.export

import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.Events.addEvent
import me.anno.engine.projects.GameEngineProject
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.EnumInput
import me.anno.utils.Clock
import me.anno.utils.Color.white
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
import java.io.IOException

class ExportMenu(val configFile: FileReference, val presetName: String?) {

    companion object {
        private val LOGGER = LogManager.getLogger(ExportMenu::class)
    }

    val presets = loadPresets()
    val ui = PanelListY(style)

    fun loadPresets(): List<ExportSettings> {
        return try {
            (JsonStringReader.read(configFile, workspace, true)
                .waitFor() ?: emptyList())
                .filterIsInstance2(ExportSettings::class)
                .sortedByDescending { it.lastUsed }
        } catch (e: IOException) {
            LOGGER.warn("Failed parsing presets", e)
            emptyList()
        }
    }

    fun storePresets(presets: List<ExportSettings>) {
        configFile.writeText(JsonStringWriter.toText(presets, workspace))
    }

    fun reloadUI(nameToLoad: String?) {
        Menu.close(ui)
        ExportMenu(configFile, nameToLoad)
    }

    fun markPresetAsUsed(preset: ExportSettings) {
        // save that we used it
        val presets0 = loadPresets()
        presets0.firstOrNull2 { it.name == preset.name }
            ?.lastUsed = System.currentTimeMillis()
        storePresets(presets0)
    }

    fun runExport(preset: ExportSettings) {
        markPresetAsUsed(preset)
        runOnNonGFXThread("Export") {
            val clock = Clock(LOGGER)
            val progress = GFX.someWindow.addProgressBar("Export", "Files", 1.0)
            progress.intFormatting = true
            ExportProcess.execute(GameEngineProject.currentProject!!, preset, progress) { success, err ->
                if (success != null) {
                    clock.stop("Export")
                    addEvent { msg(NameDesc("Export Finished!")) }
                } else {
                    LOGGER.warn("Export failed!", err ?: Exception())
                    progress.cancel(true)
                    addEvent { msg(NameDesc("Failed Export :/")) }
                }
            }
        }
    }

    fun addSeparator(body: PanelList) {
        body.add(SpacerPanel(0, 8, style).makeBackgroundTransparent())
        body.add(SpacerPanel(0, 1, style.getChild("deep")))
        body.add(SpacerPanel(0, 8, style).makeBackgroundTransparent())
    }

    val body = PanelListY(style)
    fun createPresetUI(preset: ExportSettings) {
        body.clear()
        // quick-button
        body.add(
            TextButton(NameDesc("Export"), style)
                .addLeftClickListener {
                    runExport(preset)
                })
        // inputs
        preset.createInspector(body, style, { nameDesc, parent ->
            val group = SettingCategory(nameDesc, style).showByDefault()
            parent.add(group)
            group.content
        }, {
            createPresetUI(preset)
        })
        addSeparator(body)
        // buttons
        body.add(
            TextButton(NameDesc("Export"), style)
                .addLeftClickListener {
                    runExport(preset)
                })
        body.add(
            TextButton(NameDesc("Save Preset"), style)
                .addLeftClickListener {
                    preset.lastUsed = System.currentTimeMillis()
                    storePresets(presets)
                    msg(NameDesc("Saved Preset!"))
                })
        body.add(
            TextButton(NameDesc("Save Preset As..."), style)
                .addLeftClickListener {
                    askName(
                        ui.windowStack, NameDesc("Preset Name"), preset.name,
                        NameDesc("Save"), { -1 }) { newName0 ->
                        val newPreset = preset.clone() as ExportSettings
                        val newName = newName0.trim()
                        newPreset.name = newName
                        newPreset.lastUsed = System.currentTimeMillis()
                        // presets must be reloaded, because maybe we changed the current preset,
                        // and now we want to save it under a NEW name, not the old one
                        storePresets(
                            loadPresets()
                                .filter { it.name != newName } + newPreset)
                        msg(NameDesc("Saved Preset!"))
                        reloadUI(newName)
                    }
                })
        body.add(
            TextButton(NameDesc("Delete Preset"), style)
                .addLeftClickListener {
                    ask(ui.windowStack, NameDesc("Delete ${preset.name}?")) {
                        storePresets(presets.filter { it !== preset })
                        reloadUI(presetName)
                    }
                })
    }

    val loadedInitially = presets
        .firstOrNull { it.name == presetName }
        ?: presets.firstOrNull()

    init {

        if (loadedInitially != null) {
            createPresetUI(loadedInitially)
        }

        ui.add(
            EnumInput(
                NameDesc("Preset"),
                NameDesc(loadedInitially?.name ?: "Please Choose"),
                listOf(NameDesc("New Preset")) +
                        presets.map { it.nameDesc }, style
            ).setChangeListener { _, index, _ ->
                if (index == 0) {
                    // create a new preset -> ask user for name
                    askName(
                        GFX.someWindow.windowStack,
                        NameDesc.EMPTY, "Preset Name", NameDesc("Create"), { white }, { newName0 ->
                            Menu.close(ui)
                            val newName = newName0.trim()
                            val newList =
                                loadPresets().filter { it.name != newName } +
                                        ExportSettings().apply { name = newName }
                            storePresets(newList)
                            reloadUI(newName)
                        }
                    )
                } else {
                    val preset = presets[index - 1]
                    storePresets(loadPresets()) // undo all not-saved changes
                    reloadUI(preset.name)
                }
            })

        ui.add( // add a decorative strip around the settings ^^
            PanelContainer(
                PanelContainer(
                    PanelContainer(body, Padding(4), style),
                    Padding(1), style.getChild("deep")
                ), Padding(4), style
            )
        )

        GFX.someWindow.windowStack
            .push(ScrollPanelY(ui, style))
    }
}