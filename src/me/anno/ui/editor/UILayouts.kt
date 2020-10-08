package me.anno.ui.editor

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFX.ask
import me.anno.gpu.GFX.openMenu
import me.anno.gpu.GFX.select
import me.anno.gpu.GFXBase0
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.objects.Text
import me.anno.objects.cache.Cache
import me.anno.objects.rendering.RenderSettings
import me.anno.studio.GFXSettings
import me.anno.studio.RemsStudio
import me.anno.studio.RemsStudio.gfxSettings
import me.anno.studio.RemsStudio.windowStack
import me.anno.studio.RemsStudio.workspace
import me.anno.studio.RemsStudio.nullCamera
import me.anno.studio.RemsStudio.project
import me.anno.studio.RemsStudio.root
import me.anno.studio.RemsStudio.targetDuration
import me.anno.studio.RemsStudio.targetFPS
import me.anno.studio.RemsStudio.targetHeight
import me.anno.studio.RemsStudio.targetOutputFile
import me.anno.studio.RemsStudio.targetWidth
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.base.*
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomListX
import me.anno.ui.custom.CustomListY
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.editor.config.ConfigPanel
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.editor.sceneView.ScenePreview
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.video.VideoAudioCreator
import me.anno.video.VideoCreator
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.attribute.GroupPrincipal
import kotlin.concurrent.thread
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.roundToInt

object UILayouts {

    private val LOGGER = LogManager.getLogger(UILayouts::class)

    fun createLoadingUI() {

        val style = DefaultConfig.style

        val ui = PanelListY(style)
        val customUI = CustomListY(style)
        customUI.setWeight(10f)

    }

    fun renderPart(size: Int) {
        render(targetWidth / size, targetHeight / size)
    }

    var isRendering = false
    fun render(width: Int, height: Int) {
        if (width % 2 != 0 || height % 2 != 0) return render(
            width / 2 * 2,
            height / 2 * 2
        )
        if(isRendering){
            openMenu("Rendering already in progress!", listOf(
                "Ok" to {}
            ))
            return
        }
        isRendering = true
        LOGGER.info("Rendering video at $width x $height")
        val tmpFile = File(
            targetOutputFile.parentFile,
            targetOutputFile.nameWithoutExtension + ".tmp." + targetOutputFile.extension
        )
        val fps = targetFPS
        val totalFrameCount = (fps * targetDuration).toInt()
        val sampleRate = 48000
        val creator = VideoAudioCreator(
            VideoCreator(
                width, height,
                targetFPS, totalFrameCount, tmpFile
            ), sampleRate, targetOutputFile
        )
        creator.onFinished = { isRendering = false }
        creator.start()
    }

