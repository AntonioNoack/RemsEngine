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
import me.anno.mesh.blender.impl.nodes.BNodeLink
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
class BlenderMaterialConverter(
    private val prefab: Prefab,
    private val imageMap: Map<String, FileReference>,
    private val links: List<BNodeLink>
) {

    companion object {
        private val LOGGER = LogManager.getLogger(BlenderMaterialConverter::class)

        /**
         * read textures for roughness, metallic, emissive, and such
         * */
        fun defineDefaultMaterial(prefab: Prefab, mat: BMaterial, imageMap: Map<String, FileReference>) {
            val nodeTree = mat.nodeTree
            if (nodeTree != null) {
                defineDefaultMaterial(prefab, nodeTree, imageMap)
            } else {
                prefab["diffuseBase"] = Vector4f(mat.r, mat.g, mat.b, mat.a)
                prefab["metallicMinMax"] = Vector2f(0f, mat.metallic)
                prefab["roughnessMinMax"] = Vector2f(0f, mat.roughness)
            }
        }

        /**
         * read textures for roughness, metallic, emissive, and such
         * */
        fun defineDefaultMaterial(prefab: Prefab, nodeTree: BNodeTree, imageMap: Map<String, FileReference>) {
            LOGGER.debug(nodeTree)

            val nodes = nodeTree.nodes.toList()
            val outputNode = nodes.firstOrNull { it.type == "ShaderNodeOutputMaterial" } ?: return
            val links = nodeTree.links.toList()
            BlenderMaterialConverter(prefab, imageMap, links)
                .findData(outputNode)
        }
    }

    private fun getDefault(default: BNSValue?): Pair<Vector4f, FileReference>? {
        return when (default) {
            is BNSVVector -> {
                val value = default.value
                if (value.size == 3) {
                    val (x, y, z) = value
                    Vector4f(x, y, z, 1f)
                } else {
                    Vector4f(default.value)
                }
            }
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

    data class Link(
        val defaultValue: BNSValue?,
        val fromNode: BNode?,
        val fromName: String?,
    )

    private fun findInputs(node: BNode, inputName: String): List<Link> {
        return links.filter { it.toNode == node && it.toSocket.name == inputName }.map {
            Link(it.toSocket.defaultValue, it.fromNode, it.fromSocket.name)
        } + node.inputs.filter { it.name == inputName }.map {
            Link(it.defaultValue, null, null)
        }
    }

    private fun multiply(value: Pair<Vector4f, FileReference>, strength: Float?): Pair<Vector4f, FileReference> {
        if (strength == null) return value
        return Pair(value.first * strength, value.second)
    }

    private fun findTintedMap(links: List<Link>): Pair<Vector4f, FileReference>? {
        for (value in links) {
            val map = findTintedMap(value.fromNode, value.fromName)
            if (map != null) return map
        }
        val value = links.firstNotNullOfOrNull { it.defaultValue } ?: return null
        return getDefault(value)
    }

    private fun findTintedMap(node: BNode?, fromName: String?): Pair<Vector4f, FileReference>? {
        if (node == null) return null
        return when (node.type) {
            // todo can we get a bunch of Blender files to test this?
            "ShaderNodeRGB" -> {
                val output = node.findOutputSocket("Color")
                getDefault(output?.defaultValue)
            }
            "ShaderNodeNormalMap" -> {
                // to do check normal space... only tangent is truly supported
                val color = findTintedMap(findInputs(node, "Color"))
                if (color != null) {
                    val strength = findTintedMap(findInputs(node, "Strength"))
                    multiply(color, strength?.first?.x)
                } else null
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
                val source1 = if (fromName == "Alpha") source0.getChild("a.png") else source0
                white4 to source1
            }
            "ShaderNodeValToRGB" -> {
                val value1 = findTintedMap(findInputs(node, "Fac"))
                if (value1 != null) {
                    // kinda...
                    Pair(Vector4f(value1.first.x), value1.second)
                } else null
            }
            "ShaderNodeMixRGB" -> {
                // todo find mix value?
                // return first value as a guess
                findTintedMap(findInputs(node, "Color1"))
                    ?: findTintedMap(findInputs(node, "Color2"))
            }
            "ShaderNodeMix" -> {
                // todo this node is actually a multiply-node,
                //  and it has tons of inputs of the same name :(
                null
            }
            else -> {
                LOGGER.warn("Unknown node type ${node.type}, ${node.inputs.map { "${it.type} ${it.name}" }}")
                null
            }
        }
    }

    private fun setMultiplicativeEmissive(
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

    private fun setMetallic(shaderNode: BNode) {
        val (strength, metallicMap) = findTintedMap(findInputs(shaderNode, "Metallic")) ?: return
        prefab["metallicMap"] = metallicMap
        prefab["metallicMinMax"] = Vector2f(0f, strength.x)
    }

    private fun setRoughness(shaderNode: BNode) {
        val (strength, roughnessMap) = findTintedMap(findInputs(shaderNode, "Roughness")) ?: return
        prefab["roughnessMap"] = roughnessMap
        prefab["roughnessMinMax"] = Vector2f(0f, strength.x)
    }

    private fun setNormals(shaderNode: BNode) {
        val (strength, normalMap) = findTintedMap(findInputs(shaderNode, "Normal")) ?: return
        prefab["normalMap"] = normalMap
        prefab["normalStrength"] = strength.x
    }

    private fun setDiffuse(shaderNode: BNode, name: String) {
        val (base, diffuseMap) = findTintedMap(findInputs(shaderNode, name)) ?: return
        val gamma = ShaderLib.gammaInv.toFloat()
        // todo is it correct, that we only need gamma correction here???
        prefab["diffuseBase"] = Vector4f(
            base.x.pow(gamma),
            base.y.pow(gamma),
            base.z.pow(gamma), base.w
        )
        prefab["diffuseMap"] = diffuseMap
    }

    private fun setEmissive(shaderNode: BNode) {
        val emissive = findTintedMap(findInputs(shaderNode, "Emission"))
        val emissive1 = findTintedMap(findInputs(shaderNode, "Emission Strength"))
        if (emissive != null || emissive1 != null) {
            setMultiplicativeEmissive(emissive, emissive1)
        }
    }

    private fun findData(node: BNode) {
        println("Finding data from ${node.type}, inputs: ${node.inputs.map { "${it.name}" }}, outputs: ${node.outputs.map { "${it.name}" }}")
        // todo when we change the output type, something weird happens:
        //  our node loses most or all inputs, and the issue is fixed by deleting and recreating the MaterialOutput node in Blender
        when (node.type) {
            // types: https://docs.blender.org/api/current/bpy.types.html
            "ShaderNodeBsdfPrincipled" -> {
                // extract all properties as far as possible
                setDiffuse(node, "Base Color")
                // can we find out mappings? are they used at all?
                setMetallic(node)
                setRoughness(node)
                setNormals(node)
                setEmissive(node)
                // awkward... transmission exists, too
                // val alpha = findTintedMap()
            }
            "ShaderNodeBsdfDiffuse" -> {
                setDiffuse(node, "Color")
                setRoughness(node)
                setNormals(node)
            }
            "ShaderNodeBsdfGlass" -> {
                prefab["pipelineStage"] = GLASS_PASS
                setDiffuse(node, "Color")
                setRoughness(node)
                val ior = findTintedMap(findInputs(node, "IOR"))
                if (ior != null) prefab["indexOfRefraction"] = ior.first.x
            }
            "ShaderNodeEmission" -> {
                val map = findTintedMap(findInputs(node, "Color"))
                val strength = findTintedMap(findInputs(node, "Strength"))
                if (map != null || strength != null) {
                    setMultiplicativeEmissive(map, strength)
                }
            }
            "ShaderNodeMixShader", "ShaderNodeAddShader" -> {
                // go both paths...
                for (link in findInputs(node, "Shader")) {
                    val fromNode = link.fromNode ?: continue
                    findData(fromNode)
                }
            }
            "ShaderNodeOutputMaterial" -> {
                val inputs = findInputs(node, "Surface")
                var nodes = 0
                for (link in inputs) {
                    val fromNode = link.fromNode ?: continue
                    findData(fromNode)
                    nodes++
                }
                if (nodes == 0) {
                    LOGGER.warn("No input node found :(")
                }
            }
            else -> LOGGER.warn("Unknown/unsupported shader node: ${node.type}")
            // ...
        }
    }
}