package me.anno.ui.editor

import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.getRecentProjects
import me.anno.config.DefaultStyle.black
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase0
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.io.config.ConfigBasics
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.Camera
import me.anno.objects.text.Text
import me.anno.studio.GFXSettings
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.studio.rems.ProjectSettings
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.gfxSettings
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.RemsStudio.versionName
import me.anno.studio.rems.RenderSettings
import me.anno.studio.rems.Rendering.renderAudio
import me.anno.studio.rems.Rendering.renderPart
import me.anno.studio.rems.Rendering.renderSetPercent
import me.anno.studio.rems.Selection.selectTransform
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.Menu.openMenuComplex2
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomListX
import me.anno.ui.custom.CustomListY
import me.anno.ui.debug.RuntimeInfoPanel
import me.anno.ui.editor.config.ConfigPanel
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.editor.sceneView.ScenePreview
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.editor.treeView.TreeViewPanel.Companion.openAddMenu
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.Threads.threadWithName
import me.anno.utils.files.Files.openInExplorer
import me.anno.utils.files.OpenInBrowser.openInBrowser
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

object UILayouts {

    private val windowStack get() = GFX.windowStack

    private val LOGGER = LogManager.getLogger(UILayouts::class)

    /**
     * opens a project; completely async
     * @param name: name of the project
     * @param folder: location of the project
     * */
    fun openProject(name: String, folder: File) {
        threadWithName("UILayouts::openProject()") {
            RemsStudio.loadProject(name.trim(), folder)
            addEvent {
                windowStack.clear()
                createEditorUI()
            }
            DefaultConfig.addToRecentProjects(project!!)
        }
    }

