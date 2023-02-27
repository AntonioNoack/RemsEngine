package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.documents

fun main() {
    // todo file explorer's scrolling is lagging every once in a while;
    //  - fps stays perfectly stable
    //  - panel reacts to hover events, responsive
    //  - children just are not layout correctly
    // -> effect is not appearing in pure file explorer :/
    testUI3 { FileExplorer(documents, style) }
}