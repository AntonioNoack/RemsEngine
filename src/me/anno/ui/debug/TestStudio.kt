package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineActions
import me.anno.gpu.GFX
import me.anno.gpu.WindowX
import me.anno.input.ActionManager
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelListY

class TestStudio(val createMainPanel: () -> List<Panel>) : StudioBase(true, "Test", 1) {

    override fun createUI() {
        val ui = PanelListY(style)
        ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style))
        for (panel in createMainPanel()) {
            ui.add(panel)
        }
        ui.setWeight(1f)
        val windowStack = GFX.someWindow.windowStack
        windowStack.add(Window(ui, false, windowStack))
    }

    override fun loadConfig() {
        super.loadConfig()
        EngineActions.register()
        ActionManager.init()
    }

    override fun onGameLoop(window: WindowX, w: Int, h: Int) {
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
        fun testUI2(createMainPanel: () -> List<Panel>) {
            TestStudio(createMainPanel).run()
        }

    }
}