package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.gpu.pipeline.PipelineStage.Companion.TRANSPARENT_PASS
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.impl.BMaterial
import me.anno.mesh.blender.impl.BNode
import me.anno.mesh.blender.impl.BNodeSocket
import me.anno.mesh.blender.impl.BNodeTree
import me.anno.mesh.blender.impl.values.*
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

object BlenderShaderTree {

    private val LOGGER = LogManager.getLogger(BlenderShaderTree::class)

    fun defineDefaultMaterial(prefab: Prefab, mat: BMaterial) {
        prefab["diffuseBase"] = Vector4f(mat.r, mat.g, mat.b, mat.a)
        prefab["metallicMinMax"] = Vector2f(0f, mat.metallic)
        prefab["roughnessMinMax"] = Vector2f(0f, mat.roughness)
        val nodeTree = mat.nodeTree
        if (nodeTree != null) {
            defineDefaultMaterial(prefab, nodeTree)
        }
    }

    // todo read textures for roughness, metallic, emissive, and such
    fun defineDefaultMaterial(prefab: Prefab, nodeTree: BNodeTree) {
        println("node tree: $nodeTree")
        val nodes = nodeTree.nodes.toList()
        val links = nodeTree.links
        val outputNode = nodes.firstOrNull {
            it.type == "ShaderNodeOutputMaterial"
        }
        if (outputNode != null) {
            // todo find color / texture input into this node
            // todo alternatively, we could build a shader here using our own shader graph... (meh xD)
            // (cool, but not well scalable)
            val inputs = HashMap<Pair<BNode, BNodeSocket>, Pair<BNode?, BNodeSocket>>()
            for (link in links) {
                inputs[Pair(link.toNode, link.toSocket)] = Pair(link.fromNode, link.fromSocket)
            }

            fun getDefault(default: BNSValue?): Pair<Vector4f, FileReference>? {
                return when (default) {
                    is BNSVVector -> Vector4f(default.value, 1f)
                    is BNSVFloat -> Vector4f(default.value)
                    is BNSVInt -> Vector4f(default.value.toFloat())
                    is BNSVRGBA -> default.value
                    is BNSVBoolean -> Vector4f(if (default.value) 1f else 0f)
                    null -> return null
                    else -> {
                        LOGGER.warn("Unknown default value: $default")
                        return null
                    }
                } to InvalidRef
            }

            fun findTintedMap(value: Pair<BNode?, BNodeSocket>?): Pair<Vector4f, FileReference>? {
                val (node, socket) = value ?: return null
                when (node?.type) {
                    // "ShaderNodeTexImage"
                    "ShaderNodeRGB" -> {
                        val output = node.findOutputSocket("Color")
                        return getDefault(output?.defaultValue)
                    }
                    null -> return getDefault(socket.defaultValue)
                    else -> LOGGER.warn("Unknown node type ${node.type}")
                }
                return null
            }

            fun lookup(node: BNode, name: String): Pair<BNode?, BNodeSocket>? {
                val socket = node.findInputSocket(name)
                return inputs[node to socket] ?: if (socket != null) {
                    null to socket
                } else null
            }

            // only first is relevant, because Shader nodes only have a single output
            val shaderNode = lookup(outputNode, "Surface")?.first
            if (shaderNode != null) {
                fun setMultiplicativeEmissive(
                    emissive: Pair<Vector4f, FileReference>?,
                    emissive1: Pair<Vector4f, FileReference>?
                ) {
                    // find out strength by multiplying
                    prefab["emissiveMap"] = emissive?.second?.nullIfUndefined() ?: emissive1?.second
                    val strength = Vector3f(1f)
                    if (emissive != null) {
                        strength.mul(emissive.first.x, emissive.first.y, emissive.first.z)
                    }
                    if (emissive1 != null) strength.mul(emissive1.first.x) // this is just a scalar
                    prefab["emissiveBase"] = strength
                }
                when (shaderNode.type) {
                    // types: https://docs.blender.org/api/current/bpy.types.html
                    "ShaderNodeBsdfPrincipled" -> {
                        // todo extract all properties as far as possible
                        val diffuse = findTintedMap(lookup(shaderNode, "Base Color"))
                        if (diffuse != null) {
                            prefab["diffuseBase"] = diffuse.first
                            prefab["diffuseMap"] = diffuse.second
                        }
                        // todo can we find out mappings? are they used at all?
                        val metallic = findTintedMap(lookup(shaderNode, "Metallic"))
                        if (metallic != null) {
                            prefab["metallicMap"] = metallic.second
                            prefab["metallicMinMax"] = Vector2f(0f, metallic.first.x)
                        }
                        val roughness = findTintedMap(lookup(shaderNode, "Roughness"))
                        if (roughness != null) {
                            prefab["roughnessMap"] = roughness.second
                            prefab["roughnessMinMax"] = Vector2f(0f, roughness.first.x)
                        }
                        val normals = findTintedMap(lookup(shaderNode, "Normal"))
                        if (normals != null) {
                            prefab["normalMap"] = normals.second
                            prefab["normalStrength"] = normals.first.x
                        }
                        val emissive = findTintedMap(lookup(shaderNode, "Emission"))
                        val emissive1 = findTintedMap(lookup(shaderNode, "Emission Strength"))
                        if (emissive != null || emissive1 != null) {
                            setMultiplicativeEmissive(emissive, emissive1)
                        }
                        // awkward... transmission exists, too
                        // val alpha = findTintedMap()
                    }
                    "ShaderNodeBsdfDiffuse" -> {
                        // todo test this
                        val diffuse = findTintedMap(lookup(shaderNode, "Color"))
                        if (diffuse != null) {
                            prefab["diffuseBase"] = diffuse.first
                            prefab["diffuseMap"] = diffuse.second
                        }
                        val roughness = findTintedMap(lookup(shaderNode, "Roughness"))
                        if (roughness != null) {
                            prefab["roughnessMap"] = roughness.second
                            prefab["roughnessMinMax"] = Vector2f(0f, roughness.first.x)
                        }
                        val normals = findTintedMap(lookup(shaderNode, "Normal"))
                        if (normals != null) {
                            prefab["normalMap"] = normals.second
                            prefab["normalStrength"] = normals.first.x
                        }
                    }
                    "ShaderNodeBsdfGlass" -> {
                        // todo test these all, whether their names actually exist like that
                        prefab["pipelineStage"] = TRANSPARENT_PASS
                        val diffuse = findTintedMap(lookup(shaderNode, "Color"))
                        if (diffuse != null) {
                            prefab["diffuseBase"] = diffuse.first
                            prefab["diffuseMap"] = diffuse.second
                        }
                        val roughness = findTintedMap(lookup(shaderNode, "Roughness"))
                        if (roughness != null) {
                            prefab["roughnessMap"] = roughness.second
                            prefab["roughnessMinMax"] = Vector2f(0f, roughness.first.x)
                        }
                        val ior = findTintedMap(lookup(shaderNode, "IOR"))
                        if (ior != null) prefab["indexOfRefraction"] = ior.first.x
                    }
                    "ShaderNodeEmission" -> {
                        val emissive = findTintedMap(lookup(shaderNode, "Color"))
                        val emissive1 = findTintedMap(lookup(shaderNode, "Strength"))
                        if (emissive != null || emissive1 != null) {
                            setMultiplicativeEmissive(emissive, emissive1)
                        }
                    }
                    else -> LOGGER.warn("Unknown/unsupported shader node: ${shaderNode.type}")
                    // ...
                }
                println(prefab.sets)
            } else LOGGER.warn("Surface Socket missing")
        }
    }
}