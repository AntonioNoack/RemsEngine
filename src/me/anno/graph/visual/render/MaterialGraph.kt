package me.anno.graph.visual.render

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.graph.visual.node.NodeLibrary
import me.anno.utils.Color.black
import me.anno.utils.Color.toARGB

// todo quat to vec?

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

    fun convert(builder: StringBuilder, srcType: String, dstType: String, expr: () -> Unit): Unit? {
        if (srcType == dstType || kotlinToGLSL(srcType) == kotlinToGLSL(dstType)) return expr()
        when (srcType) {
            "Bool", "Boolean" -> {
                var prefix = ""
                val suffix = when (dstType) {
                    "Int", "Long" -> "?1:0"
                    "Float", "Double" -> "?1.0:0.0"
                    "Vector2f" -> {
                        prefix = "vec2("
                        "?1.0:0.0)"
                    }
                    "Vector3f" -> {
                        prefix = "vec3("
                        "?1.0:0.0)"
                    }
                    "Vector4f" -> {
                        prefix = "vec4(vec3("
                        "?1.0:0.0),1.0)"
                    }
                    else -> return null
                }
                exprAppend(builder, prefix, suffix, expr)
            }
            "Float", "Double" -> {
                var suffix = ")"
                val prefix = when (dstType) {
                    "Bool", "Boolean" -> {
                        suffix = "!=0.0)"
                        "("
                    }
                    "Float", "Double" -> {
                        suffix = ""
                        ""
                    }
                    "Int", "Long" -> "int("
                    "Vector2f" -> "vec2("
                    "Vector3f" -> "vec3("
                    "Vector4f" -> {
                        suffix = "),1.0)"
                        "vec4(vec3("
                    }
                    else -> return null
                }
                exprAppend(builder, prefix, suffix, expr)
            }
            "Int", "Long" -> {
                var suffix = ")"
                val prefix = when (dstType) {
                    "Bool", "Boolean" -> {
                        suffix = "!=0)"
                        "("
                    }
                    "Int", "Long" -> {
                        suffix = ""
                        ""
                    }
                    "Float", "Double" -> "float("
                    "Vector2f" -> "vec2("
                    "Vector3f" -> "vec3("
                    "Vector4f" -> "vec4("
                    else -> return null
                }
                exprAppend(builder, prefix, suffix, expr)
            }
            "Vector2f" -> {
                var prefix = "("
                val suffix = when (dstType) {
                    "Bool", "Boolean" -> ").x!=0.0"
                    "Float", "Double" -> ").x"
                    "Vector3f" -> {
                        prefix = "vec3("
                        ",0.0)"
                    }
                    "Vector4f" -> {
                        prefix = "vec4("
                        ",0.0,1.0)"
                    }
                    else -> return null
                }
                exprAppend(builder, prefix, suffix, expr)
            }
            "Vector3f" -> {
                var prefix = "("
                val suffix = when (dstType) {
                    "Bool", "Boolean" -> ").x!=0.0"
                    "Float", "Double" -> ").x"
                    "Vector2f" -> ").xy"
                    "Vector4f" -> {
                        prefix = "vec4("
                        ",1.0)"
                    }
                    else -> return null
                }
                exprAppend(builder, prefix, suffix, expr)
            }
            "Vector4f" -> {
                val suffix = when (dstType) {
                    "Bool", "Boolean" -> ".x!=0.0"
                    "Float", "Double" -> ".x"
                    "Vector2f" -> ".xy"
                    "Vector3f" -> ".xyz"
                    "Texture" -> ""
                    else -> return null
                }
                exprAppend(builder, "(", ")", suffix, expr)
            }
            "ITexture2D" -> {
                val suffix = when (dstType) {
                    "Bool", "Boolean" -> ".x!=0.0"
                    "Float", "Double" -> ".x"
                    "Vector2f" -> ".xy"
                    "Vector3f" -> ".xyz"
                    "Vector4f" -> ""
                    else -> return null
                }
                exprAppend(builder, "texture(", ",uv)", suffix, expr)
            }
            else -> return null
        }
        return Unit
    }

    private fun exprAppend(
        builder: StringBuilder, prefix: String, preSuffix: String, suffix: String,
        expr: () -> Unit
    ) {
        builder.append(prefix)
        expr()
        builder.append(preSuffix).append(suffix)
    }

    private fun exprAppend(builder: StringBuilder, prefix: String, suffix: String, expr: () -> Unit) {
        builder.append(prefix)
        expr()
        builder.append(suffix)
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

    val floatVecTypes = listOf(
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
