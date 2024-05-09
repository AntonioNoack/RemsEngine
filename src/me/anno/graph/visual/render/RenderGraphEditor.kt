package me.anno.graph.visual.render

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.compiler.ExpressionRenderer
import me.anno.graph.visual.render.compiler.ShaderGraphNode
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.StartNode
import me.anno.ui.editor.graph.GraphEditor
import me.anno.ui.Style

/**
 * UI for render graph editing
 * */
class RenderGraphEditor(val rv: RenderView, graph: FlowGraph, style: Style) : GraphEditor(graph, style) {

    init {

        minScale = 0.01

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

    override fun onUpdate() {
        super.onUpdate()
        invalidateDrawing() // for smooth rendering
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (drawResultInBackground) {
            drawTransparentBackground(x, y, width, height)
            RenderGraph.draw(rv, this, graph as FlowGraph)
            drawNodeGroups(x0, y0, x1, y1)
            drawNodeConnections(x0, y0, x1, y1)
            drawNodePanels(x0, y0, x1, y1)
            drawSelection(x0, y0, x1, y1)
            drawScrollbars(x0, y0, x1, y1)
        } else super.onDraw(x0, y0, x1, y1)
    }

    override fun canDeleteNode(node: Node): Boolean {
        return node !is StartNode
    }
}