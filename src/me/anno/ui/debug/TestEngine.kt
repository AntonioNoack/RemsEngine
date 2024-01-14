package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineActions
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.input.ActionManager
import me.anno.language.translation.NameDesc
import me.anno.engine.EngineBase
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu

/**
 * engine runtime for testing
 * */
open class TestEngine(title: String, val createMainPanel: () -> List<Panel>) : EngineBase(title, "Test", 1, true) {

    override fun createUI() {
        val ui = PanelListY(style)
        ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style))
        ui.addAll(createMainPanel())
        ui.fill(1f)
        val windowStack = GFX.someWindow.windowStack
        val window = Window(ui, false, windowStack)
        window.drawDirectly = true
        windowStack.add(window)
    }

    override fun loadConfig() {
        super.loadConfig()
        DefaultConfig.defineDefaultFileAssociations()
        EngineActions.register()
        ActionManager.init()
    }

    override fun onGameLoop(window: OSWindow, w: Int, h: Int) {
        DefaultConfig.saveMaybe("main.config")
        super.onGameLoop(window, w, h)
    }

    override fun save() {
        try {
            ECSSceneTabs.currentTab?.save()
        } catch (e: Exception) {
            e.printStackTrace()
            Menu.msg(GFX.someWindow.windowStack, NameDesc(e.toString()))
        }
    }

    companion object {

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(title: String, createMainPanel: () -> Panel) {
            TestEngine(title) { listOf(createMainPanel()) }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(title: String, mainPanel: Panel) {
            TestEngine(title) { listOf(mainPanel) }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(title: String, mainPanels: List<Panel>) {
            TestEngine(title) { mainPanels }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI2(title: String, createMainPanel: () -> List<Panel>) {
            TestEngine(title, createMainPanel).run()
        }

        /**
         * like testUI, just with automatic weight of 1
         * */
        fun testUI3(title: String, createMainPanel: () -> Panel) {
            TestEngine(title) {
                val p = createMainPanel()
                p.fill(1f)
                listOf(p)
            }.run()
        }

        /**
         * like testUI, just with automatic weight of 1
         * */
        fun testUI3(title: String, mainPanel: Panel) {
            TestEngine(title) {
                mainPanel.fill(1f)
                listOf(mainPanel)
            }.run()
        }
    }
}