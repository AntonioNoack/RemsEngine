package me.anno.graph.render

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.graph.Node
import me.anno.graph.render.compiler.ExpressionRenderer
import me.anno.graph.render.compiler.ShaderGraphNode
import me.anno.graph.render.scene.RenderSceneNode0
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.StartNode
import me.anno.graph.ui.GraphEditor
import me.anno.io.Saveable
import me.anno.io.Saveable.Companion.registerCustomClass
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
                        is RenderSceneNode0 -> node.invalidate()
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