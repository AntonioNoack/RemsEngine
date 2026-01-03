package me.anno.mesh.blender

import me.anno.ecs.prefab.Prefab
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.GLASS_PASS
import me.anno.gpu.shader.ShaderLib
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.impl.BID
import me.anno.mesh.blender.impl.BImage
import me.anno.mesh.blender.impl.BMaterial
import me.anno.mesh.blender.impl.nodes.BNode
import me.anno.mesh.blender.impl.nodes.BNodeSocket
import me.anno.mesh.blender.impl.nodes.BNodeTree
import me.anno.mesh.blender.impl.values.BNSVBoolean
import me.anno.mesh.blender.impl.values.BNSVFloat
import me.anno.mesh.blender.impl.values.BNSVInt
import me.anno.mesh.blender.impl.values.BNSVRGBA
import me.anno.mesh.blender.impl.values.BNSVVector
import me.anno.mesh.blender.impl.values.BNSValue
import me.anno.utils.Color.white4
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.pow

/**
 * Converts a node tree of the "Shading" section in Blender to a Rem's Engine Material as far as possible.
 * */
object BlenderMaterialConverter {

    private val LOGGER = LogManager.getLogger(BlenderMaterialConverter::class)

    fun defineDefaultMaterial(prefab: Prefab, mat: BMaterial, imageMap: Map<String, FileReference>) {
        prefab["diffuseBase"] = Vector4f(mat.r, mat.g, mat.b, mat.a)
        prefab["metallicMinMax"] = Vector2f(0f, mat.metallic)
        prefab["roughnessMinMax"] = Vector2f(0f, mat.roughness)
        val nodeTree = mat.nodeTree
        if (nodeTree != null) {
            defineDefaultMaterial(prefab, nodeTree, imageMap)
        }
    }

