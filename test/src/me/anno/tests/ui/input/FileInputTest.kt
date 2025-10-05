package me.anno.tests.ui.input

import me.anno.engine.OfficialExtensions
import me.anno.language.translation.NameDesc
import me.anno.tests.ui.UITests.Companion.style
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.FileInput
import me.anno.utils.OS.home

fun main() {
    OfficialExtensions.initForTests()
    val fileInput = FileInput(NameDesc("Source"), style, home, emptyList())
    testUI3("FileInput", fileInput)
}