package me.anno.tests.gfx.graphs

import me.anno.gpu.pipeline.PipelineStage
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestCloneGfxGraph {

    class TestNode : ActionNode(
        "TestNode",
        listOf("Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage"),
        emptyList()
    ) {

        init {
            setInput(1, PipelineStage.GLASS)
        }

        val stage: PipelineStage
            get() = getInput(1) as PipelineStage

        override fun executeAction() {}
    }

    @BeforeEach
    fun init() {
        // ensure types are available for cloning
        registerCustomClass(FlowGraph::class)
        registerCustomClass(NodeInput::class)
        registerCustomClass(NodeOutput::class)
        registerCustomClass(TestNode::class)
    }

    @Test
    fun testCloningGraph() {
        val originalGraph = FlowGraph()
        originalGraph.add(TestNode())
        val originalNode = originalGraph.nodes
            .firstInstance2(TestNode::class)
        assertEquals(PipelineStage.GLASS, originalNode.stage)
        originalNode.setInput(1, PipelineStage.DECAL)
        assertEquals(PipelineStage.DECAL, originalNode.stage)
        val clonedGraph = originalGraph.clone() as FlowGraph
        val clonedNode = clonedGraph.nodes
            .firstInstance2(TestNode::class)
        assertEquals(PipelineStage.DECAL, clonedNode.stage)
    }

    @Test
    fun testCloningNode() {
        val originalNode = TestNode()
        assertEquals(PipelineStage.GLASS, originalNode.stage)
        originalNode.setInput(1, PipelineStage.DECAL)
        assertEquals(PipelineStage.DECAL, originalNode.stage)
        val clonedNode = originalNode.clone() as TestNode
        assertEquals(PipelineStage.DECAL, clonedNode.stage)
    }
}