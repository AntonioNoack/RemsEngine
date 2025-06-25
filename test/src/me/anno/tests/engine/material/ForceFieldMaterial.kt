package me.anno.tests.engine.material

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.PlaneModel.createPlane
import me.anno.ecs.components.mesh.shapes.UVSphereModel.createUVSphere
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.GLASS_PASS
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.input.Input
import me.anno.maths.Maths.mix
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures
import org.joml.Vector3f
import kotlin.math.sin

object ForceFieldShader : ECSMeshShader("ForceField") {

    fun findDepthTexture(fb: IFramebuffer): IFramebuffer? {
        val tx0 = fb.depthTexture
        if (tx0 != null) return fb
        val da = (fb as? Framebuffer)?.depthAttachment
        if (da != null) return findDepthTexture(da)
        return null
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        shader.v1f("uvScroll", (Time.gameTime * 0.17) % 1.0)
        bindDepthUniforms(shader)
        var depth = findDepthTexture(GFXState.framebuffer.currentValue)
        if (depth == null) println("no depth was found!")
        if (Input.isShiftDown && depth is Framebuffer) { // todo we probably need to do this in some environments, don't we?
            val tmp = FBStack["depth", depth.width, depth.height, 0, true, 1, DepthBufferType.TEXTURE]
            depth.copyTo(tmp, false, true)
            depth = tmp
        }
        (depth?.depthTexture ?: depthTexture).bindTrulyNearest(shader, "depthTex")
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key) + listOf(
                    Variable(GLSLType.S2D, "depthTex"),
                    Variable(GLSLType.V1F, "uvScroll")
                ) + depthVars,
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        "float alpha0 = texture(diffuseMap, vec2(16.0,8.0) * uv + uvScroll).x;\n" +
                        "alpha0 = 1.0 - pow(alpha0,2.0);\n" +
                        "finalColor = alpha0 * diffuseBase.xyz;\n" +
                        "finalAlpha = diffuseBase.a * alpha0 * 0.99 + 0.01;\n" +
                        "finalEmissive = finalColor * emissiveBase;\n" +
                        normalTanBitanCalculation +
                        normalMapCalculation +
                        v0 +
                        "float fresnel = pow(1.0 - abs(dot(V0,finalNormal)), 3.0);\n" +
                        "ivec2 depthUV = ivec2(gl_FragCoord.xy);\n" +
                        "float sampledDepth = rawToDepth(texelFetch(depthTex,depthUV,0).x);\n" +
                        "float zDistance = 1.0 / gl_FragCoord.w;\n" +
                        "float distanceToSurface = abs(sampledDepth - zDistance);\n" +
                        "float betterFresnel = fresnel + max(1.0-4.0*distanceToSurface, 0.0);\n" +
                        "betterFresnel = min(pow(betterFresnel,3.0), 1.0);\n" +
                        "finalAlpha *= betterFresnel;\n" +
                        // emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        // sheenCalculation +
                        // clearCoatCalculation +
                        reflectionCalculation +
                        finalMotionCalculation
            ).add(ShaderLib.brightness).add(rawToDepth).add(RendererLib.getReflectivity)
        )
    }
}

class ForceFieldMaterial : Material() {
    init {
        pipelineStage = GLASS_PASS
        // roughnessMinMax.set(0.01f)
        // metallicMinMax.set(1f)
        diffuseMap = pictures.getChild("HexagonalGrid2.png")
        diffuseBase.set(0.3f, .9f, 1f)
        emissiveBase.set(60f)
        shader = ForceFieldShader
    }

    override fun bind(shader: GPUShader) {
        emissiveBase.set(60f * mix(1f, 1.05f, sin(1.5 * Time.gameTime).toFloat()))
        super.bind(shader)
    }
}

/**
 * Force field shader like in Brackeys' tutorial for Unity https://www.youtube.com/watch?v=NiOGWZXBg4Y
 *
 * todo water shader based on same principles for foam
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity("Scene")
    val planeMat = Material.diffuse(0x191919)
    val floorMesh = createPlane(
        1, 5, Vector3f(0f, 0.2f, 0f),
        Vector3f(5f, 0f, 0f), Vector3f(0f, 0f, 5f)
    ).apply {
        cullMode = CullMode.BOTH
    }
    Entity("Floor", scene)
        .add(MeshComponent(floorMesh, planeMat))

    val wallMesh = createPlane(
        1, 5, Vector3f(0f, 0.2f, -0.3f),
        Vector3f(3f, 0f, 0f), Vector3f(0f, 3f, 0f)
    ).apply {
        cullMode = CullMode.BOTH
    }
    Entity("Wall", scene)
        .add(MeshComponent(wallMesh, planeMat))

    val forceFieldMaterial = ForceFieldMaterial()
    Entity("ForceField", scene)
        .add(MeshComponent(createUVSphere(30, 30), forceFieldMaterial))
        .setPosition(+1.0, 0.0, 0.0)
    Entity("Monkey", scene)
        .add(MeshComponent(documents.getChild("monkey.obj"), forceFieldMaterial))
        .setPosition(-1.3, 1.0, 0.0)
    scene.add(Skybox())
    testSceneWithUI("Force Field", scene)
}