package me.anno.graph.ui

import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.input.Input.isShiftDown
import me.anno.language.translation.NameDesc
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.pow
import org.joml.Vector3d

class GraphPanel(var graph: Graph? = null, style: Style) : ScrollPanelXY(ContentPanel(style), Padding(4), style) {

    // todo graphics: billboards: light only / override color
    // todo rendered when point is visible, or always (for nice light camera-bug effects, e.g. stars with many blades)

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

    override val content = child as ContentPanel

    init {
        content.p = this
    }

    // todo allow moving around
    // todo zooming in and out
    // todo show all nodes
    // todo reset transform to get back in case you are lost
    // todo zoom onto a node?

    // todo add scroll bars, when the content goes over the borders

    var center = Vector3d()
    var zoom = 1.0

    // what should the default size of a node be?
    var defaultNodeSize = 250.0

    private val childrenMap = HashMap<Node, NodePanel>()

    init {
        addRightClickListener {
            // todo list of all node options
            // todo groups for them
            // todo reset graph? idk...
            // todo button to save graph (?)
            // todo button to create new sub function (?)
            openMenu(listOf(
                MenuOption(NameDesc("New Node")) {

                }
            ))
        }
    }

    override fun remove(child: Panel) {}

    /*override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // background
        super.onDraw(x0, y0, x1, y1)

        // todo show soft coordinate lines? idk

    }*/

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (isShiftDown) {
            // move around
            center.x += dx * zoom
            center.y += dy * zoom
        } else {
            // todo zoom in on the mouse pointer
            zoom *= pow(1.05, dy.toDouble())
        }
    }

}