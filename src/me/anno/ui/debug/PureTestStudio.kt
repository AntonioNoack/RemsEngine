package me.anno.ui.debug

import me.anno.gpu.GFX
import me.anno.ui.Panel
import me.anno.ui.Window

/**
 * engine runtime for testing; without console
 * */
@Suppress("unused")
class PureTestStudio(title: String, val createMainPanel1: () -> Panel) : TestStudio(title, { emptyList() }) {

    override fun createUI() {
        val ui = createMainPanel1()
        ui.weight = 1f
        val windowStack = GFX.someWindow!!.windowStack
        val window = Window(ui, false, windowStack)
        window.drawDirectly = true
        windowStack.add(window)
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