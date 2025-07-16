package me.anno.export

import me.anno.utils.Threads
import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Docs
import me.anno.export.idea.IdeaProject
import me.anno.export.idea.IdeaProject.Companion.kotlinc
import me.anno.export.platform.LinuxPlatforms
import me.anno.export.platform.MacOSPlatforms
import me.anno.export.platform.WindowsPlatforms
import me.anno.extensions.ExtensionInfo
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.NamedSaveable
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
import me.anno.utils.async.Callback.Companion.mapCallback

class ExportSettings : NamedSaveable() {

    // todo TreeView for FilesToInclude/Exclude: check boxes on every level, and then state gets saved

    // todo option to bundle ffmpeg to be fully self-contained
    // todo could we use the Windows API to load video to optimize game size a little more, or not needing to unpack?

    var gameTitle = ""
    var configName = ""
    var versionNumber = 1

    var minimalUI = false
    var useKotlynReflect = false

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
            else -> if (!setSerializableProperty(name, value)) {
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

    fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (nameDesc: NameDesc, parent: PanelList) -> PanelList,
        refresh: () -> Unit
    ) {
        // general settings
        val general = getGroup(NameDesc("General"), list)
        general.add(
            TextInput(NameDesc("Name"), "", name, DefaultConfig.style)
                .addChangeListener { name = it.trim() })
        general.add(
            TextInput(NameDesc("Description"), "", description, DefaultConfig.style)
                .addChangeListener { description = it.trim() })
        general.add(
            FileInput(NameDesc("Destination"), DefaultConfig.style, dstFile, emptyList(), false)
                .addChangeListener { dstFile = it })
        general.add(
            FileInput(NameDesc("Icon Override"), DefaultConfig.style, iconOverride, emptyList(), false)
                .addChangeListener { iconOverride = it })
        general.add(
            TextInput(NameDesc("Game Title"), "", gameTitle, DefaultConfig.style)
                .addChangeListener { gameTitle = it.trim() })
        general.add(
            TextInput(NameDesc("Config Name"), "", configName, DefaultConfig.style)
                .addChangeListener { configName = it.trim() })
        general.add(
            IntInput(NameDesc("Version Number"), "", versionNumber, DefaultConfig.style)
                .setChangeListener { versionNumber = it.toInt() })
        general.add(
            FileInput(NameDesc("First Scene"), DefaultConfig.style, firstSceneRef, emptyList(), false)
                .addChangeListener { firstSceneRef = it })
        val shared = getGroup(NameDesc("Shared Settings"), list)
        shared.add(
            FileInput(NameDesc("Kotlinc Folder"), style, kotlinc, emptyList(), true)
                .addChangeListener { kotlinc = it })
        // platforms
        val platforms = getGroup(NameDesc("Platforms"), list)
        val linux = getGroup(NameDesc("Linux"), platforms)
        linux.add(
            BooleanInput(NameDesc("x64"), linuxPlatforms.x64, true, style)
                .setChangeListener { linuxPlatforms.x64 = it })
        linux.add(
            BooleanInput(NameDesc("arm64"), linuxPlatforms.arm64, true, style)
                .setChangeListener { linuxPlatforms.arm64 = it })
        linux.add(
            BooleanInput(NameDesc("arm32"), linuxPlatforms.arm32, false, style)
                .setChangeListener { linuxPlatforms.arm32 = it })
        val windows = getGroup(NameDesc("Windows"), platforms)
        windows.add(
            BooleanInput(NameDesc("x64 (64-bit)"), windowsPlatforms.x64, true, style)
                .setChangeListener { windowsPlatforms.x64 = it })
        windows.add(
            BooleanInput(NameDesc("x86 (32-bit)"), windowsPlatforms.x86, false, style)
                .setChangeListener { windowsPlatforms.x86 = it })
        windows.add(
            BooleanInput(NameDesc("arm64"), windowsPlatforms.arm64, true, style)
                .setChangeListener { windowsPlatforms.arm64 = it })
        windows.add(
            FileInput(NameDesc("Exe-Base-Location"), style, windowsPlatforms.exeBaseLocation, emptyList())
                .addChangeListener { windowsPlatforms.exeBaseLocation = it }
                .setTooltip("When exporting as an .exe-file, use this file as the base (.jar-contents just get appended to it)"))
        val macos = getGroup(NameDesc("MacOS"), platforms)
        macos.add(
            BooleanInput(NameDesc("x64 (Intel)"), macosPlatforms.x64, true, style)
                .setChangeListener { macosPlatforms.x64 = it })
        macos.add(
            BooleanInput(NameDesc("arm64 (M-Series)"), macosPlatforms.arm64, true, style)
                .setChangeListener { macosPlatforms.arm64 = it })

        // modules
        val logicSources = getGroup(NameDesc("Project Roots"), list)
        logicSources.add(
            object : ArrayPanel<FileReference, Panel>(NameDesc("IntellijIdea Projects"), "", { InvalidRef }, style) {
                override fun createPanel(value: FileReference): Panel {
                    return FileInput(NameDesc("Project Root"), style, value, emptyList(), true)
                        .addChangeListener { ref -> set(this, ref) }
                }

                override fun onChange() {
                    projectRoots.clear()
                    projectRoots.addAll(values)
                    refresh()
                }
            }.apply {
                setValues(projectRoots)
            }
        )

        // todo show a warning when a dependency isn't fulfilled
        val modules = getGroup(NameDesc("Included Modules"), list)

        projectRoots.mapCallback({ _, dir, cb ->
            IdeaProject.loadModules(dir, cb)
        }, { files: List<List<FileReference>>?, err ->

            val moduleList = (files ?: emptyList())
                .flatten().distinct()
                .sortedBy { it.name }

            for (file in moduleList) {
                val name = file.nameWithoutExtension
                val checkbox = BooleanInput(NameDesc(name), name !in excludedModules, false, style)
                checkbox.setChangeListener { included ->
                    if (included) excludedModules.remove(name)
                    else excludedModules.add(name)
                }
                checkbox.setTooltip(file.toLocalPath())
                modules.add(checkbox)
                // load module info for ttt
                Threads.start("extInfo(${file.nameWithoutExtension})") {
                    val extInfoTxt = file.getSibling("src").listChildren()
                        .firstOrNull { it.name.endsWith("-ext.info") }
                    val info = if (extInfoTxt != null) {
                        ExtensionInfo().loadFromTxt(extInfoTxt)
                    } else null
                    checkbox.setTooltip(
                        if (info?.description.isNullOrBlank()) file.toLocalPath()
                        else "${info.description}\n${file.toLocalPath()}"
                    )
                }
            }
        })

        val opt = getGroup(NameDesc("Space Optimization"), list)
        opt.add(
            BooleanInput(NameDesc("Minimal UI"), minimalUI, false, style)
                .setChangeListener { minimalUI = it })
        opt.add(
            BooleanInput(
                NameDesc("Kotlyn Reflect", "Minimized Kotlin reflections to reduce export size", ""),
                useKotlynReflect, false, style
            ).setChangeListener { useKotlynReflect = it })

        // assets
        val assets = getGroup(NameDesc("Assets"), list)

        // todo inputs for all settings
    }
}