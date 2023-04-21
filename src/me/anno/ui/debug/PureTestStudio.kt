package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.engine.EngineActions
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.input.ActionManager
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Window

/**
 * engine runtime for testing; without console
 * */
class PureTestStudio(val createMainPanel: () -> Panel) : StudioBase(true, "Test", 1) {

    override fun createUI() {
        val ui = createMainPanel()
        ui.weight = 1f
        val windowStack = GFX.someWindow.windowStack
        windowStack.add(Window(ui, false, windowStack))
    }

    override fun loadConfig() {
        super.loadConfig()
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
        fun testPureUI(createMainPanel: () -> Panel) {
            PureTestStudio(createMainPanel).run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testPureUI(mainPanel: Panel) {
            PureTestStudio { mainPanel }.run()
        }

    }
}