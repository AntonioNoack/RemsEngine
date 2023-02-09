package me.anno.graph.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.SceneView.Companion.testScene2
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.render.compiler.MaterialGraphCompiler
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.StartNode
import me.anno.graph.ui.GraphEditor
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Color.black
import me.anno.utils.Color.toARGB

// todo bug: <tab> in vector input not switching to next one

// todo quat to vec?

// todo create post-process graph with stages...
// todo use that for general rendering instead of our pre-defined attempts :)

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
                "Vector4f" -> "vec4(vec3($expr?1.0:0.0),1.0)"
                else -> null
            }
            "Float" -> when (dstType) {
                "Bool" -> "$expr!=0.0"
                "Int" -> "int($expr)"
                "Vector2f" -> "vec2($expr)"
                "Vector3f" -> "vec3($expr)"
                "Vector4f" -> "vec4(vec3($expr),1.0)"
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
                "Bool" -> "($expr).x!=0.0"
                "Float" -> "($expr).x"
                "Vector3f" -> "vec3($expr,0.0)"
                "Vector4f" -> "vec4($expr,0.0,1.0)"
                else -> null
            }
            "Vector3f" -> when (dstType) {
                "Bool" -> "($expr).x!=0.0"
                "Float" -> "($expr).x"
                "Vector2f" -> "($expr).xy"
                "Vector4f" -> "vec4($expr,1.0)"
                else -> null
            }
            "Vector4f" -> when (dstType) {
                "Bool" -> "($expr).x!=0.0"
                "Float" -> "($expr).x"
                "Vector2f" -> "($expr).xy"
                "Vector3f" -> "($expr).xyz"
                "Texture" -> expr
                else -> null
            }
            "ITexture2D" -> when (dstType) {
                "Bool" -> "texture($expr,uv).x!=0.0"
                "Float" -> "texture($expr,uv).x"
                "Vector2f" -> "texture($expr,uv).xy"
                "Vector3f" -> "texture($expr,uv).xyz"
                "Vector4f" -> "texture($expr,uv)"
                else -> null
            }
            else -> null
        }
    }

    val colorWithAlpha = DeferredLayerType(
        "Color", "finalColorA", 4,
        DeferredLayerType.COLOR.defaultValueARGB.toARGB() or black
    )
    val layers = arrayOf(
        colorWithAlpha,
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

    val library = NodeLibrary(
        NodeLibrary.flowNodes.nodes + listOf(
            { DiscardNode() },
            { MaterialReturnNode() },
            { TextureNode() },
            { MovieNode() },
            { NormalMap() },
        )
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val g = object : FlowGraph() {
            override fun canConnectTypeToOtherType(srcType: String, dstType: String): Boolean {
                if (srcType == "Texture") return when (dstType) {
                    "Boolean", "Bool", "Int", "Float", "Vector2f", "Vector3f", "Vector4f" -> true
                    else -> false
                }
                return convert(srcType, dstType, "") != null
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
        testUI {
            val ui = CustomList(false, style)
            val ge = object : GraphEditor(g, style) {
                override fun canDeleteNode(node: Node): Boolean {
                    return node !== start
                }
            }
            ge.library = library
            // register everything for copying
            registerCustomClass(NodeInput())
            registerCustomClass(NodeOutput())
            for (element in ge.library.nodes) {
                registerCustomClass(element)
            }
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
