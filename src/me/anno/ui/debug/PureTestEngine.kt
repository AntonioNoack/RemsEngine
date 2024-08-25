package me.anno.ui.debug

import me.anno.gpu.GFX
import me.anno.ui.Panel
import me.anno.ui.Window

/**
 * engine runtime for testing; without console
 * */
@Suppress("unused")
class PureTestEngine(title: String, val createMainPanel1: () -> Panel) : TestEngine(title, { emptyList() }) {

    override fun createUI() {
        val ui = createMainPanel1()
        ui.weight = 1f
        GFX.someWindow.windowStack
            .push(ui)
            .drawDirectly = true
    }

    companion object {

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testPureUI(title: String, createMainPanel: () -> Panel) {
            PureTestEngine(title, createMainPanel).run()
        }

        /**
         * create a very simple instance of the editor,
         * with a single panel; full gfx capabilities,
         * no audio
         * */
        fun testPureUI(title: String, mainPanel: Panel) {
            PureTestEngine(title) { mainPanel }.run()
        }
    }
}