    fun createWelcomeUI() {

        // manage and load recent projects
        // load recently opened parts / scenes / default scene
        // list of all known projects
        // color them depending on existence

        val dir = "directory" // vs folder ^^
        val style = DefaultConfig.style
        val welcome = PanelListY(style)
        val title = TextPanel("Rem's Studio", style)
        title.textSize *= 3
        welcome += title

        welcome += SpacePanel(0, 1, style)

        val recentProjects = SettingCategory("Recent Projects", style)
        recentProjects.show2()
        welcome += recentProjects

        fun openProject(name: String, file: File){
            thread {
                RemsStudio.loadProject(name.trim(), file)
                addEvent {
                    nullCamera.farZ.set(5000f)
                    windowStack.clear()
                    createEditorUI()
                }
                DefaultConfig.addToRecentProjects(project!!)
            }
        }

        for (project in DefaultConfig.getRecentProjects()) {
            val tp = TextPanel(project.name, style)
            tp.enableHoverColor = true
            tp.setTooltip(project.file.absolutePath)
            thread {// file search can use some time
                if(!project.file.exists()){
                    tp.textColor = 0xff0000 or black
                    tp.setTooltip("${project.file.absolutePath}, not found!")
                }
            }
            tp.setOnClickListener { _, _, button, _ ->
                fun open(){// open zip?
                    if(project.file.exists() && project.file.isDirectory){
                        openProject(project.name, project.file)
                    } else {
                        openMenu(listOf(
                            "File not found!" to {}
                        ))
                    }
                }
                when {
                    button.isLeft -> open()
                    button.isRight -> {
                        openMenu(listOf(
                            "Open" to { open() },
                            "Hide" to {
                                DefaultConfig.removeFromRecentProjects(project.file)
                                tp.visibility = Visibility.GONE
                            },
                            "Delete" to {
                                ask("Are you sure?"){
                                    DefaultConfig.removeFromRecentProjects(project.file)
                                    project.file.deleteRecursively()
                                    tp.visibility = Visibility.GONE
                                }
                            }
                        ))
                    }
                }
            }
            tp.padding.top--
            tp.padding.bottom--
            recentProjects += tp
        }

        welcome += SpacePanel(0, 1, style)

        val newProject = SettingCategory("New Project", style)
        newProject.show2()
        welcome += newProject

        val nameInput = TextInput("Title", style, "New Project")
        var lastName = nameInput.text

        val fileInput = FileInput("Project Location", style, File(workspace, nameInput.text))

        var usableFile: File? = null

        fun updateFileInputColor() {
            fun rootIsOk(file: File): Boolean {
                if (file.exists()) return true
                return rootIsOk(file.parentFile ?: return false)
            }
            var invalidName = ""
            fun fileNameIsOk(file: File): Boolean {
                if(file.name.isEmpty() && file.parentFile == null) return true // root drive
                if(file.name.toAllowedFilename() != file.name){
                    invalidName = file.name
                    return false
                }
                return fileNameIsOk(file.parentFile ?: return true)
            }

            // todo check if all file name parts are valid...
            // todo check if we have write and read access
            val file = File(fileInput.text)
            var state = 0
            var msg = ""
            when {
                !rootIsOk(file) -> {
                    state = -2
                    msg = "Root $dir does not exist!"
                }
                !file.parentFile.exists() -> {
                    state = -1
                    msg = "Parent $dir does not exist!"
                }
                !fileNameIsOk(file) -> {
                    state = -2
                    msg = "Invalid file name \"$invalidName\""
                }
                file.exists() && file.list()?.isNotEmpty() == true -> {
                    state = -1
                    msg = "Folder is not empty!"
                }
            }
            fileInput.tooltip = msg
            val base = fileInput.base
            base.textColor = when (state) {
                -1 -> 0xffff00
                -2 -> 0xff0000
                else -> 0x00ff00
            } or black
            usableFile = if(state == -2){
                null
            } else file
            base.focusTextColor = base.textColor
        }

        updateFileInputColor()

        nameInput.setChangeListener {
            val newName = if(it.isBlank()) "-" else it.trim()
            if (lastName == fileInput.file.name) {
                fileInput.setText(File(fileInput.file.parentFile, newName).toString(), false)
                updateFileInputColor()
            }
            lastName = newName
        }
        newProject += nameInput

        fileInput.setChangeListener {
            updateFileInputColor()
        }
        newProject += fileInput

        fun loadNewProject() {
            val file = usableFile
            if(file != null){
                openProject(nameInput.text, file)
            } else {
                openMenu("Please choose a $dir!", listOf(
                    "Ok" to {}
                ))
            }
        }

        val button = ButtonPanel("Create Project", style)
        button.setSimpleClickListener {
            loadNewProject()
        }
        newProject += button

        welcome += SpacePanel(0, 1, style)

        val quickSettings = SettingCategory("Quick Settings", style)
        quickSettings.show2()
        welcome += quickSettings

        quickSettings += EnumInput("GFX Quality", true,
            gfxSettings.displayName, GFXSettings.values().map { it.displayName }, style)
            .setChangeListener { _, index, _ ->
                val value = GFXSettings.values()[index]
                gfxSettings = value
            }
            .setTooltip("Disables MSAA")
        quickSettings += BooleanInput("Enable Vsync", GFXBase0.enableVsync, style)
            .setChangeListener {
                DefaultConfig["editor.vsync"] = it
                GFXBase0.setVsyncEnabled(it)
            }
            .setTooltip("Recommended: true, false only for debugging")
        quickSettings += BooleanInput("Show FPS", RemsStudio.showFPS, style)
            .setChangeListener { DefaultConfig["debug.fps.show"] = it }
            .setTooltip("For debugging / monitoring stutters")

        val scroll = ScrollPanelY(welcome, Padding(5), style)
        scroll += WrapAlign.Center

        val background = ScenePreview(style)

        val background2 = PanelListX(style)
        background2.backgroundColor = 0x77777777

        // todo some kind of colored border?
        windowStack.push(Window(background))
        windowStack.push(Window(background2, false, 0, 0))
        val mainWindow = Window(scroll, false, 0, 0)
        mainWindow.acceptsClickAway = {
            if (it.isLeft) {
                loadNewProject()
                usableFile != null
            } else false
        }
        windowStack.push(mainWindow)

        root.children.clear()
        Text("Rem's Studio", root).apply {
            blockAlignmentX = AxisAlignment.CENTER
            blockAlignmentY = AxisAlignment.CENTER
            textAlignment = AxisAlignment.CENTER
            relativeCharSpacing = 0.12f
            invalidate()
        }

        nullCamera.farZ.set(100f)

    }

