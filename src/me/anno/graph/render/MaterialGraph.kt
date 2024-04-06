package me.anno.graph.render

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.graph.types.NodeLibrary
import me.anno.utils.Color.black
import me.anno.utils.Color.toARGB

// todo bug: <tab> in vector input not switching to next one

// todo quat to vec?

// todo create post-process graph with stages...
// todo use that for general rendering instead of our pre-defined attempts :)

object MaterialGraph {

    fun kotlinToGLSL(type: String): String {
        return when (type) {
            "Int", "Long" -> "int"
            "Float", "Double" -> "float"
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
                "Int", "Long" -> "$expr?1:0"
                "Float", "Double" -> "$expr?1.0:0.0"
                "Vector2f" -> "vec2($expr?1.0:0.0)"
                "Vector3f" -> "vec3($expr?1.0:0.0)"
                "Vector4f" -> "vec4(vec3($expr?1.0:0.0),1.0)"
                else -> null
            }
            "Float", "Double" -> when (dstType) {
                "Bool" -> "$expr!=0.0"
                "Float", "Double" -> expr
                "Int", "Long" -> "int($expr)"
                "Vector2f" -> "vec2($expr)"
                "Vector3f" -> "vec3($expr)"
                "Vector4f" -> "vec4(vec3($expr),1.0)"
                else -> null
            }
            "Int", "Long" -> when (dstType) {
                "Bool" -> "$expr!=0"
                "Float", "Double" -> "float($expr)"
                "Vector2f" -> "vec2($expr)"
                "Vector3f" -> "vec3($expr)"
                "Vector4f" -> "vec4($expr)"
                else -> null
            }
            "Vector2f" -> when (dstType) {
                "Bool" -> "($expr).x!=0.0"
                "Float", "Double" -> "($expr).x"
                "Vector3f" -> "vec3($expr,0.0)"
                "Vector4f" -> "vec4($expr,0.0,1.0)"
                else -> null
            }
            "Vector3f" -> when (dstType) {
                "Bool" -> "($expr).x!=0.0"
                "Float", "Double" -> "($expr).x"
                "Vector2f" -> "($expr).xy"
                "Vector4f" -> "vec4($expr,1.0)"
                else -> null
            }
            "Vector4f" -> when (dstType) {
                "Bool" -> "($expr).x!=0.0"
                "Float", "Double" -> "($expr).x"
                "Vector2f" -> "($expr).xy"
                "Vector3f" -> "($expr).xyz"
                "Texture" -> expr
                else -> null
            }
            "ITexture2D" -> when (dstType) {
                "Bool" -> "texture($expr,uv).x!=0.0"
                "Float", "Double" -> "texture($expr,uv).x"
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
        DeferredLayerType.COLOR.defaultWorkValue.toARGB() or black
    )
    val layers = listOf(
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

    val types = listOf(
        "Float",
        "Vector2f",
        "Vector3f",
        "Vector4f"
    )

    val library by lazy {
        NodeLibrary(
            NodeLibrary.flowNodes.nodes + listOf(
                { DiscardNode() },
                { MaterialReturnNode() },
                { TextureNode() },
                { MovieNode() },
                { NormalMap() },
            )
        )
    }

}
