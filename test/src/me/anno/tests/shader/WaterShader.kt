package me.anno.tests.shader

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.shaders.AutoTileableShader
import me.anno.ecs.components.mesh.material.shaders.AutoTileableShader.getTextureNoLUT
import me.anno.ecs.components.mesh.material.shaders.AutoTileableShader.sampleTileNoLUT
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.transparency.GlassPass.Companion.glassPassDepth
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.anisotropic16
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS.pictures
import me.anno.utils.types.Booleans.hasFlag
import kotlin.math.PI

// todo implement water shader like
//  https://www.youtube.com/watch?v=-u3gEkhc8co
//  https://github.com/zulubo/VWater
// -> currently, this is extremely bare-bones

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
                    Variable(GLSLType.V2F, "movingUV"),
                    Variable(GLSLType.V1F, "worldScale"),
                    Variable(GLSLType.V3F, "cameraPosition"),
                    Variable(GLSLType.V2F, "uv", VariableMode.INMOD),
                    Variable(GLSLType.V4F, "tangent", VariableMode.INMOD)
                ) + AutoTileableShader.tilingVars, concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        baseColorCalculation +
                        "ivec2 uvi = ivec2(gl_FragCoord.xy);\n" +
                        "float backgroundDepth = rawToDepth(texelFetch(depthTexture,uvi,0).x);\n" +
                        "vec3 camDir = quatRot(vec3(0.0,0.0,-1.0),d_camRot);\n" +
                        "float deltaDepth = (backgroundDepth - rawToDepth(gl_FragCoord.z)) / (0.5 + abs(camDir.z));\n" +
                        "finalRoughness = 0.01;\n" +
                        "finalColor = diffuseBase.rgb;\n" +
                        "finalAlpha = diffuseBase.a;\n" +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            "" +
                                    "uv = 0.003 * (finalPosition / worldScale + cameraPosition).xz;\n" +
                                    "tangent = vec4(1.0,0.0,0.0,1.0);\n" + // good like that? yes, tangent = u = x
                                    normalTanBitanCalculation +
                                    // normal mapping
                                    "mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                                    "vec3 rawColor = mix(\n" +
                                    "   sampleAutoTileableTextureNoLUT(normalMap, uv + movingUV).xyz,\n" +
                                    "   sampleAutoTileableTextureNoLUT(normalMap, uv * 5.7 - movingUV * 3.7).xyz,\n" +
                                    "   vec3(0.3)\n" +
                                    ") * 2.0 - 1.0;\n" +
                                    "vec3 normalFromTex = matMul(tbn, rawColor);\n" +
                                    "finalNormal = mix(finalNormal, normalFromTex, normalStrength.x);\n" +

                                    "float t = deltaDepth * absorption + rawColor.x;\n" +
                                    "finalRoughness = clamp((1.0 - 5.0 * t) * (cos(t*30.0)*0.5+0.5), 0.0, 1.0);\n" +
                                    "finalMetallic = 1.0;\n" +
                                    v0 + reflectionCalculation
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity).add(rawToDepth)
                .add(sampleTileNoLUT).add(getTextureNoLUT)
                .add(anisotropic16).add(randomGLSL)
        )
    }

    fun bindDepthTexture(shader: Shader) {
        var depthTex = glassPassDepth ?: TextureLib.depthTexture
        if (depthTex.samples > 1) {
            // we need single-sampled depth when using MSAA
            val tmp = FBStack["waterDepth", depthTex.width, depthTex.height, 1, true, 1, DepthBufferType.NONE]
            useFrame(tmp) {
                renderPurely {
                    GFX.copyNoAlpha(depthTex)
                }
            }
            depthTex = tmp.getTexture0()
            shader.use() // rebind shader
        }
        depthTex.bindTrulyNearest(shader, "depthTexture")
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        DepthTransforms.bindDepthUniforms(shader)
        bindDepthTexture(shader)
        shader.v1f("worldScale", RenderState.worldScale)
        shader.v3f("cameraPosition", RenderState.cameraPosition)
        shader.v1f("absorption", 0.05f / RenderState.worldScale)
        shader.v2f("movingUV", Time.gameTime.toFloat() * 0.005f, 0f)
        shader.v1b("anisotropic", true)
        // could be customizable, but who would use that?
        shader.m2x2("latToWorld", AutoTileableShader.latToWorld)
        shader.m2x2("worldToLat", AutoTileableShader.worldToLat)
    }
}

fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity()
    scene.add(MeshComponent(getReference("res://meshes/NavMesh.fbx")))
    val material = Material().apply {
        pipelineStage = PipelineStage.TRANSPARENT
        normalMap = pictures.getChild("normal waves.jpg")
        indexOfRefraction = 1f
        shader = WaterShader
    }
    scene.add(
        Entity()
            .add(MeshComponent(DefaultAssets.plane, material))
            .add(
                Entity()
                    .add(PlanarReflection())
                    // todo why is that offset against z-fighting needed?
                    .setPosition(0.0, 1e-4, 0.0)
                    .setRotation(PI / 2, 0.0, 0.0)
            )
            .setPosition(0.0, 10.0, 0.0)
            .setScale(15000.0)
    )
    testSceneWithUI("Water", scene)
}