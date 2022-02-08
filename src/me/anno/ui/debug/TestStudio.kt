package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineActions
import me.anno.input.ActionManager
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelListY

class TestStudio(val createMainPanel: () -> Panel) : StudioBase(true, "Test", 1) {

    override fun createUI() {
        val ui = PanelListY(style)
        // todo somehow missing in layout for UnityReader
        ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style))
        ui.add(createMainPanel())
        windowStack.add(Window(ui, windowStack))
    }

    override fun loadConfig() {
        super.loadConfig()
        EngineActions.register()
        ActionManager.init()
    }

    override fun onGameLoop(w: Int, h: Int) {
        DefaultConfig.saveMaybe("main.config")
        super.onGameLoop(w, h)
    }

    companion object {
        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testUI(createMainPanel: () -> Panel) {
            TestStudio(createMainPanel).run()
        }
    }
}