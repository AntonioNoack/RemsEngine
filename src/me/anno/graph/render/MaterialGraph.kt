package me.anno.graph.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.SceneView.Companion.testScene2
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.StartNode
import me.anno.graph.ui.GraphEditor
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI

// todo bug: <tab> in vector input not switching to next one

// todo const color node
// todo quat to vec?

object MaterialGraph {

    fun kotlinToGLSL(type: String): String {
        return when (type) {
            "Int" -> "int"
            "Float" -> "float"
            "Vector2f" -> "vec2"
            "Vector3f" -> "vec3"
            "Vector4f" -> "vec4"
            "Bool", "Boolean" -> "bool"
            else -> throw NotImplementedError(type)
        }
    }

    fun convert(srcType: String, dstType: String, expr: String): String? {
        if (srcType == dstType) return expr
        if (srcType == "Boolean") return convert("Bool", dstType, expr)
        else if (dstType == "Boolean") return convert(srcType, "Bool", expr)
        return when (srcType) {
            "Bool" -> when (dstType) {
                "Int" -> "$expr?1:0"
                "Float" -> "$expr?1.0:0.0"
                "Vector2f" -> "vec2($expr?1.0:0.0)"
                "Vector3f" -> "vec3($expr?1.0:0.0)"
                "Vector4f" -> "vec4($expr?1.0:0.0)"
                else -> null
            }
            "Float" -> when (dstType) {
                "Bool" -> "$expr!=0.0"
                "Int" -> "int($expr)"
                "Vector2f" -> "vec2($expr)"
                "Vector3f" -> "vec3($expr)"
                "Vector4f" -> "vec4($expr)"
                else -> null
            }
            "Int" -> when (dstType) {
                "Bool" -> "$expr!=0"
                "Float" -> "float($expr)"
                "Vector2f" -> "vec2($expr)"
                "Vector3f" -> "vec3($expr)"
                "Vector4f" -> "vec4($expr)"
                else -> null
            }
            "Vector2f" -> when (dstType) {
                "Bool" -> "$expr!=vec2(0)"
                "Float" -> "length($expr)"
                "Vector3f" -> "vec3($expr,0.0)"
                "Vector4f" -> "vec4($expr,0.0,0.0)"
                else -> null
            }
            "Vector3f" -> when (dstType) {
                "Bool" -> "$expr!=vec3(0)"
                "Float" -> "length($expr)"
                "Vector2f" -> "($expr).xy"
                "Vector4f" -> "vec4($expr,0.0)"
                else -> null
            }
            "Vector4f" -> when (dstType) {
                "Bool" -> "$expr!=vec4(0)"
                "Float" -> "length($expr)"
                "Vector2f" -> "($expr).xy"
                "Vector3f" -> "($expr).xyz"
                else -> null
            }
            else -> null
        }
    }

    val layers = arrayOf(
        DeferredLayerType.COLOR,
        DeferredLayerType.EMISSIVE,
        DeferredLayerType.NORMAL,
        DeferredLayerType.TANGENT,
        DeferredLayerType.POSITION,
        DeferredLayerType.METALLIC,
        DeferredLayerType.ROUGHNESS,
        DeferredLayerType.OCCLUSION,
        // DeferredLayerType.TRANSLUCENCY,
        // DeferredLayerType.SHEEN,
        // DeferredLayerType.SHEEN_NORMAL,
        // DeferredLayerType.CLEAR_COAT,
        // DeferredLayerType.CLEAT_COAT_ROUGH_METALLIC,
        // DeferredLayerType.CLEAR_COAT_NORMAL,
        // DeferredLayerType.SUBSURFACE,
        // DeferredLayerType.ANISOTROPIC,
        // DeferredLayerType.INDEX_OF_REFRACTION,
    )

    val types = arrayOf(
        "Float",
        "Vector2f",
        "Vector3f",
        "Vector4f"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val g = object : FlowGraph() {
            override fun canConnectTypeToOtherType(srcType: String, dstType: String): Boolean {
                return convert(srcType, dstType, "") != null
            }
        }
        // todo create simple calculation
        val start = StartNode(
            listOf(
                "Vector3f", "Local Position",
                "Vector3f", "CamSpace Position",
                "Vector2f", "UVs",
                "Vector3f", "Normal",
                "Vector4f", "Tangent",
                "Vector4f", "Vertex Color",
            )
        )
        g.nodes.add(start)
        start.position.set(-200.0, 0.0, 0.0)
        // define return node
        val ret = MaterialReturnNode()

        g.nodes.add(ret)
        ret.position.set(200.0, 0.0, 0.0)
        start.connectTo(ret)
        val m = Material()

        fun compile() {
            m.shader = MaterialGraphCompiler(start, g, 1000).shader
        }

        compile()
        // show resulting material as preview
        testUI {
            val ui = CustomList(false, style)
            val ge = GraphEditor(g, style)
            ge.library = NodeLibrary(
                ge.library.nodes + listOf(
                    { DiscardNode() },
                    { MaterialReturnNode() },
                    { TextureNode() },
                    { RandomNode() },
                    { ColorNode() },
                )
            )
            ge.addChangeListener { _, isNodePositionChange ->
                if (!isNodePositionChange) {
                    compile()
                }
            }
            ui.add(ge, 1f)
            ui.add(testScene2(m))
        }
    }
}
