package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.files.FileReference
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.TreeView
import me.anno.utils.assertions.assertFail

class Element {

    var name = "Element ${ctr++}"
    var parent: Element? = null
    val children = ArrayList<Element>()

    fun add(element: Element) {
        if (element == this) assertFail("Cannot append element to itself")
        element.parent?.children?.remove(element)
        children.add(element)
        element.parent = this
    }

    fun add(index: Int, element: Element) {
        if (index < 0 || index > children.size) throw IllegalArgumentException("Cannot insert at $index")
        val prevParent = element.parent
        if (prevParent === this) {
            throw IllegalStateException("Cannot add element to same parent twice")
        } else {
            prevParent?.children?.remove(element)
            children.add(index, element)
            element.parent = this
        }
    }

    override fun toString() = name

    companion object {
        var ctr = 0
    }
}

class TestTreeView(val roots: List<Element>, val selected: MutableCollection<Element>) :
    TreeView<Element>(object : FileContentImporter<Element>() {

        override fun setName(element: Element, name: String) {
            element.name = name
        }

        override fun import(
            parent: Element?,
            file: FileReference,
            useSoftLink: SoftLinkMode,
            doSelect: Boolean,
            depth: Int,
            callback: (Element) -> Unit
        ) {
            throw NotImplementedError()
        }

        override fun createNode(parent: Element?): Element {
            val child = Element()
            parent?.add(child)
            return child
        }
    }, true, style) {

    override fun listRoots() = roots

    override fun selectElements(elements: List<Element>) {
        selected += elements
    }

    override fun openAddMenu(parent: Element) {
        parent.add(Element())
    }

    override fun onDeleteKey(x: Float, y: Float) {
        onGotAction(x, y, 0f, 0f, "Delete", false)
    }

    override fun getSymbol(element: Element) = "X"
    override fun getTooltipText(element: Element) = null
    override fun getChildren(element: Element) = element.children
    override fun isCollapsed(element: Element) = false
    override fun getName(element: Element) = element.name
    override fun getParent(element: Element) = element.parent

    override fun setCollapsed(element: Element, collapsed: Boolean) {}

    override fun addChild(element: Element, child: Any, type: Char, index: Int): Boolean {
        element.add(index, child as Element)
        return true
    }

    override fun removeChild(parent: Element, child: Element) {
        parent.children.remove(child)
        child.parent = null
    }

    override fun removeRoot(root: Element) {}

    override fun destroy(element: Element) {
        val parent = element.parent ?: return
        removeChild(parent, element)
    }

    override fun setName(element: Element, name: String) {
        element.name = name
    }

    override fun stringifyForCopy(element: Element): String {
        return element.name
    }

    override fun canBeRemoved(element: Element) = getParent(element) != null
    override fun canBeInserted(parent: Element, element: Element, index: Int) = true
    override fun getDragType(element: Element) = "S"
    override fun isValidElement(element: Any?) = element is Element
}

fun main() {

    val root = Element()
    val selected = HashSet<Element>()

    disableRenderDoc()
    testUI3("Tree View") {
        TestTreeView(listOf(root), selected)
    }
}