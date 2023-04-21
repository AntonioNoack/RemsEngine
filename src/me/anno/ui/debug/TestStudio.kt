package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineActions
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.input.ActionManager
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelListY

/**
 * engine runtime for testing
 * */
class TestStudio(val createMainPanel: () -> List<Panel>) : StudioBase(true, "Test", 1) {

    override fun createUI() {
        val ui = PanelListY(style)
        ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style))
        ui.addAll(createMainPanel())
        ui.weight = 1f
        val windowStack = GFX.someWindow.windowStack
        windowStack.add(Window(ui, false, windowStack))
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

    companion object {

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(createMainPanel: () -> Panel) {
            TestStudio { listOf(createMainPanel()) }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(mainPanel: Panel) {
            TestStudio { listOf(mainPanel) }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(mainPanels: List<Panel>) {
            TestStudio { mainPanels }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI2(createMainPanel: () -> List<Panel>) {
            TestStudio(createMainPanel).run()
        }

        /**
         * like testUI, just with automatic weight of 1
         * */
        fun testUI3(createMainPanel: () -> Panel) {
            TestStudio {
                val p = createMainPanel()
                p.weight = 1f
                listOf(p)
            }.run()
        }

        /**
         * like testUI, just with automatic weight of 1
         * */
        fun testUI3(mainPanel: Panel) {
            TestStudio {
                mainPanel.weight = 1f
                listOf(mainPanel)
            }.run()
        }

    }
}