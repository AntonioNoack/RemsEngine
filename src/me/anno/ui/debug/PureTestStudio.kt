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
@Suppress("unused")
class PureTestStudio(title: String, val createMainPanel: () -> Panel) : StudioBase(title, 1, true) {

    override fun createUI() {
        val ui = createMainPanel()
        ui.weight = 1f
        val windowStack = GFX.someWindow!!.windowStack
        val window = Window(ui, false, windowStack)
        window.drawDirectly = true
        windowStack.add(window)
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
        fun testPureUI(title: String, createMainPanel: () -> Panel) {
            PureTestStudio(title, createMainPanel).run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testPureUI(title: String, mainPanel: Panel) {
            PureTestStudio(title) { mainPanel }.run()
        }

    }
}