package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.gpu.Window
import me.anno.input.ActionManager
import me.anno.studio.StudioBase
import me.anno.studio.rems.StudioActions
import me.anno.ui.base.Panel

class TestStudio(val createMainPanel: () -> Panel) : StudioBase(false, "Test", 1) {

    override fun createUI() {
        windowStack.add(Window(createMainPanel(), windowStack))
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