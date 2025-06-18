package me.anno.ui.editor

import me.anno.cache.AsyncCacheData
import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase
import me.anno.engine.Events.addEvent
import me.anno.engine.GFXSettings
import me.anno.engine.WindowRenderFlags
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.engine.projects.GameEngineProject
import me.anno.engine.projects.ProjectHeader
import me.anno.engine.projects.Projects
import me.anno.gpu.GFX
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.language.translation.NameDesc.Companion.translate
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.SizeLimitingContainer
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.editor.files.FileExplorerOptions.openInExplorerDesc
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.TextInput
import me.anno.utils.Color.black
import me.anno.utils.GFXFeatures
import me.anno.utils.async.Callback
import me.anno.utils.files.OpenFileExternally.openInExplorer
import me.anno.utils.types.Strings.isBlank2
import kotlin.concurrent.thread

interface WelcomeUI {

    fun createBackground(style: Style): Panel {
        return Panel(style).apply { background.color = black }
    }

    fun create(studio: EngineBase) {

        val window = GFX.someWindow
        val windowStack = window.windowStack

        // manage and load recent projects
        // load recently opened parts / scenes / default scene
        // list of all known projects
        // color them depending on existence

        val style = DefaultConfig.style
        val welcome = PanelListY(style)

        welcome += TextPanel(studio.nameDesc, style).apply { font = font.withSize(font.size * 3f) }
        welcome += TextPanel(
            NameDesc("Version %1", "Which version is running in this window", "ui.welcome.version")
                .with("%1", studio.versionName), style
        ).apply {
            textColor = textColor and 0x7fffffff
            disableFocusColors()
        }

        welcome += SpacerPanel(0, 1, style)

        val recent = Projects.getRecentProjects()

        welcome += createRecentProjectsUI(studio, style, recent)
        welcome += SpacerPanel(0, 1, style)

        welcome += createNewProjectUI(studio, style)
        welcome += SpacerPanel(0, 1, style)

        val quickSettings = SettingCategory(
            NameDesc(
                "Quick Settings", "",
                "ui.welcome.quickSettings.title"
            ), style
        )
        quickSettings.showByDefault()
        welcome += quickSettings

        quickSettings += Dict.selectLanguage(style) {
            create(studio)
        }

        val gfxNames = GFXSettings.entries.map { it.nameDesc }

        quickSettings += EnumInput(
            NameDesc(
                "GFX Quality",
                "Low disables UI MSAA",
                "ui.settings.gfxQuality"
            ),
            studio.gfxSettings.nameDesc, gfxNames, style
        ).setChangeListener { _, index, _ ->
            val value = GFXSettings.entries[index]
            studio.gfxSettings = value
        }

        if (GFXFeatures.canToggleVSync) {
            quickSettings += BooleanInput(
                NameDesc(
                    "Enable Vsync",
                    "Recommended; false for debugging",
                    "ui.settings.vSync"
                ),
                WindowRenderFlags.enableVSync, true, style
            ).setChangeListener { enableVSync ->
                WindowRenderFlags.enableVSync = enableVSync
                window.setVsyncEnabled(enableVSync) // todo do we need to call this???
            }
        } // else we cannot set vsync

        quickSettings += BooleanInput(
            NameDesc(
                "Show FPS",
                "Shows how many frames were rendered per second, for monitoring stutters",
                "ui.settings.showFPS"
            ),
            showFPS, false, style
        ).setChangeListener { showFPS = it }

        val fontSize = style.getSize("fontSize", 15)
        val cop = ConsoleOutputPanel.createConsoleWithStats(true, style)
        val slc = SizeLimitingContainer(cop, fontSize * 25, -1, style)
        slc.padding.top = fontSize / 2
        welcome += slc

        val scroll = ScrollPanelY(welcome, Padding(5), style)
        scroll.alignmentX = AxisAlignment.CENTER
        scroll.alignmentY = AxisAlignment.CENTER

        windowStack.push(createBackground(style))

        val mainWindow = Window(scroll, isTransparent = true, isFullscreen = false, windowStack, 0, 0)
        mainWindow.acceptsClickAway = {
            if (it == Key.BUTTON_LEFT) {
                loadLastProject(studio, usableFile, nameInput, recent)
                usableFile != null
            } else false
        }

        windowStack.push(mainWindow)
    }

