package me.anno.tests.gfx.graphs

import me.anno.config.DefaultConfig
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.SceneView
import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.render.MaterialGraph
import me.anno.graph.render.MaterialReturnNode
import me.anno.graph.render.compiler.MaterialGraphCompiler
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.StartNode
import me.anno.graph.ui.GraphEditor
import me.anno.io.ISaveable
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio

fun main(){

    val g = object : FlowGraph() {
        override fun canConnectTypeToOtherType(srcType: String, dstType: String): Boolean {
            if (srcType == "Texture") return when (dstType) {
                "Boolean", "Bool", "Int", "Float", "Vector2f", "Vector3f", "Vector4f" -> true
                else -> false
            }
            return MaterialGraph.convert(srcType, dstType, "") != null
        }
    }

    val start = StartNode(
        listOf(
            "Vector3f", "Local Position",
            "Vector3f", "CamSpace Position",
            "Vector2f", "UVs",
            "Vector3f", "Normal",
            "Vector4f", "Tangent",
            "Vector3f", "Bitangent",
            "Vector4f", "Vertex Color",
        )
    )
    g.add(start)
    start.position.set(-200.0, 0.0, 0.0)
    val end = g.add(MaterialReturnNode())
    end.position.set(200.0, 0.0, 0.0)
    start.connectTo(end)
    start.connectTo(3, end, 1)

    val m = Material()
    fun compile() {
        m.shader = MaterialGraphCompiler(start, g, 1000).shader
    }

    compile()
    // show resulting material as preview
    TestStudio.testUI("MaterialGraph") {
        val ui = CustomList(false, DefaultConfig.style)
        val ge = object : GraphEditor(g, DefaultConfig.style) {
            override fun canDeleteNode(node: Node): Boolean {
                return node !== start
            }
        }
        ge.library = MaterialGraph.library
        // register everything for copying
        ISaveable.registerCustomClass(NodeInput())
        ISaveable.registerCustomClass(NodeOutput())
        for (element in ge.library.nodes) {
            ISaveable.registerCustomClass(element)
        }
        ge.addChangeListener { _, isNodePositionChange ->
            if (!isNodePositionChange) {
                compile()
            }
        }
        ui.add(ge, 1f)
        ui.add(SceneView.testScene2(m))
    }
}