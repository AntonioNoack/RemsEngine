package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXBase
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ReverseDepth.bindDepthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPositionList
import me.anno.gpu.shader.ReverseDepth.rawToDepth
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.hasFlag
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.OS.pictures
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import java.util.*

// decal pass:
//  Input: pos, normal (we could pass in color theoretically, but idk)
//  Output: new color, new normal, new emissive
// todo different blend modes: additive, subtractive, default, ...
class DecalMaterial : Material() {

    companion object {

        private val shaderLib = HashMap<Int, ECSMeshShader>()
        private val sett get() = GFXState.currentRenderer.deferredSettings

        @JvmStatic
        fun main(args: Array<String>) {
            GFXBase.forceLoadRenderDoc()
            val scene = Entity("Scene")
            val decal = Entity("Decal")
            decal.scale = decal.scale.set(1.0, 1.0, 0.1)
            decal.position = decal.position.set(0.0, 0.0, 1.0)
            val decalMesh = MeshComponent((flatCube.front.clone() as Mesh).apply {
                ensureNorTanUVs()
                val pos = positions!!
                val nor = normals!!
                val uvs = uvs.resize(nor.size / 3 * 2)
                val tan = tangents.resize(nor.size)
                nor.fill(0f)
                tan.fill(0f)
                var j = 0
                for (i in nor.indices step 3) {
                    nor[i + 2] = -1f
                    tan[i] = 1f
                    uvs[j++] = pos[i] * .5f + .5f
                    uvs[j++] = pos[i + 1] * .5f + .5f
                }
                this.uvs = uvs
                this.tangents = tan
            })
            scene.add(decal)
            decal.add(decalMesh)
            val mat = DecalMaterial()
            mat.linearFiltering = false
            mat.diffuseMap = pictures.getChild("fav128.png")
            // mat.emissiveMap = mat.diffuseMap
            mat.writeEmissive = true
            // mat.emissiveBase.set(20f)
            mat.normalMap = pictures.getChild("BricksNormal.png")
            mat.writeNormal = true
            decalMesh.materials = listOf(mat.ref)
            val baseMesh = Entity("Object")
            baseMesh.add(MeshComponent(flatCube.front))
            scene.add(baseMesh)
            testSceneWithUI(scene)
        }
    }

    class DecalShader(val layers: ArrayList<DeferredLayerType>) : ECSMeshShader("decal") {
        override fun createFragmentStages(
            isInstanced: Boolean,
            isAnimated: Boolean,
            motionVectors: Boolean
        ): List<ShaderStage> {
            val sett = sett ?: return super.createFragmentStages(isInstanced, isAnimated, motionVectors)
            val loadPart2 = StringBuilder()
            for (layer in sett.layers) {
                layer.appendMapping(loadPart2, "_in2", "_in1", "_in0", "", null, null)
            }
            val original = super.createFragmentStages(isInstanced, isAnimated, motionVectors)
            // can a decal modify the depth? it shouldn't ...
            return listOf(
                // inputs
                ShaderStage(
                    "inputs", depthToPositionList + sett.layers2.map { layer ->
                        Variable(GLSLType.S2D, layer.name + "_in0")
                    } + listOf(
                        Variable(GLSLType.S2D, "depth_in0"),
                        Variable(GLSLType.M4x3, "invLocalTransform"),
                        Variable(GLSLType.V2F, "windowSize"),
                        Variable(GLSLType.V2F, "uv", VariableMode.OUT),
                        Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                        Variable(GLSLType.V1F, "alphaMultiplier", VariableMode.OUT),
                    ), "" +
                            "ivec2 uvz = ivec2(gl_FragCoord.xy);\n" +
                            // load all data
                            sett.layers2.joinToString("") {
                                "vec4 ${it.name}_in1 = texelFetch(${it.name}_in0, uvz, 0);\n"
                            } + loadPart2.toString() +
                            "finalPosition = rawDepthToPosition(gl_FragCoord.xy/windowSize, texelFetch(depth_in0, uvz, 0).x);\n" +
                            "localPosition = invLocalTransform * vec4(finalPosition, 1.0);\n" +
                            "uv = localPosition.xy * vec2(0.5,-0.5) + 0.5;\n" +
                            // automatic blending on edges? alpha should be zero there anyway
                            "alphaMultiplier = abs(uv.x-0.5) < 0.5 && abs(uv.y-0.5) < 0.5 ? 1.0-abs(localPosition.z) : 0.0;\n" +
                            "if(!(alphaMultiplier > 0.5/255.0)) discard;\n" +
                            // override normal by surface?
                            ""
                ).add(quatRot).add(rawToDepth).add(depthToPosition),
            ) + original + listOf(
                ShaderStage(
                    "effect-modulator", listOf(
                        Variable(GLSLType.V1F, "alphaMultiplier", VariableMode.INOUT),
                        Variable(GLSLType.V1F, "finalAlpha", VariableMode.INOUT)
                    ), "" +
                            // factor for normal-alignment
                            "alphaMultiplier *= dot(finalNormal, finalNormal_in2);\n" +
                            "finalAlpha *= alphaMultiplier;\n" +
                            "if(!(finalAlpha > 0.5/255.0)) discard;\n" +
                            // blend all values with the loaded properties
                            layers.joinToString("") {
                                "${it.glslName} = mix(${it.glslName}_in2, ${it.glslName}, finalAlpha);\n"
                            } +
                            // for all other values, override them completely with the loaded values
                            sett.layerTypes
                                .filter {
                                    it !in layers && original.any2 { stage ->
                                        stage.variables.any2 { variable ->
                                            variable.name == it.glslName
                                        }
                                    }
                                }
                                .joinToString("") {
                                    "${it.glslName} = ${it.glslName}_in2;\n"
                                } +
                            "finalAlpha = 1.0;\n"
                )
            )
        }

