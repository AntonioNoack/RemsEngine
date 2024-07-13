package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.transparency.GlassPass.Companion.glassPassDepth
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.Reference.getReference
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.OS.pictures
import me.anno.utils.types.Booleans.hasFlag

// todo implement water shader like
//  https://www.youtube.com/watch?v=-u3gEkhc8co
//  https://github.com/zulubo/VWater
// -> currently, this is extremely bare-bones

// todo we'd kind of need our own pass for this, because glass-fresnel is already adding stuff that I don't necessarily want...

// todo NaN issue: when looking from above, there is a few NaN/black spots,
//  where are they coming from?

object WaterShader : ECSMeshShader("Water") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key) +
                        DepthTransforms.depthVars + listOf(
                    Variable(GLSLType.S2D, "depthTexture"),
                    Variable(GLSLType.V1F, "absorption"),
                ), concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        baseColorCalculation +
                        "ivec2 uv2 = ivec2(gl_FragCoord.xy);\n" +
                        "float backgroundDepth = rawToDepth(texelFetch(depthTexture,uv2,0).x);\n" +
                        "vec3 camDir = quatRot(vec3(0.0,0.0,-1.0),d_camRot);\n" +
                        "float deltaDepth = (backgroundDepth - rawToDepth(gl_FragCoord.z)) / (0.5 + abs(camDir.z));\n" +
                        "finalRoughness = 0.01;\n" +
                        "finalColor = mix(diffuseBase.rgb,vec3(1.0),exp(-deltaDepth * absorption));\n" +
                        "finalAlpha = diffuseBase.a;\n" +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            "" +
                                    normalTanBitanCalculation +
                                    normalMapCalculation +
                                    "finalMetallic = 1.0;\n" +
                                    v0 + reflectionCalculation +
                                    "finalRoughness = 1.0;\n"
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity).add(rawToDepth)
        )
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        DepthTransforms.bindDepthUniforms(shader)
        (glassPassDepth ?: TextureLib.depthTexture).bindTrulyNearest(shader, "depthTexture")
        shader.v1f("absorption", 0.05f / RenderState.worldScale)
    }
}

fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity()
    scene.add(MeshComponent(getReference("res://meshes/NavMesh.fbx")))
    val material = Material().apply {
        pipelineStage = PipelineStage.TRANSPARENT
        diffuseBase.set(HSLuv.toRGB(0.703, 0.795, 0.430, 1.0))
        normalMap = pictures.getChild("normal waves.jpg")
        roughnessMinMax.set(0.02f)
        indexOfRefraction = 1f
        shader = WaterShader
    }
    scene.add(
        Entity()
            .add(MeshComponent(DefaultAssets.plane, material))
            .setPosition(0.0, 10.0, 0.0)
            .setScale(50.0)
    )
    testSceneWithUI("Water", scene)
}