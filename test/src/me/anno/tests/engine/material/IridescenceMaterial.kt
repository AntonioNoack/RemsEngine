package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.documents
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3f

class IridescenceMaterial : Material() {

    @Range(0.0, 1.0)
    var iriStrength = Vector3f(0.25f)
    var iriSpeed = Vector3f(7f, 0f, 4f)

    init {
        shader = IridescenceShader
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        shader.v3f("iriSpeed", iriSpeed)
        shader.v3f("iriStrength", iriStrength)
    }
}

object IridescenceShader : ECSMeshShader("Iridescence") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key) + listOf(
                    Variable(GLSLType.V3F, "iriSpeed"),
                    Variable(GLSLType.V3F, "iriStrength")
                ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        baseColorCalculation +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            "" +
                                    normalTanBitanCalculation +
                                    normalMapCalculation +
                                    v0 +
                                    "float fresnel = 1.0 - abs(dot(finalNormal,V0));\n" +
                                    "finalColor *= 1.0 + iriStrength * (sin(sqrt(fresnel) * iriSpeed)*0.5-0.5);\n" +
                                    emissiveCalculation +
                                    occlusionCalculation +
                                    metallicCalculation +
                                    roughnessCalculation +
                                    sheenCalculation +
                                    clearCoatCalculation +
                                    reflectionCalculation
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity)
        )
    }
}

// add optional iridescence parameter for shading ... it looks really nice on leather and metal :)
// https://belcour.github.io/blog/research/publication/2017/05/01/brdf-thin-film.html
// source code it at the top of the page
// -> or even better, make this rendering part here modular, so you can use any parameters and materials, you want

/**
 * testing a very simple iridescence shader (oily look caused by interference)
 * */
fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity("Scene")
    val golden = IridescenceMaterial()
    0xf5ba6c.withAlpha(1f).toVecRGBA(golden.diffuseBase)
    golden.roughnessMinMax.set(0.2f)
    golden.metallicMinMax.set(1f)
    val monkey = documents.getChild("monkey.obj")
    scene.add(
        Entity("Golden Monkey")
            .add(MeshComponent(monkey, golden))
    )
    val glass = IridescenceMaterial()
    glass.pipelineStage = PipelineStage.GLASS
    glass.roughnessMinMax.set(0.2f)
    glass.metallicMinMax.set(1f)
    scene.add(
        Entity("Glass Monkey")
            .setPosition(3.0, 0.0, 0.0)
            .add(MeshComponent(monkey, glass))
    )
    testSceneWithUI("Iridescence", scene)
}