        // forward shader isn't really supported

        fun getDisabledLayers(): BitSet? {
            val settings = sett ?: return null
            val disabled = BitSet(settings.layers2.size)
            for (i in settings.layers2.indices) disabled.set(i, true)
            for (layer in layers) {
                val layer1 = settings.findLayer(layer) ?: continue
                disabled.set(layer1.texIndex, false)
            }
            return disabled
        }

        override fun createDeferredShader(
            deferred: DeferredSettingsV2,
            isInstanced: Boolean,
            isAnimated: Boolean,
            motionVectors: Boolean,
            limitedTransform: Boolean,
            postProcessing: ShaderStage?
        ): Shader {
            val base = createBase(
                isInstanced, isAnimated,
                deferred.layerTypes.size > 1 || !motionVectors,
                motionVectors, limitedTransform, postProcessing
            )
            base.outputs = deferred
            base.disabledLayers = getDisabledLayers()
            // build & finish
            val shader = base.create()
            finish(shader)
            return shader
        }
    }

    // can we support this in forward rendering?
    // yes, but it will be a bit more expensive
    // a) list of all decals for all pixels -> bad
    // b) render normal extra; and apply lighting twice

    var writeColor = true
        set(value) {
            field = value
            shader = getShader()
        }

    var writeNormal = false
        set(value) {
            field = value
            shader = getShader()
        }

    var writeEmissive = false
        set(value) {
            field = value
            shader = getShader()
        }

    var writeRoughness = false
        set(value) {
            field = value
            shader = getShader()
        }

    var writeMetallic = false
        set(value) {
            field = value
            shader = getShader()
        }

    init {
        pipelineStage = 2
        shader = getShader()
    }

    override fun bind(shader: Shader) {
        super.bind(shader)
        bindDepthToPosition(shader)
        // bind textures
        val buff = GFXState.currentBuffer
        val sett = sett
        if (sett != null) {
            val layers = sett.layers2
            for (index in layers.indices) {
                buff.getTextureI(index).bindTrulyNearest(shader, layers[index].name + "_in0")
            }
        }
        shader.v2f("windowSize", buff.w.toFloat(), buff.h.toFloat())
        buff.depthTexture?.bindTrulyNearest(shader, "depth_in0")
    }

    fun getShader(): ECSMeshShader {
        val flags = writeColor.toInt() + writeNormal.toInt(2) +
                writeEmissive.toInt(4) + writeRoughness.toInt(8) + writeMetallic.toInt(16)
        return shaderLib.getOrPut(flags) {
            val layers = ArrayList<DeferredLayerType>(5)
            if (flags.hasFlag(1)) layers.add(DeferredLayerType.COLOR)
            if (flags.hasFlag(2)) layers.add(DeferredLayerType.NORMAL)
            if (flags.hasFlag(4)) layers.add(DeferredLayerType.EMISSIVE)
            if (flags.hasFlag(8)) layers.add(DeferredLayerType.ROUGHNESS)
            if (flags.hasFlag(16)) layers.add(DeferredLayerType.METALLIC)
            DecalShader(layers)
        }
    }

    override val className get() = "DecalMaterial"

}