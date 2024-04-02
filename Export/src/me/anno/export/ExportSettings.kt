package me.anno.export

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Docs
import me.anno.engine.inspector.Inspectable
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
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.TextInput

class ExportSettings : NamedSaveable(), Inspectable {

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

    val modulesToInclude = HashSet<String>()

    var firstSceneRef: FileReference = InvalidRef

    @Docs("Collection of files/folders; external stuff is always exported, when referenced by an internal asset")
    val assetsToInclude = HashSet<FileReference>()

    // idk...
    val excludedClasses = HashSet<String>()

    var dstFile: FileReference = InvalidRef
    var iconOverride: FileReference = InvalidRef

    var lastUsed = 0L

    var linuxPlatforms = LinuxPlatforms()
    var windowsPlatforms = WindowsPlatforms()
    var macosPlatforms = MacOSPlatforms()

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeStringArray("modulesToInclude", modulesToInclude.toTypedArray())
        writer.writeFileArray("assetsToInclude", assetsToInclude.toTypedArray())
        writer.writeStringArray("excludedClasses", excludedClasses.toTypedArray())
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        if (readSerializableProperty(name, value)) return
        when (name) {
            "modulesToInclude" -> loadStringArray(modulesToInclude, value)
            "assetsToInclude" -> loadFileArray(assetsToInclude, value)
            "excludedClasses" -> loadStringArray(excludedClasses, value)
            else -> super.setProperty(name, value)
        }
    }

    private fun loadFileArray(dst: MutableCollection<FileReference>, value: Any?) {
        if (value is Array<*>) {
            dst.clear()
            dst.addAll(value.filterIsInstance<FileReference>())
        }
    }

    private fun loadStringArray(dst: MutableCollection<String>, value: Any?) {
        if (value is Array<*>) {
            dst.clear()
            dst.addAll(value.filterIsInstance<String>())
        }
    }

    fun clone(): ExportSettings {
        return JsonStringReader.readFirst(JsonStringWriter.toText(this, InvalidRef), InvalidRef)
    }

    override fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (nameDesc: NameDesc) -> SettingCategory
    ) {
        // general settings
        val general = getGroup(NameDesc("General"))
        general.addChild(TextInput("Name", "", name, DefaultConfig.style)
            .addChangeListener { name = it.trim() })
        general.addChild(TextInput("Description", "", description, DefaultConfig.style)
            .addChangeListener { description = it.trim() })
        general.addChild(FileInput("Destination", DefaultConfig.style, dstFile, emptyList(), false)
            .addChangeListener { dstFile = it })
        general.addChild(FileInput("Icon Override", DefaultConfig.style, iconOverride, emptyList(), false)
            .addChangeListener { iconOverride = it })
        general.addChild(TextInput("Game Title", "", gameTitle, DefaultConfig.style)
            .addChangeListener { gameTitle = it.trim() })
        general.addChild(TextInput("Config Name", "", configName, DefaultConfig.style)
            .addChangeListener { configName = it.trim() })
        general.addChild(IntInput("Version Number", "", versionNumber, DefaultConfig.style)
            .setChangeListener { versionNumber = it.toInt() })
        general.addChild(FileInput("First Scene", DefaultConfig.style, firstSceneRef, emptyList(), false)
            .addChangeListener { firstSceneRef = it })
        // platforms
        val linux = getGroup(NameDesc("Linux"))
        linux.addChild(BooleanInput("x64", linuxPlatforms.x64, true, style)
            .setChangeListener { linuxPlatforms.x64 = it })
        linux.addChild(BooleanInput("arm64", linuxPlatforms.arm64, true, style)
            .setChangeListener { linuxPlatforms.arm64 = it })
        linux.addChild(BooleanInput("arm32", linuxPlatforms.arm32, false, style)
            .setChangeListener { linuxPlatforms.arm32 = it })
        val windows = getGroup(NameDesc("Windows"))
        windows.addChild(BooleanInput("x64 (64-bit)", windowsPlatforms.x64, true, style)
            .setChangeListener { windowsPlatforms.x64 = it })
        windows.addChild(BooleanInput("x86 (32-bit)", windowsPlatforms.x86, false, style)
            .setChangeListener { windowsPlatforms.x86 = it })
        windows.addChild(BooleanInput("arm64", windowsPlatforms.arm64, true, style)
            .setChangeListener { windowsPlatforms.arm64 = it })
        val macos = getGroup(NameDesc("MacOS"))
        macos.addChild(BooleanInput("x64 (Intel)", macosPlatforms.x64, true, style)
            .setChangeListener { macosPlatforms.x64 = it })
        macos.addChild(BooleanInput("arm64 (M-Series)", macosPlatforms.arm64, true, style)
            .setChangeListener { macosPlatforms.arm64 = it })
        // todo inputs for all settings
    }
}