    fun createRecentProjectsUI(style: Style, recent: List<DefaultConfig.ProjectHeader>): Panel {

        val recentProjects =
            SettingCategory(
                "Recent Projects",
                "Your projects of the past",
                "ui.recentProjects.title",
                true,
                style
            )
        recentProjects.show2()

        for (project in recent) {
            val tp = TextPanel(project.name, style)
            tp.enableHoverColor = true
            tp.setTooltip(project.file.absolutePath)
            thread {// file search can use some time
                if (!project.file.exists()) {
                    tp.textColor = 0xff0000 or black
                    tp.setTooltip(
                        Dict["%1, not found!", "ui.recentProjects.projectNotFound"].replace(
                            "%1",
                            project.file.absolutePath
                        )
                    )
                }
            }
            tp.setOnClickListener { _, _, button, _ ->
                fun open() {// open zip?
                    if (project.file.exists() && project.file.isDirectory) {
                        openProject(project.name, project.file)
                    } else msg(NameDesc("File not found!", "", "ui.recentProjects.fileNotFound"))
                }
                when {
                    button.isLeft -> open()
                    button.isRight -> {
                        openMenu(listOf(
                            MenuOption(
                                NameDesc(
                                    "Open",
                                    "Opens that project", "ui.recentProjects.open"
                                )
                            ) { open() },
                            MenuOption(
                                NameDesc(
                                    "Open In Explorer",
                                    "Opens that project in the file explorer", "ui.recentProjects.openInExplorer"
                                )
                            ) { project.file.openInExplorer() },
                            MenuOption(
                                NameDesc(
                                    "Hide",
                                    "Moves the project to the end of the list or removes it",
                                    "ui.recentProjects.hide"
                                )
                            ) {
                                DefaultConfig.removeFromRecentProjects(project.file)
                                tp.visibility = Visibility.GONE
                            },
                            MenuOption(
                                NameDesc(
                                    "Delete",
                                    "Removes the project from your drive!", "ui.recentProjects.delete"
                                )
                            ) {
                                ask(NameDesc("Are you sure?", "", "")) {
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

        return recentProjects

    }

    fun loadLastProject(
        usableFile: File?, nameInput: TextInput,
        recent: List<DefaultConfig.ProjectHeader>
    ) {
        if (recent.isEmpty()) loadNewProject(usableFile, nameInput)
        else {
            val project = recent.first()
            openProject(project.name, project.file)
        }
    }

    fun loadNewProject(usableFile: File?, nameInput: TextInput) {
        val file = usableFile
        if (file != null) {
            openProject(nameInput.text, file)
        } else {
            msg(NameDesc("Please choose a $dirNameEn!", "", "ui.newProject.pleaseChooseDir"))
        }
    }

    private const val dirNameEn = "directory" // vs folder ^^
    lateinit var nameInput: TextInput
    var usableFile: File? = null

    fun createNewProjectUI(style: Style): Panel {

        val newProject = SettingCategory("New Project", "New Workplace", "ui.project.new", style)
        newProject.show2()

        // cannot be moved down
        lateinit var fileInput: FileInput

        fun updateFileInputColor() {
            fun rootIsOk(file: File): Boolean {
                if (file.exists()) return true
                return rootIsOk(file.parentFile ?: return false)
            }

            var invalidName = ""
            fun fileNameIsOk(file: File): Boolean {
                if (file.name.isEmpty() && file.parentFile == null) return true // root drive
                if (file.name.toAllowedFilename() != file.name) {
                    invalidName = file.name
                    return false
                }
                return fileNameIsOk(file.parentFile ?: return true)
            }

            // todo check if all file name parts are valid...
            // todo check if we have write and read access
            val file = fileInput.file
            var state = 0
            var msg = ""
            when {
                !rootIsOk(file) -> {
                    state = -2
                    // todo translate
                    msg = "Root $dirNameEn does not exist!"
                }
                !file.parentFile.exists() -> {
                    state = -1
                    // todo translate
                    msg = "Parent $dirNameEn does not exist!"
                }
                !fileNameIsOk(file) -> {
                    state = -2
                    // todo translate
                    msg = "Invalid file name \"$invalidName\""
                }
                file.exists() && file.list()?.isNotEmpty() == true -> {
                    state = -1
                    // todo translate
                    msg = "Folder is not empty!"
                }
            }
            fileInput.tooltip = msg
            val base = fileInput.base2
            base.textColor = when (state) {
                -1 -> 0xffff00
                -2 -> 0xff0000
                else -> 0x00ff00
            } or black
            usableFile = if (state == -2) {
                null
            } else file
            base.focusTextColor = base.textColor
        }

        nameInput = TextInput("Title", style, Dict["New Project", "ui.newProject.defaultName"])
        nameInput.setEnterListener { loadNewProject(usableFile, nameInput) }

        var lastName = nameInput.text
        fileInput =
            FileInput(Dict["Project Location", "ui.newProject.location"], style, File(workspace, nameInput.text))

        updateFileInputColor()

        nameInput.setChangeListener {
            val newName = if (it.isBlank2()) "-" else it.trim()
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

        val button = TextButton("Create Project", "Creates a new project", "ui.createNewProject", false, style)
        button.setSimpleClickListener { loadNewProject(usableFile, nameInput) }
        newProject += button

        return newProject

    }

    fun createWelcomeUI() {

        // manage and load recent projects
        // load recently opened parts / scenes / default scene
        // list of all known projects
        // color them depending on existence

        val style = DefaultConfig.style
        val welcome = PanelListY(style)

        welcome += TextPanel("Rem's Studio", style).apply { font = font.withSize(font.size * 3f) }
        welcome += TextPanel("Version $versionName", style).apply {
            textColor = textColor and 0x7fffffff
            focusTextColor = textColor
        }

        welcome += SpacePanel(0, 1, style)

        val recent = getRecentProjects()

        welcome += createRecentProjectsUI(style, recent)
        welcome += SpacePanel(0, 1, style)

        welcome += createNewProjectUI(style)
        welcome += SpacePanel(0, 1, style)

        val quickSettings = SettingCategory(Dict["Quick Settings", "ui.welcome.quickSettings.title"], style)
        quickSettings.show2()
        welcome += quickSettings

        quickSettings += Dict.selectLanguages(style) {
            createWelcomeUI()
        }

        val gfxNames = GFXSettings.values().map { it.naming }

        quickSettings += EnumInput(
            "GFX Quality",
            "Low disables UI MSAA", "ui.settings.gfxQuality",
            gfxSettings.displayName, gfxNames, style
        ).setChangeListener { _, index, _ ->
            val value = GFXSettings.values()[index]
            gfxSettings = value
        }

        quickSettings += BooleanInput(
            "Enable Vsync",
            "Recommended; false for debugging", "ui.settings.vSync",
            GFXBase0.enableVsync, style
        ).setChangeListener {
            DefaultConfig["debug.ui.enableVsync"] = it
            GFXBase0.setVsyncEnabled(it)
        }

        quickSettings += BooleanInput(
            "Show FPS",
            "Shows how many frames were rendered per second, for monitoring stutters", "ui.settings.showFPS",
            RemsStudio.showFPS,
            style
        ).setChangeListener { DefaultConfig["debug.ui.showFPS"] = it }

        val scroll = ScrollPanelY(welcome, Padding(5), style)
        scroll += WrapAlign.Center

        val background = ScenePreview(style)

        val background2 = PanelListX(style)
        background2.backgroundColor = 0x77777777

        // todo some kind of colored border?
        windowStack.push(Window(background))
        windowStack.push(Window(background2, false, 0, 0))
        val mainWindow = Window(scroll, false, 0, 0)
        mainWindow.cannotClose()
        mainWindow.acceptsClickAway = {
            if (it.isLeft) {
                loadLastProject(usableFile, nameInput, recent)
                usableFile != null
            } else false
        }
        windowStack.push(mainWindow)

        root.children.clear()
        Text("Rem's Studio", root).apply {
            blockAlignmentX.set(0f)
            blockAlignmentY.set(0f)
            textAlignment.set(0f)
            relativeCharSpacing = 0.12f
            invalidate()
        }

    }

    fun createEditorUI(loadUI: Boolean = true) {

        val style = DefaultConfig.style

        val ui = PanelListY(style)

        val options = OptionBar(style)

        fun createReloadWindow(panel: Panel, fullscreen: Boolean): Window {
            return object : Window(
                panel, fullscreen,
                if (fullscreen) 0 else mouseX.toInt(),
                if (fullscreen) 0 else mouseY.toInt()
            ) {
                override fun destroy() {
                    createEditorUI()
                }
            }
        }

        val configTitle = Dict["Config", "ui.top.config"]
        val projectTitle = Dict["Project", "ui.top.project"]
        val selectTitle = Dict["Select", "ui.top.select"]
        val debugTitle = Dict["Debug", "ui.top.debug"]
        val renderTitle = Dict["Render", "ui.top.render"]
        val helpTitle = Dict["Help", "ui.top.help"]

        options.addMajor(Dict["Add", "ui.top.add"]) {
            openAddMenu(selectedTransform ?: root)
        }

        // todo complete translation
        // todo option to save/load/restore layout
        options.addAction(configTitle, Dict["Settings", "ui.top.config.settings"]) {
            val panel = ConfigPanel(DefaultConfig, false, style)
            val window = createReloadWindow(panel, true)
            panel.create()
            windowStack.push(window)
        }

        options.addAction(configTitle, Dict["Style", "ui.top.config.style"]) {
            val panel = ConfigPanel(DefaultConfig.style.values, true, style)
            val window = createReloadWindow(panel, true)
            panel.create()
            windowStack.push(window)
        }

        options.addAction(configTitle, Dict["Language", "ui.top.config.language"]) {
            Dict.selectLanguages(style).onMouseClicked(mouseX, mouseY, MouseButton.LEFT, false)
        }

        options.addAction(configTitle, Dict["Open Config Folder", "ui.top.config.openFolder"]) {
            ConfigBasics.configFolder.openInExplorer()
        }

        val menuStyle = style.getChild("menu")

        options.addAction(
            projectTitle,
            Dict["Settings", "ui.top.project.settings"]
        ) { selectTransform(ProjectSettings) }
        options.addAction(projectTitle, Dict["Save", "ui.top.project.save"]) {
            Input.save()
            LOGGER.info("Saved the project")
        }

        options.addAction(projectTitle, Dict["Load", "ui.top.project.load"]) {
            openMenuComplex2(
                NameDesc("Load Project", "", "ui.loadProject"), listOf(
                    createRecentProjectsUI(menuStyle, getRecentProjects()),
                    createNewProjectUI(menuStyle)
                )
            )
        }

        options.addAction(projectTitle, Dict["Reset UI", "ui.top.resetUI"]) {
            ask(NameDesc("Are you sure?", "", "")) {
                project?.apply {
                    resetUIToDefault()
                    createEditorUI(false)
                }
            }
        }

        options.addAction(selectTitle, "Inspector Camera") { selectTransform(nullCamera) }
        options.addAction(selectTitle, "Root") { selectTransform(root) }
        options.addAction(selectTitle, "First Camera") {
            selectTransform(
                root.listOfAll.filterIsInstance<Camera>().firstOrNull()
            )
        }

        options.addAction(debugTitle, "Reload Cache (Ctrl+F5)") { CacheSection.clearAll() }
        options.addAction(debugTitle, "Clear Cache") { ConfigBasics.cacheFolder.deleteRecursively() }
        options.addAction(debugTitle, "Reload Plugins") { ExtensionLoader.reloadPlugins() }
        // todo overview to show plugins & mods
        // todo marketplace for plugins & mods?
        // ...

        // todo shortcuts, which can be set for all actions??...

        options.addAction(renderTitle, "Settings") { selectTransform(RenderSettings) }
        options.addAction(renderTitle, "Set%") { renderSetPercent(true) {} }
        options.addAction(renderTitle, "Full") { renderPart(1, true) {} }
        options.addAction(renderTitle, "Half") { renderPart(2, true) {} }
        options.addAction(renderTitle, "Quarter") { renderPart(4, true) {} }
        options.addAction(renderTitle, "Audio") { renderAudio(true) {} }

        options.addAction(helpTitle, "Tutorials") {
            URL("https://remsstudio.phychi.com/?s=learn").openInBrowser()
        }
        options.addAction(helpTitle, "Version: $versionName") {}
        options.addAction(helpTitle, "About") {
            // to do more info
            msg(NameDesc("Rem's Studio is created by Antonio Noack from Jena, Germany", "", ""))
            // e.g. the info, why I created it
            // that it is Open Source
        }

        ui += options
        ui += SceneTabs
        ui += SpacePanel(0, 1, style)

        val project = project!!
        if (loadUI) project.loadUI()

        ui += project.mainUI

        ui += SpacePanel(0, 1, style)

        val bottom = PanelListX(style)
        bottom += RemsStudio.createConsole().setWeight(1f)
        bottom += RuntimeInfoPanel(style)
        ui += bottom

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
        customUI.add(CustomContainer(timeline, style), 0.25f)

        val linear = CuttingView(style)
        customUI.add(CustomContainer(linear, style), 0.25f)

        return customUI

    }

    /**
     * prints the layout for UI debugging
     * */
    fun printLayout() {
        LOGGER.info("Layout:")
        for (window1 in GFX.windowStack) {
            window1.panel.printLayout(1)
        }
    }

}