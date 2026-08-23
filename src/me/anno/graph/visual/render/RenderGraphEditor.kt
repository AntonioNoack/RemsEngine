package me.anno.graph.visual.render

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.compiler.ExpressionRenderer
import me.anno.graph.visual.render.compiler.ShaderGraphNode
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.StartNode
import me.anno.ui.canvas.Canvas
import me.anno.ui.editor.graph.GraphEditor
import me.anno.ui.Style

/**
 * UI for render graph editing
 * */
class RenderGraphEditor(val rv: RenderView, graph: FlowGraph, style: Style) : GraphEditor(graph, style) {

    init {

        minScale.set( 0.01)

        library = RenderGraph.library
        for (it in library.allNodes) {
            val sample = it.first
            if (sample.className !in objectTypeRegistry)
                registerCustomClass(sample)
        }

        addChangeListener { _, isNodePositionChange ->
            if (!isNodePositionChange) {
                for (node in graph.nodes) {
                    when (node) {
                        is ShaderGraphNode -> node.invalidate()
                        is ExpressionRenderer -> node.invalidate()
                        is RenderViewNode -> node.invalidate()
                    }
                }
            }
        }
    }

    var drawResultInBackground = true

    override fun draw(canvas: Canvas) {
        if (drawResultInBackground) {
            // these two calls could be joined
            drawBackground(canvas)
            drawTransparentBackground(x, y, width, height)
            // actually drawing the graph
            RenderGraph.draw(rv, this, graph as FlowGraph)
            drawNodeGroups(canvas)
            drawNodeConnections(canvas)
            drawNodePanels(canvas)
            drawSelection(canvas)
            drawScrollbars(canvas)
        } else super.draw(canvas)
    }

    override fun canDeleteNode(node: Node): Boolean {
        return node !is StartNode
    }
}