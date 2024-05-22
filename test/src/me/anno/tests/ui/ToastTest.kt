package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    testUI3("Toast", TextButton("Click Me!", style)
        .addLeftClickListener { msg(NameDesc("Hello World!")) })
}