    fun createRecentProjectsUI(studio: EngineBase, style: Style, recent: List<ProjectHeader>): Panel {

        val window = GFX.someWindow
        val recentProjects = SettingCategory(
            NameDesc(
                "Recent Projects", "Your projects of the past",
                "ui.recentProjects.title"
            ), true, style
        )
        recentProjects.showByDefault()

        registerCustomClass(GameEngineProject::class)
        for (project in recent) {
            val tp = TextPanel(project.name, style)
            tp.enableHoverColor = true
            tp.tooltip = project.file.absolutePath

            if (!project.file.exists) {
                tp.textColor = 0xff0000 or black
                tp.tooltip = Dict["%1, not found!", "ui.recentProjects.projectNotFound"].replace(
                    "%1",
                    project.file.absolutePath
                )
            }

            fun open() {// open zip?
                if (project.file.exists && project.file.isDirectory) {
                    openProject(studio, project.name, project.file)
                } else Menu.msg(
                    window.windowStack,
                    NameDesc("File not found!", "", "ui.recentProjects.fileNotFound")
                )
            }

            tp.addLeftClickListener { open() }
            tp.addRightClickListener {
                Menu.openMenu(
                    window.windowStack, listOf(
                        MenuOption(
                            NameDesc(
                                "Open",
                                "Opens that project", "ui.recentProjects.open"
                            )
                        ) { open() },
                        MenuOption(openInExplorerDesc) {
                            openInExplorer(project.file)
                        },
                        MenuOption(
                            NameDesc(
                                "Hide",
                                "Moves the project to the end of the list or removes it",
                                "ui.recentProjects.hide"
                            )
                        ) {
                            Projects.removeFromRecentProjects(project.file)
                            tp.isVisible = false
                        },
                        MenuOption(
                            NameDesc(
                                "Delete",
                                "Removes the project from your drive!", "ui.recentProjects.delete"
                            )
                        ) {
                            Menu.ask(window.windowStack, NameDesc("Are you sure?", "", "")) {
                                Projects.removeFromRecentProjects(project.file)
                                project.file.delete()
                                tp.isVisible = false
                            }
                        }
                    ))
            }
            tp.padding.top--
            tp.padding.bottom--
            recentProjects += tp
        }

        return recentProjects
    }

    fun loadProject(name: String, folder: FileReference, callback: Callback<ProjectHeader>)

    fun loadProjectHeader(folder: FileReference, callback: Callback<ProjectHeader>)

    fun createProjectUI()

    fun openProject(studio: EngineBase, name: String, folder: FileReference) {
        thread(name = "OpenProject") { // prevent the engine from hanging
            openProject2(name, folder)
        }
    }

    private fun openProject2(name: String, folder: FileReference) {
        loadProject(name.trim(), folder, Callback.onSuccess { (name, project) ->
            addEvent {
                GFX.someWindow.windowStack.clear()
                createProjectUI()
            }
            Projects.addToRecentProjects(name, project)
        })
    }

    fun loadLastProject(
        studio: EngineBase,
        usableFile: FileReference?, nameInput: TextInput,
        recent: List<ProjectHeader>
    ) {
        if (recent.isEmpty()) {
            loadNewProject(studio, usableFile, nameInput)
        } else {
            val project = recent.first()
            openProject(studio, project.name, project.file)
        }
    }

    fun loadNewProject(studio: EngineBase, file: FileReference?, nameInput: TextInput) {
        if (file != null) {
            openProject(studio, nameInput.value, file)
        } else {
            Menu.msg(
                NameDesc(
                    "Please choose a ${dirNameEn}!",
                    "", "ui.newProject.pleaseChooseDir"
                )
            )
        }
    }

