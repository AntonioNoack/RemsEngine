package me.anno.graph.ui

import me.anno.graph.Graph
import me.anno.input.Input.isShiftDown
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.pow
import org.joml.Vector3d

class GraphPanel(style: Style) : PanelGroup(style) {

    var graph: Graph? = null

    // todo allow moving around
    // todo zooming in and out
    // todo show all nodes
    // todo reset transform to get back in case you are lost
    // todo zoom onto a node?

    // todo options to create a new node

    var offset = Vector3d()
    var zoom = 1.0

    // what should the default size of a node be?
    var defaultNodeSize = 100.0

    override val children = ArrayList<NodePanel>()

    override fun remove(child: Panel) {}

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        children.clear()
        for (node in graph?.nodes ?: emptyList()) {
            val panel = NodePanel(node, style)
            children.add(panel)
            panel.parent = this
            // calculate the zoomed size
            // the panel may not comply, because it needs the space
            // the node panel could reduce itself to only the header,
            // only the name and color,
            // or just a colored dot :)
            val cw = (defaultNodeSize * zoom).toInt()
            panel.calculateSize(cw, cw)
        }

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        // place all children
        for (child in children) {
            val node = child.node
            val position = node.position
            // center the nodes at their respective position
            val dx = (position.x - offset.x) * zoom - child.w * 0.5
            val dy = (position.y - offset.y) * zoom - child.h * 0.5
            child.placeInParent(dx.toInt(), dy.toInt())
        }
    }

    /*override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // background
        super.onDraw(x0, y0, x1, y1)

        // todo show soft coordinate lines? idk

    }*/

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if (isShiftDown) {
            // move around
            offset.x += dx * zoom
            offset.y += dy * zoom
        } else {
            // todo zoom in on the mouse pointer
            zoom *= pow(1.05, dy.toDouble())
        }
    }

}