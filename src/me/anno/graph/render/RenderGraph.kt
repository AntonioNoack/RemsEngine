package me.anno.graph.render

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.FlowGraphNode
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.ui.GraphPanel
import me.anno.ui.Panel
import me.anno.ui.style.Style
import me.anno.utils.structures.lists.Lists.firstOrNull2
import java.util.BitSet

// todo scene processing
// todo stage 0:
//  scene, meshes
//  filtering & adding to pipeline
//  pipeline list with their settings
// todo stage 3:
//  applying effects & calculations
// todo stage 4:
//  primary & debug outputs; name -> enum in RenderView
// data type is RGBA or Texture2D or sth like this

// todo create collection of render graphs for debugging

class RenderGraph {

    class SceneNode: ActionNode(
        "Scene",
        listOf(),
        // list all available deferred layers
        DeferredLayerType.values.map { listOf("Texture", it.name) }.flatten()
    ) {
        val enabledLayers = BitSet(DeferredLayerType.values.size)
        override fun executeAction(graph: FlowGraph) {
            TODO("Not yet implemented")
        }
    }

    val graph = Graph()

    fun createUI(style: Style): Panel {
        val panel = GraphPanel(graph, style)
        panel.library = library
        return panel
    }

    fun draw(view: RenderView) {
        // which result do we need?
        val result = graph.outputs.firstOrNull2() ?: return
        // calculate, which scenes/meshes we need
        view.pipeline.clear()
        view.pipeline.fill(view.getWorld() ?: return, view.cameraPosition, view.worldScale)
        // todo first run: find all deferred layers, that we need
        // todo then decide about deferred rendering yes/no

    }

    companion object {
        val library = NodeLibrary(
            NodeLibrary.flowNodes.nodes
        )
    }

}