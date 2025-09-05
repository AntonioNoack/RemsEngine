package me.anno.bugs.done

import me.anno.config.DefaultConfig.style
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.BooleanInput

fun main() {
    // solved bug: text panel entries aren't really redrawing themselves well...
    //  reproduce: open config, click on "Debug" in the left bar, optionally refresh using ctrl+f5
    testUI3("Text Updates Missing") {
        // can't reproduce it here...
        val ui = PanelListY(style)
        for (i in 0 until 100) {
            ui.add(BooleanInput(NameDesc("Test $i"), true, false, style))
        }
        ScrollPanelY(ui, style)
    }
}