package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.engine.Events.addEvent
import me.anno.engine.WindowRenderFlags.enableVSync
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.language.translation.NameDesc
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.SettingCategory
import me.anno.utils.Color.black
import kotlin.random.Random

fun main() {
    // test whether this enough to cause layout issues -> yes
    // todo why does stuff stay invisible when "Child 1" is toggled repeatedly???
    // disableRenderDoc()
    val ui = SettingCategory(NameDesc("Root"), style)
        .showByDefault()

    val rnd = Random(1324)
    fun createGroup() {
        val ui1 = SettingCategory(NameDesc("Child ${ui.content.children.size + 1}"), style)
            .showByDefault()
        val tp = TextPanel("Test ${ui.content.children.size + 1}", style)
        tp.backgroundColor = rnd.nextInt() or black
        ui1.content.add(tp)
        ui.content.add(ui1)
    }
    createGroup()
    createGroup()
    createGroup()

    // redraws are too fast to see...
    ui.addChild(SpyPanel { Thread.sleep(50) })

    addEvent {
        enableVSync = true
        showFPS = true
    }

    testUI3("ListOfSettings", ui)
}