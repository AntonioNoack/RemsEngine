package me.anno.graph.ui

import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.input.Input.isShiftDown
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.pow
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.style.Style
import org.joml.Vector3d

class GraphPanel(var graph: Graph? = null, style: Style) :
    PanelList(null, style) {

    // todo graphics: billboards: light only / override color
    // todo rendered when point is visible, or always (for nice light camera-bug effects, e.g. stars with many blades)

    // todo allow moving around
    // todo zooming in and out
    // todo show all nodes
    // todo reset transform to get back in case you are lost
    // todo zoom onto a node?

    // todo add scroll bars, when the content goes over the borders

    var center = Vector3d()

    // large scale = fast movement
    var scale = 1.0

    // what should the default size of a node be?
    var defaultNodeSize = 250.0

    val childrenMap = HashMap<Node, NodePanel>()

    fun ensureChildren() {
        val graph = graph ?: return
        for (node in graph.nodes) {
            childrenMap.getOrPut(node) {
                NodePanel(node, style)
            }
        }
    }

    init {
        addRightClickListener {
            // todo list of all node options
            // todo groups for them
            // todo reset graph? idk...
            // todo button to save graph (?)
            // todo button to create new sub function (?)
            openMenu(windowStack, listOf(
                MenuOption(NameDesc("New Node")) {

                }
            ))
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        ensureChildren()
        for (child in children) {
            val childSize = (scale * 200).toInt()
            child.calculateSize(childSize, childSize)
        }
        minW = w
        minH = h
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        ensureChildren()
        // place all children
        val graph = graph ?: return
        for (node in graph.nodes) {
            val panel = childrenMap[node] ?: continue
            val xi = x + (node.position.x - center.x).toInt() - panel.w / 2
            val yi = y + (node.position.y - center.y).toInt() - panel.h / 2
            panel.placeInParent(xi, yi)
        }
    }

    // todo show soft coordinate lines
    // todo powers of two
    /*override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // background
        super.onDraw(x0, y0, x1, y1)

    }*/

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (isShiftDown) {
            // move around
            center.x += dx * scale
            center.y += dy * scale
        } else {
            // todo zoom in on the mouse pointer
            scale *= pow(1.05, dy.toDouble())
        }
    }

}