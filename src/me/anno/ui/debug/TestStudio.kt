package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.Window
import me.anno.input.ActionManager
import me.anno.studio.StudioBase
import me.anno.studio.rems.StudioActions
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack

class TestStudio(val createMainPanel: () -> Panel) : StudioBase(true, "Test", 1) {

    override fun createUI() {
        val ui = PanelListY(style)
        ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style)) // todo somehow missing in layout for UnityReader
        ui.add(createMainPanel())
        windowStack.add(Window(ui, windowStack))
    }

    override fun loadConfig() {
        super.loadConfig()
        StudioActions.register()
        ActionManager.init()
    }

    override fun onGameLoop(w: Int, h: Int): Boolean {
        DefaultConfig.saveMaybe("main.config")
        return super.onGameLoop(w, h)
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