    fun createNewProjectUI(studio: EngineBase, style: Style): Panel {

        val newProject = SettingCategory(NameDesc("New Project", "New Workplace", "ui.project.new"), style)
        newProject.showByDefault()

        val createProjectName = NameDesc("Create Project", "Creates a new project", "ui.createNewProject")
        val openProjectName = NameDesc("Open Project", "Open an existing project", "ui.openExistingProject")
        val createButton = TextButton(createProjectName, false, style)

        val nameInput = TextInput(NameDesc("Project Name"), "", Dict["New Project", "ui.newProject.defaultName"], style)
        nameInput.setEnterListener { loadNewProject(studio, usableFile, nameInput) }
        Companion.nameInput = nameInput

        var lastName = nameInput.value

        // cannot be moved down
        val fileInput = FileInput(
            NameDesc("Project Location", "", "ui.newProject.location"), style,
            EngineBase.workspace.getChild(lastName), emptyList(),
            true
        )

        fun updateFileInputColor() {

            var invalidName = ""

            fun fileNameIsOk(file: FileReference): Boolean {
                val parent = file.getParent()
                val parentIsRoot = parent == FileRootRef
                if (parentIsRoot) {
                    if (file.name.isEmpty()) return true // root drive
                    if (file.name.length == 2 && file.name[0].isLetter() && file.name[1] == ':') return true // windows drive letter
                }
                if (file.name.toAllowedFilename() != file.name) {
                    invalidName = file.name
                    return false
                }
                return fileNameIsOk(parent)
            }

            val file = fileInput.value
            var state = "ok"
            var msg = ""
            var alreadyExists = false
            when {
                !rootIsOk(file) -> {
                    state = "error"
                    msg = translate("Root $dirNameEn does not exist!", "ui.project.rootMissing")
                }
                !file.getParent().exists -> {
                    state = "warning"
                    msg = translate("Parent $dirNameEn does not exist!", "ui.project.parentMissing")
                }
                !fileNameIsOk(file) -> {
                    state = "error"
                    msg = translate("Invalid file name \"$invalidName\"", "ui.project.invalidFileName")
                }
                file.exists && file.listChildren().isNotEmpty() -> {
                    alreadyExists = true
                    state = "warning"
                    msg = translate("Folder is not empty!", "ui.project.folderNotEmpty")
                }
                else -> {}
            }
            if (alreadyExists) {
                val project = AsyncCacheData.loadSync { loadProjectHeader(file, it) }
                if (project != null) {
                    nameInput.setValue(project.name, true)
                    msg = "Project already exists"
                    state = "open"
                } else alreadyExists = false
            }

            fileInput.tooltip = msg
            fileInput.uiParent?.tooltip = msg

            // change colors
            val base = fileInput.base2
            base.textColor = when (state) {
                "warning" -> 0xffff00
                "error" -> 0xff0000
                "open" -> 0x77ff77
                else -> 0x00ff00
            } or black
            usableFile = if (state == "error") {
                null
            } else file
            base.focusTextColor = base.textColor

            // switch title of button between "Create" and "Open"
            val nameDesc =
                if (alreadyExists) openProjectName
                else createProjectName
            createButton.text = nameDesc.name
            createButton.tooltip = nameDesc.desc
        }

        updateFileInputColor()

        nameInput.addChangeListener {
            val newName = if (it.isBlank2()) "-" else it.trim()
            if (lastName == fileInput.value.name) {
                fileInput.setText(fileInput.value.getSibling(newName).toString(), false)
                updateFileInputColor()
            }
            lastName = newName
        }
        newProject += nameInput

        fileInput.addChangeListener {
            updateFileInputColor()
        }
        newProject += fileInput

        createButton.addLeftClickListener { loadNewProject(studio, usableFile, nameInput) }
        newProject += createButton

        return newProject
    }

    companion object {

        const val dirNameEn = "directory" // vs folder

        lateinit var nameInput: TextInput
        var usableFile: FileReference? = null

        fun rootIsOk(file: FileReference): Boolean {
            var fileI = file
            while (true) {
                if (fileI.exists) return true
                if (fileI == InvalidRef) return false
                fileI = fileI.getParent()
            }
        }
    }
}