    /**
     * read textures for roughness, metallic, emissive, and such
     * */
    fun defineDefaultMaterial(prefab: Prefab, nodeTree: BNodeTree, imageMap: Map<String, FileReference>) {
        LOGGER.debug(nodeTree)
        val nodes = nodeTree.nodes.toList()
        val links = nodeTree.links
        val outputNode = nodes.firstOrNull {
            it.type == "ShaderNodeOutputMaterial"
        }
        if (outputNode != null) {
            // find color / texture input into this node
            //     alternatively, we could build a shader here using our own shader graph... (meh xD)
            //     (cool, but not well scalable)
            val inputs = HashMap<Pair<BNode, BNodeSocket>, Pair<BNode?, BNodeSocket>>()
            for (link in links) {
                inputs[Pair(link.toNode, link.toSocket)] = Pair(link.fromNode, link.fromSocket)
            }

            fun getDefault(default: BNSValue?): Pair<Vector4f, FileReference>? {
                return when (default) {
                    is BNSVVector -> Vector4f(default.value)
                    is BNSVFloat -> Vector4f(default.value)
                    is BNSVInt -> Vector4f(default.value.toFloat())
                    is BNSVRGBA -> Vector4f(default.value)
                    is BNSVBoolean -> Vector4f(if (default.value) 1f else 0f)
                    null -> return null
                    else -> {
                        LOGGER.warn("Unknown default value: $default")
                        return null
                    }
                } to InvalidRef
            }

            fun lookup(node: BNode, name: String): Pair<BNode?, BNodeSocket>? {
                val socket = node.findInputSocket(name)
                return inputs[node to socket] ?: if (socket != null) {
                    null to socket
                } else null
            }

            fun multiply(value: Pair<Vector4f, FileReference>, strength: Float?): Pair<Vector4f, FileReference> {
                if (strength == null) return value
                return Pair(value.first * strength, value.second)
            }

            fun findTintedMap(value: Pair<BNode?, BNodeSocket>?): Pair<Vector4f, FileReference>? {
                val (node, socket) = value ?: return null
                when (node?.type) {
                    // todo can we get a bunch of Blender files to test this?
                    "ShaderNodeRGB" -> {
                        val output = node.findOutputSocket("Color")
                        return getDefault(output?.defaultValue)
                    }
                    "ShaderNodeNormalMap" -> {
                        // to do check normal space... only tangent is truly supported
                        val color = findTintedMap(lookup(node, "Color"))
                        if (color != null) {
                            val strength = findTintedMap(lookup(node, "Strength"))
                            return multiply(color, strength?.first?.x)
                        }
                    }
                    "ShaderNodeTexImage" -> {
                        // UVs: "Vector",
                        // Output: "Color"/"Alpha" -> use the correct slot (given by socket)
                        val imageName = when (val nid = node.id) {
                            is BID -> nid.realName
                            is BImage -> nid.id.realName
                            else -> return null
                        }
                        val source0 = imageMap[imageName] ?: return null
                        val source1 = if (value.second.name == "Alpha") source0.getChild("a.png") else source0
                        return white4 to source1
                    }
                    "ShaderNodeValToRGB" -> {
                        val value1 = findTintedMap(lookup(node, "Fac"))
                        if (value1 != null) {
                            // kinda...
                            return Pair(Vector4f(value1.first.x), value1.second)
                        }
                    }
                    "ShaderNodeMixRGB" -> {
                        // todo find mix value?
                        // return first value as a guess
                        return findTintedMap(lookup(node, "Color1")) ?: findTintedMap(lookup(node, "Color2"))
                    }
                    "ShaderNodeMix" -> {
                        // todo this node is actually a multiply-node,
                        //  and it has tons of inputs of the same name :(
                        return getDefault(socket.defaultValue)
                    }
                    null -> return getDefault(socket.defaultValue)
                    else -> LOGGER.warn("Unknown node type ${node.type}, ${node.inputs.map { "${it.type} ${it.name}" }}")
                }
                return null
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

                fun findData(shaderNode: BNode) {
                    when (shaderNode.type) {
                        // types: https://docs.blender.org/api/current/bpy.types.html
                        "ShaderNodeBsdfPrincipled" -> {
                            // extract all properties as far as possible
                            val diffuse = findTintedMap(lookup(shaderNode, "Base Color"))
                            LOGGER.debug("ShaderNodeBsdfPrincipled.diffuse: {}", diffuse)
                            if (diffuse != null) {
                                val base = diffuse.first
                                val gamma = ShaderLib.gammaInv.toFloat()
                                prefab["diffuseBase"] = Vector4f(
                                    base.x.pow(gamma),
                                    base.y.pow(gamma),
                                    base.z.pow(gamma), base.w
                                )
                                prefab["diffuseMap"] = diffuse.second
                            }
                            // can we find out mappings? are they used at all?
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
                            prefab["pipelineStage"] = GLASS_PASS
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
                            val map = findTintedMap(lookup(shaderNode, "Color"))
                            val strength = findTintedMap(lookup(shaderNode, "Strength"))
                            if (map != null || strength != null) {
                                setMultiplicativeEmissive(map, strength)
                            }
                        }
                        "ShaderNodeMixShader" -> {
                            // go both paths...
                            for (input in shaderNode.inputs) {
                                if (input.name == "Shader") { // true for both; but not for "Fac"
                                    val output = inputs[shaderNode to input]?.first
                                    if (output != null) findData(output)
                                }
                            }
                        }
                        "ShaderNodeAddShader" -> {
                            // go both paths...
                            for (input in shaderNode.inputs) {
                                if (input.name == "Shader") { // true for both
                                    val output = inputs[shaderNode to input]?.first
                                    if (output != null) findData(output)
                                }
                            }
                        }
                        else -> LOGGER.warn("Unknown/unsupported shader node: ${shaderNode.type}")
                        // ...
                    }
                }
                findData(shaderNode)
                LOGGER.debug(prefab.sets)
            } else LOGGER.warn("Surface Socket missing")
        }
    }
}