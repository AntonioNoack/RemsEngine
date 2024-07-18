package me.anno.tests.bugs.done

import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.tests.ui.Element
import me.anno.tests.ui.TestTreeView
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    // fixed: open scene with deep entries in TreeView,
    //  some items don't have enough renderspace, why ever... why???
    //  -> it was a bug in PanelListX and PanelListY, where remainingW/H forgot padding.left/top

    val root = Element()
    var element = root
    for (i in 0 until 20) {
        val child = Element()
        element.add(child)
        element = child
    }

    val selected = HashSet<Element>()

    disableRenderDoc()
    testUI3("Tree View") {
        TestTreeView(listOf(root), selected)
    }
}