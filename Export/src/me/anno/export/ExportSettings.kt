package me.anno.export

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Docs
import me.anno.export.idea.IdeaProject
import me.anno.export.idea.IdeaProject.Companion.kotlinc
import me.anno.export.platform.LinuxPlatforms
import me.anno.export.platform.MacOSPlatforms
import me.anno.export.platform.WindowsPlatforms
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.stacked.ArrayPanel
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.TextInput
import me.anno.utils.OS.documents

class ExportSettings : NamedSaveable() {

    // todo:
    //  - which platforms to support (LWJGL libraries) -> Linux, Windows, MacOS, x86/arm
    //  - exclude classes from ECSRegistry via
    //      a) reflections (as their implementation -> fail save),
    //      b) renaming them inside the .class file?,
    //      c) rewriting the file using the Kotlin compiler
    //      d) replacing their .class files with stubs? (still registered, but without any content) <- might be the cleanest solution

    // todo TreeView for FilesToInclude/Exclude: check boxes on every level, and then state gets saved

    var gameTitle = ""
    var configName = ""
    var versionNumber = 1

    var minimalUI = false

    val projectRoots = arrayListOf(documents.getChild("IdeaProjects/RemsEngine"))

    val excludedModules = HashSet<String>()

    var firstSceneRef: FileReference = InvalidRef

    @Docs("Collection of files/folders; external stuff is always exported, when referenced by an internal asset")
    val includedAssets = HashSet<FileReference>()

    // idk...
    val excludedClasses = HashSet<String>()

    var dstFile: FileReference = InvalidRef
    var iconOverride: FileReference = InvalidRef

    var lastUsed = 0L

    var linuxPlatforms = LinuxPlatforms()
    var windowsPlatforms = WindowsPlatforms()
    var macosPlatforms = MacOSPlatforms()

    override val approxSize: Int get() = 1000

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFileList("projectRoots", projectRoots)
        writer.writeStringList("excludedClasses", excludedClasses.toList())
        writer.writeStringList("excludedModules", excludedModules.toList())
        writer.writeFileList("includedAssets", includedAssets.toList())
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "excludedModules" -> loadStringArray(excludedModules, value)
            "excludedClasses" -> loadStringArray(excludedClasses, value)
            "projectRoots" -> loadFileArray(projectRoots, value)
            "includedAssets" -> loadFileArray(includedAssets, value)
            else -> if (!readSerializableProperty(name, value)) {
                super.setProperty(name, value)
            }
        }
    }

    private fun loadFileArray(dst: MutableCollection<FileReference>, value: Any?) {
        if (value is List<*>) {
            dst.clear()
            dst.addAll(value.filterIsInstance<FileReference>())
        }
    }

    private fun loadStringArray(dst: MutableCollection<String>, value: Any?) {
        if (value is List<*>) {
            dst.clear()
            dst.addAll(value.filterIsInstance<String>())
        }
    }

    fun clone(): ExportSettings {
        return JsonStringReader.readFirst(JsonStringWriter.toText(this, InvalidRef), InvalidRef)
    }

    fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (nameDesc: NameDesc, parent: PanelList) -> PanelList
    ) {
        // general settings
        val general = getGroup(NameDesc("General"), list)
        general.add(TextInput("Name", "", name, DefaultConfig.style)
            .addChangeListener { name = it.trim() })
        general.add(TextInput("Description", "", description, DefaultConfig.style)
            .addChangeListener { description = it.trim() })
        general.add(FileInput("Destination", DefaultConfig.style, dstFile, emptyList(), false)
            .addChangeListener { dstFile = it })
        general.add(FileInput("Icon Override", DefaultConfig.style, iconOverride, emptyList(), false)
            .addChangeListener { iconOverride = it })
        general.add(TextInput("Game Title", "", gameTitle, DefaultConfig.style)
            .addChangeListener { gameTitle = it.trim() })
        general.add(TextInput("Config Name", "", configName, DefaultConfig.style)
            .addChangeListener { configName = it.trim() })
        general.add(IntInput("Version Number", "", versionNumber, DefaultConfig.style)
            .setChangeListener { versionNumber = it.toInt() })
        general.add(FileInput("First Scene", DefaultConfig.style, firstSceneRef, emptyList(), false)
            .addChangeListener { firstSceneRef = it })
        val shared = getGroup(NameDesc("Shared Settings"), list)
        shared.add(FileInput("Kotlinc Folder", style, kotlinc, emptyList(), true)
            .addChangeListener { kotlinc = it })
        // platforms
        val platforms = getGroup(NameDesc("Platforms"), list)
        val linux = getGroup(NameDesc("Linux"), platforms)
        linux.add(BooleanInput("x64", linuxPlatforms.x64, true, style)
            .setChangeListener { linuxPlatforms.x64 = it })
        linux.add(BooleanInput("arm64", linuxPlatforms.arm64, true, style)
            .setChangeListener { linuxPlatforms.arm64 = it })
        linux.add(BooleanInput("arm32", linuxPlatforms.arm32, false, style)
            .setChangeListener { linuxPlatforms.arm32 = it })
        val windows = getGroup(NameDesc("Windows"), platforms)
        windows.add(BooleanInput("x64 (64-bit)", windowsPlatforms.x64, true, style)
            .setChangeListener { windowsPlatforms.x64 = it })
        windows.add(BooleanInput("x86 (32-bit)", windowsPlatforms.x86, false, style)
            .setChangeListener { windowsPlatforms.x86 = it })
        windows.add(BooleanInput("arm64", windowsPlatforms.arm64, true, style)
            .setChangeListener { windowsPlatforms.arm64 = it })
        val macos = getGroup(NameDesc("MacOS"), platforms)
        macos.add(BooleanInput("x64 (Intel)", macosPlatforms.x64, true, style)
            .setChangeListener { macosPlatforms.x64 = it })
        macos.add(BooleanInput("arm64 (M-Series)", macosPlatforms.arm64, true, style)
            .setChangeListener { macosPlatforms.arm64 = it })
        // modules
        val logicSources = getGroup(NameDesc("Project Roots"), list)
        logicSources.add(
            object : ArrayPanel<FileReference, Panel>("IntellijIdea Projects", "", { InvalidRef }, style) {
                override fun createPanel(value: FileReference): Panel {
                    return FileInput("Project Root", style, value, emptyList(), true)
                        .addChangeListener { ref -> set(this, ref) }
                }

                override fun onChange() {
                    projectRoots.clear()
                    projectRoots.addAll(values)
                    // todo update modules, and their checkboxes
                }
            }.apply {
                setValues(projectRoots)
            }
        )

        // todo show a warning when a dependency isn't fulfilled
        val modules = getGroup(NameDesc("Included Modules"), list)
        val moduleList = projectRoots
            .flatMap { IdeaProject.loadModules(it) }
            .toHashSet()
            .sortedBy { it.name }
        for (file in moduleList) {
            val name = file.nameWithoutExtension
            modules.add(
                BooleanInput(name, name !in excludedModules, false, style)
                    .setChangeListener { included ->
                        if (included) excludedModules.remove(name)
                        else excludedModules.add(name)
                    }.setTooltip(file.toLocalPath())
            )
        }
        val opt = getGroup(NameDesc("Space Optimization"), list)
        opt.add(BooleanInput("Minimal UI", minimalUI, false, style)
            .setChangeListener { minimalUI = it })

        // assets
        val assets = getGroup(NameDesc("Assets"), list)

        // todo inputs for all settings
    }
}