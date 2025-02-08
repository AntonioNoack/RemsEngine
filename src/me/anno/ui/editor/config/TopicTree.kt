package me.anno.ui.editor.config

import me.anno.config.DefaultStyle.iconGray
import me.anno.ui.Style
import me.anno.ui.editor.treeView.TreeView

class TopicTree(val configPanel: ConfigPanel, style: Style) :
    TreeView<TopicNode>(null, true, style) {

    init {
        // we already have a search panel
        searchPanel.hide()
    }

    override fun listRoots(): List<TopicNode> = configPanel.rootNode.children
    override fun isValidElement(element: Any?): Boolean = element is TopicNode

    override fun getDragType(element: TopicNode): String = ""
    override fun canBeInserted(parent: TopicNode, element: TopicNode, index: Int): Boolean = false
    override fun canBeRemoved(element: TopicNode): Boolean = false

    override fun stringifyForCopy(element: TopicNode): String = element.key
    override fun getName(element: TopicNode): String = element.title
    override fun setName(element: TopicNode, name: String) {}
    override fun destroy(element: TopicNode) {}
    override fun getParent(element: TopicNode): TopicNode? = element.parent
    override fun removeRoot(root: TopicNode) {}
    override fun removeChild(parent: TopicNode, child: TopicNode) {}
    override fun addChild(element: TopicNode, child: Any, type: Char, index: Int): Boolean = false

    private val textColor = style.getColor("textColor", iconGray)

    override fun getLocalColor(element: TopicNode, isHovered: Boolean, isInFocus: Boolean) = textColor
    override fun setCollapsed(element: TopicNode, collapsed: Boolean) {
        element.isCollapsed = collapsed
    }

    override fun isCollapsed(element: TopicNode): Boolean = element.isCollapsed
    override fun getChildren(element: TopicNode): List<TopicNode> = element.children

    override fun openAddMenu(parent: TopicNode) {}
    override fun selectElements(elements: List<TopicNode>) {
        configPanel.buildSettingsForTopic(elements.firstOrNull())
    }
}