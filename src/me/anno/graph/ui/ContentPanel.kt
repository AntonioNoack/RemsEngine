package me.anno.graph.ui

import me.anno.graph.Node
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style

class ContentPanel(style: Style) : PanelGroup(style) {

    lateinit var p: GraphPanel

    override val children: List<Panel>
        get() = p.childrenMap.values.toList()

    /*init {
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.FILL
    }*/

    override fun remove(child: Panel) {}

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val cw = (p.defaultNodeSize * p.zoom).toInt()
        val doneNodes = HashSet<Node>()
        for (node in p.graph?.nodes ?: emptyList()) {
            if (doneNodes.add(node)) {
                // not yet done; just in case sth appears twice
                val panel = p.childrenMap.getOrPut(node) {
                    NodePanel(node, style)
                }
                panel.parent = this
                // calculate the zoomed size
                // the panel may not comply, because it needs the space
                // the node panel could reduce itself to only the header,
                // only the name and color,
                // or just a colored dot :)
                panel.calculateSize(cw, cw)
            }
        }

        p.childrenMap.keys.removeIf { it !in doneNodes } // O(n log n)

        minX = p.childrenMap.minOfOrNull { it.value.x } ?: 0
        minY = p.childrenMap.minOfOrNull { it.value.y } ?: 0
        minW = p.childrenMap.maxOfOrNull { it.value.x + it.value.w } ?: 0 - minX
        minH = p.childrenMap.maxOfOrNull { it.value.y + it.value.h } ?: 0 - minY

        this.w = minW
        this.h = minH

    }


    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        // place all children
        val cw = p.defaultNodeSize * p.zoom
        val center = p.center
        for (child in p.childrenMap.values) {
            val node = child.node
            val position = node.position
            // center the nodes at their respective position
            val dx = (position.x - center.x) * cw + (w - child.w) * 0.5
            val dy = (position.y - center.y) * cw + (h - child.h) * 0.5
            child.placeInParent(x + dx.toInt(), y + dy.toInt())
        }
    }
}