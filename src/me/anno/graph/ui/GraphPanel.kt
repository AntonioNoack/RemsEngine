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

class GraphPanel(var graph: Graph? = null, style: Style) :
    ScrollPanelXY(ContentPanel(style), Padding(4), style) {

    // todo graphics: billboards: light only / override color
    // todo rendered when point is visible, or always (for nice light camera-bug effects, e.g. stars with many blades)

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

    val childrenMap = HashMap<Node, NodePanel>()

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