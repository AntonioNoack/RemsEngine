package me.anno.ui.editor

import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.GFX
import me.anno.ui.Window
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.io.config.ConfigBasics
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.Camera
import me.anno.studio.Projects.getRecentProjects
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.instance
import me.anno.studio.rems.ProjectSettings
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.RemsStudio.versionName
import me.anno.studio.rems.RenderSettings
import me.anno.studio.rems.Rendering.overrideAudio
import me.anno.studio.rems.Rendering.renderAudio
import me.anno.studio.rems.Rendering.renderPart
import me.anno.studio.rems.Rendering.renderSetPercent
import me.anno.studio.rems.Selection
import me.anno.studio.rems.Selection.selectTransform
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.studio.rems.ui.StudioFileExplorer
import me.anno.studio.rems.ui.StudioTreeView
import me.anno.studio.rems.ui.StudioTreeView.Companion.openAddMenu
import me.anno.studio.rems.ui.StudioUITypeLibrary
import me.anno.ui.Panel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenuByPanels
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.ConsoleOutputPanel.Companion.createConsoleWithStats
import me.anno.ui.editor.config.ConfigPanel
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.style.Style
import me.anno.utils.files.OpenInBrowser.openInBrowser
import me.anno.utils.types.Lists.firstInstanceOrNull
import org.apache.logging.log4j.LogManager
import java.net.URL

object RemsStudioUILayouts {

    private val windowStack get() = StudioBase.defaultWindowStack!!

    private val LOGGER = LogManager.getLogger(RemsStudioUILayouts::class)

    fun createReloadWindow(panel: Panel, fullscreen: Boolean, reload: () -> Unit): Window {
        return object : Window(
            panel, fullscreen, windowStack,
            if (fullscreen) 0 else mouseX.toInt(),
            if (fullscreen) 0 else mouseY.toInt()
        ) {
            override fun destroy() {
                reload()
            }
        }
    }

    fun createEditorUI(welcomeUI: WelcomeUI, loadUI: Boolean = true) {

        val style = DefaultConfig.style

        val ui = PanelListY(style)

        val options = OptionBar(style)

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
            val window = createReloadWindow(panel, true) { createEditorUI(welcomeUI) }
            panel.create()
            windowStack.push(window)
        }

        options.addAction(configTitle, Dict["Style", "ui.top.config.style"]) {
            val panel = ConfigPanel(DefaultConfig.style.values, true, style)
            val window = createReloadWindow(panel, true) { createEditorUI(welcomeUI) }
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
            Dict["Change Language", ""]
        ) {
            val panel = ProjectSettings.createSpellcheckingPanel(style)
            openMenuByPanels(windowStack, NameDesc("Change Project Language"), listOf(panel))
        }

        options.addAction(projectTitle, Dict["Save", "ui.top.project.save"]) {
            instance?.save()
            LOGGER.info("Saved the project")
        }

        options.addAction(projectTitle, Dict["Load", "ui.top.project.load"]) {
            val name = NameDesc("Load Project", "", "ui.loadProject")
            val openRecentProject = welcomeUI.createRecentProjectsUI(RemsStudio, menuStyle, getRecentProjects())
            val createNewProject = welcomeUI.createNewProjectUI(RemsStudio, menuStyle)
            openMenuByPanels(windowStack, name, listOf(openRecentProject, createNewProject))
        }

        options.addAction(projectTitle, Dict["Reset UI", "ui.top.resetUI"]) {
            ask(windowStack, NameDesc("Are you sure?", "", "")) {
                project?.apply {
                    resetUIToDefault()
                    createEditorUI(welcomeUI, false)
                }
            }
        }

        options.addAction(selectTitle, "Inspector Camera") { selectTransform(nullCamera) }
        options.addAction(selectTitle, "Root") { selectTransform(root) }
        options.addAction(selectTitle, "First Camera") {
            selectTransform(root.listOfAll.firstInstanceOrNull<Camera>())
        }

        options.addAction(debugTitle, "Reload Cache (Ctrl+F5)") { CacheSection.clearAll() }
        options.addAction(debugTitle, "Clear Cache") { ConfigBasics.cacheFolder.deleteRecursively() }
        options.addAction(debugTitle, "Reload Plugins") { ExtensionLoader.reloadPlugins() }
        // todo overview to show plugins & mods
        // todo marketplace for plugins & mods?
        // ...

        // todo shortcuts, which can be set for all actions??...

        val callback = { GFX.requestAttentionMaybe() }
        options.addAction(renderTitle, "Settings") { selectTransform(RenderSettings) }
        options.addAction(renderTitle, "Set%") { renderSetPercent(true, callback) }
        options.addAction(renderTitle, "Full") { renderPart(1, true, callback) }
        options.addAction(renderTitle, "Half") { renderPart(2, true, callback) }
        options.addAction(renderTitle, "Quarter") { renderPart(4, true, callback) }
        options.addAction(renderTitle, "Override Audio") { overrideAudio(InvalidRef, true, callback) }
        options.addAction(renderTitle, "Audio") { renderAudio(true, callback) }

        options.addAction(helpTitle, "Tutorials") {
            URL("https://remsstudio.phychi.com/?s=learn").openInBrowser()
        }
        options.addAction(helpTitle, "Version: $versionName") {}
        options.addAction(helpTitle, "About") {
            // to do more info
            msg(
                windowStack,
                NameDesc("Rem's Studio is created by Antonio Noack from Jena, Germany", "", "")
            )
            // e.g. the info, why I created it
            // that it is Open Source
        }

        ui += options
        ui += SceneTabs
        ui += SpacerPanel(0, 1, style)

        val project = project!!
        if (loadUI) project.loadUI()

        ui += project.mainUI
        ui += SpacerPanel(0, 1, style)
        ui += createConsoleWithStats(true, style)

        windowStack.clear()
        windowStack.push(ui)

    }

    fun createDefaultMainUI(style: Style): Panel {

        val customUI = CustomList(true, style)
        customUI.setWeight(10f)

        val animationWindow = CustomList(false, style)
        customUI.add(animationWindow, 2f)

        val library = StudioUITypeLibrary()

        val treeFiles = CustomList(true, style)
        treeFiles += CustomContainer(StudioTreeView(style), library, style)
        treeFiles += CustomContainer(StudioFileExplorer(project?.scenes, style), library, style)
        animationWindow.add(CustomContainer(treeFiles, library, style), 0.5f)
        animationWindow.add(CustomContainer(SceneView(style), library, style), 2f)
        animationWindow.add(
            CustomContainer(
                PropertyInspector({ Selection.selectedInspectable }, style, Unit),
                library, style
            ), 0.5f
        )
        animationWindow.setWeight(1f)

        val timeline = GraphEditor(style)
        customUI.add(CustomContainer(timeline, library, style), 0.25f)

        val linear = CuttingView(style)
        customUI.add(CustomContainer(linear, library, style), 0.25f)

        return customUI

    }

    /**
     * prints the layout for UI debugging
     * */
    fun printLayout() {
        LOGGER.info("Layout:")
        for (window1 in windowStack) {
            window1.panel.printLayout(1)
        }
    }

}