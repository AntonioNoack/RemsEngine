package me.anno.ecs.components.mesh.decal

import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.ecs.components.mesh.MeshVertexData
import me.anno.ecs.components.mesh.decal.DecalMaterial.Companion.sett
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GLSLType.Companion.floats
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.structures.lists.Lists.any2
import java.util.*

class DecalShader(val modifiedLayers: ArrayList<DeferredLayerType>) : ECSMeshShader("decal") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val sett = sett
        val availableLayers = sett?.layers?.toHashSet() ?: emptySet()
        val availableLayers2 = sett?.layers2?.toHashSet() ?: emptySet()
        GFXState.currentRenderer
        val availableLayerTypes = availableLayers.map { it.type }.toHashSet()
        val variables = ArrayList(depthVars)
        variables.addAll(
            listOf(
                Variable(GLSLType.S2D, "depth_in0"),
                Variable(GLSLType.M4x3, "invLocalTransform"),
                Variable(GLSLType.V2F, "windowSize"),
                Variable(GLSLType.V4F, "decalSharpness"),
                Variable(GLSLType.V2F, "uv", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalNormal", VariableMode.INOUT),
                Variable(GLSLType.V3F, "finalTangent", VariableMode.INOUT),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                Variable(GLSLType.V1F, "alphaMultiplier", VariableMode.OUT),
            )
        )
        for (layer in availableLayers2) {
            variables.add(Variable(GLSLType.S2D, layer.name + "_in0"))
        }
        for (layer in availableLayers) {
            variables.add(Variable(floats[layer.type.workDims - 1], "${layer.type.glslName}_in2", VariableMode.OUT))
        }
        val originalStage = super.createFragmentStages(key)
        // can a decal modify the depth? it shouldn't ...
        return listOf(
            // inputs
            ShaderStage(
                "inputs", variables, "" +
                        "ivec2 uvz = ivec2(gl_FragCoord.xy);\n" +
                        // load all textures
                        availableLayers2.joinToString("") {
                            "vec4 ${it.name}_in1 = texelFetch(${it.name}_in0, uvz, 0);\n"
                        } +
                        // map textures to deferred layers
                        availableLayers.joinToString("") {
                            val tmp = StringBuilder()
                            it.appendMapping(tmp, "_in2", "_in1", "_in0", "", null, null)
                            tmp.toString()
                        } +
                        "uv = gl_FragCoord.xy/windowSize;\n" +
                        "finalPosition = rawDepthToPosition(uv, texelFetch(depth_in0, uvz, 0).x);\n" +
                        (if (DeferredLayerType.NORMAL !in availableLayerTypes) "" +
                                // reconstruct finalNormal_in2 from depth_in0
                                // we could save a bit here by first subtracting, and then applying the camera rotation
                                // todo why is normal applied by 100% everywhere? (in NORMAL-debug mode)
                                "vec3 pos0 = finalPosition;\n" +
                                "vec3 posU = rawDepthToPosition(uv+vec2(1.0/windowSize.x,0.0), texelFetch(depth_in0, uvz+ivec2(1,0), 0).x);\n" +
                                "vec3 posV = rawDepthToPosition(uv+vec2(0.0,1.0/windowSize.y), texelFetch(depth_in0, uvz+ivec2(0,1), 0).x);\n" +
                                "vec3 finalNormal_in2 = normalize(cross(posU-pos0, posV-pos0));\n" else "") +
                        "localPosition = matMul(invLocalTransform, vec4(finalPosition, 1.0));\n" +
                        // automatic blending on edges? alpha should be zero there anyway
                        "vec3 alphaMultiplier0 = clamp(decalSharpness.xyz * (1.0-abs(localPosition)), vec3(0.0), vec3(1.0));\n" +
                        "alphaMultiplier = alphaMultiplier0.x * alphaMultiplier0.y * alphaMultiplier0.z;\n" +
                        "alphaMultiplier *= clamp(-decalSharpness.w * dot(normal, finalNormal_in2), 0.0, 1.0);\n" +
                        "if(alphaMultiplier < 0.5/255.0) discard;\n" +
                        "uv = localPosition.xy * vec2(0.5,-0.5) + 0.5;\n" +
                        // prepare pseudo-vertex outputs for original stage
                        "normal = -finalNormal_in2;\n" +
                        // calculate tangent based on uv and normal
                        // is this correct? looks like it is, based on rotating an example a bit
                        "vec3 tan3 = dFdx(uv.x) * dFdx(finalPosition) + dFdx(uv.y) * dFdy(finalPosition);\n" +
                        "tan3 -= dot(tan3, normal) * normal;\n" +
                        "tangent = vec4(normalize(tan3), 1.0);\n" +
                        // fix for sometimes tangent being NaN on the edge
                        "if(any(isnan(tangent.xyz))) tangent = vec4(0,1,0,0);\n" +
                        ""
            ).add(quatRot).add(rawToDepth).add(depthToPosition).add(ShaderLib.octNormalPacking),
        ) + originalStage + listOf(
            ShaderStage(
                "effect-modulator", listOf(
                    Variable(GLSLType.V1F, "alphaMultiplier", VariableMode.INOUT),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.INOUT)
                ), "" +
                        "finalAlpha *= alphaMultiplier;\n" +
                        "if(finalAlpha < 0.5/255.0) discard;\n" +
                        colorToLinear +
                        // blend all values with the loaded properties
                        modifiedLayers.filter { it in availableLayerTypes }.joinToString("") {
                            // gamma correction for color
                            "" + (if (it == DeferredLayerType.COLOR || it == DeferredLayerType.EMISSIVE)
                                "${it.glslName}_in2 = pow(${it.glslName}_in2,vec3(2.2));\n" else "") +
                                    "${it.glslName} = mix(${it.glslName}_in2, ${it.glslName}, finalAlpha);\n"
                        } +
                        // for all other values, override them completely with the loaded values
                        availableLayerTypes
                            .filter { type ->
                                type !in modifiedLayers && originalStage.any2 { stage ->
                                    stage.variables.any2 { variable ->
                                        variable.name == type.glslName
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
        for (layer in modifiedLayers) {
            val layer1 = settings.findLayer(layer) ?: continue
            disabled.set(layer1.texIndex, false)
        }
        return disabled
    }

    override fun createDeferredShader(key: ShaderKey): Shader {
        val base = createBase(key)
        base.outputs = key.renderer.deferredSettings
        base.disabledLayers = getDisabledLayers()
        // build & finish
        val shader = base.create("dcl${key.flags}")
        finish(shader)
        return shader
    }
}