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
class TestStudio(title: String, val createMainPanel: () -> List<Panel>) : StudioBase(title, "Test", 1, true) {

    override fun createUI() {
        val ui = PanelListY(style)
        ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style))
        ui.addAll(createMainPanel())
        ui.weight = 1f
        val windowStack = GFX.someWindow!!.windowStack
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

    companion object {

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(title: String, createMainPanel: () -> Panel) {
            TestStudio(title) { listOf(createMainPanel()) }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(title: String, mainPanel: Panel) {
            TestStudio(title) { listOf(mainPanel) }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(title: String, mainPanels: List<Panel>) {
            TestStudio(title) { mainPanels }.run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI2(title: String, createMainPanel: () -> List<Panel>) {
            TestStudio(title, createMainPanel).run()
        }

        /**
         * like testUI, just with automatic weight of 1
         * */
        fun testUI3(title: String, createMainPanel: () -> Panel) {
            TestStudio(title) {
                val p = createMainPanel()
                p.weight = 1f
                listOf(p)
            }.run()
        }

        /**
         * like testUI, just with automatic weight of 1
         * */
        fun testUI3(title: String, mainPanel: Panel) {
            TestStudio(title) {
                mainPanel.weight = 1f
                listOf(mainPanel)
            }.run()
        }

    }
}