    fun createEditorUI() {

        val style = DefaultConfig.style

        val ui = PanelListY(style)

        // todo show the file location up there, too?
        // todo fully customizable content
        val options = OptionBar(style)
        // options.addMajor("File")
        // options.addMajor("Edit")
        // options.addMajor("View")
        // options.addMajor("Navigate")
        // options.addMajor("Code")

        options.addAction("File", "Settings") {
            val panel = ConfigPanel(DefaultConfig, style)
            val window = Window(panel)
            panel.create()
            windowStack.push(window)
        }

        options.addAction("File", "Style") {
            val panel = ConfigPanel(DefaultConfig.style.values, style)
            val window = Window(panel)
            panel.create()
            windowStack.push(window)
        }

        options.addAction("File", "Save") { Input.save() }
        options.addAction("File", "Load") { }

        options.addAction("Select", "Render Settings") { select(RenderSettings) }
        options.addAction("Select", "Inspector Camera") { select(nullCamera) }
        options.addAction("Debug", "Refresh (Ctrl+F5)") { Cache.clear() }

        options.addAction("Render", "Set%") {
            render(
                max(2, (project!!.targetWidth * project!!.targetSizePercentage / 100).roundToInt()),
                max(2, (project!!.targetHeight * project!!.targetSizePercentage / 100).roundToInt())
            )
        }
        options.addAction("Render", "Full") { renderPart(1) }
        options.addAction("Render", "Half") { renderPart(2) }
        options.addAction("Render", "Quarter") { renderPart(4) }

        ui += options
        ui += SceneTabs
        ui += SpacePanel(0, 1, style)

        val project = project!!
        project.loadUI()

        ui += project.mainUI as Panel

        ui += SpacePanel(0, 1, style)


        // console.visibility = Visibility.GONE

        ui += StudioBase.instance.createConsole()

        windowStack.clear()
        windowStack += Window(ui)

    }

    fun createDefaultMainUI(style: Style): Panel {

        val customUI = CustomListY(style)
        customUI.setWeight(10f)

        val animationWindow = CustomListX(style)
        customUI.add(animationWindow, 2f)

        val treeFiles = CustomListY(style)
        treeFiles += CustomContainer(TreeView(style), style)
        treeFiles += CustomContainer(FileExplorer(style), style)
        animationWindow.add(CustomContainer(treeFiles, style), 0.5f)
        animationWindow.add(CustomContainer(SceneView(style), style), 2f)
        animationWindow.add(CustomContainer(PropertyInspector(style), style), 0.5f)
        animationWindow.setWeight(1f)

        val timeline = GraphEditor(style)
        customUI.add(CustomContainer(timeline, style), 0.5f)

        val linear = CuttingView(style)
        customUI.add(CustomContainer(linear, style), 0.5f)

        return customUI

    }

    fun printLayout() {
        println("Layout:")
        for (window1 in GFX.windowStack) {
            window1.panel.printLayout(1)
        }
    }

}