package me.anno.engine.ui.render.graph

import me.anno.graph.Graph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.ui.GraphPanel
import me.anno.ui.Panel
import me.anno.ui.style.Style

// todo scene processing
// todo stage 0:
//  scene, meshes
// todo stage 1:
//  filtering & adding to pipeline
// todo stage 2:
//  pipeline list with their settings
// todo stage 3:
//  applying effects & calculations
// todo stage 4:
//  primary & debug outputs; name -> enum in RenderView
// data type is RGBA or Texture2D or sth like this
class RenderGraph {

    val graph = Graph()

    fun createUI(style: Style): Panel {
        val panel = GraphPanel(graph, style)
        panel.library = library
        return panel
    }

    companion object {
        val library = NodeLibrary(
            NodeLibrary.flowNodes.nodes
        )
    }

}