package me.anno.experiments.trees

import me.anno.cache.FileCacheList
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.DitherMode
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.dither2x2
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.Color.black
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS.pictures
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.random.Random

/**
 * implement trees like https://www.youtube.com/watch?v=GOfttJQ-FGw
 *
 * -> this is the Temu version of it ;), all without stems or nice textures, but hey
 * */
fun main() {
    OfficialExtensions.initForTests()

    val scene = Entity()
    Entity("Bush[30,1]", scene)
        .add(MeshComponent(createBushMesh(30, Vector3f(1f), 65132)))

    Entity("Bush[100,2]", scene)
        .add(MeshComponent(createBushMesh(100, Vector3f(2f), 4891)))
        .setPosition(5.0, 0.0, 0.0)

    Entity("Bush[200,3]", scene)
        .add(MeshComponent(createBushMesh(200, Vector3f(3f), 48191)))
        .setPosition(10.0, 0.0, 0.0)

    testSceneWithUI("Trees", scene)
}

object LeavesShader : ECSMeshShader("Leaves") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + key.instanceData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key) + listOf(
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        jitterUVCorrection +

                        "vec4 color = vec4(vertexColor0.rgb, 1.0) * diffuseBase;\n" +
                        "color.a *= texture(diffuseMap, uv, lodBias).r;\n" +
                        "if (color.a < 0.5) discard;\n" +
                        "finalColor = color.rgb;\n" +
                        "finalAlpha = color.a;\n" +

                        "float sidedness = abs(dot(normalize(-finalPosition), normal));\n" +
                        "normal = normalize(localPosition);\n" +
                        "finalAlpha *= min(sidedness*5.0+0.2,1.0);\n" +
                        DitherMode.DITHER2X2.glslSnipped +

                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            createColorFragmentStage()
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity)
                .add(dither2x2)
        )
    }
}

val bushMaterialList = run {
    val material = Material()
    (0xaaff77 or black).toVecRGBA(material.diffuseBase)
    material.shader = LeavesShader
    material.diffuseMap = pictures.getChild("Textures/LeavesAlpha.jpg")
    material.cullMode = CullMode.BOTH
    material.pipelineStage = PipelineStage.GLASS
    FileCacheList.of(material)
}

fun createBushMesh(n: Int, scale: Vector3f, seed: Long): Mesh {
    val plane = DefaultAssets.plane
    val random = Random(seed)
    val mesh = object : MeshJoiner<Unit>(false, false, true) {
        val q = Quaternionf()
        override fun getMesh(element: Unit): Mesh = plane
        override fun getTransform(element: Unit, dst: Matrix4x3f) {
            q.rotationTo(
                0f, 1f, 0f,
                random.nextFloat() - 0.5f,
                random.nextFloat() - 0.5f,
                random.nextFloat() - 0.5f,
            )
            dst.translationRotate(
                (random.nextFloat() - 0.5f) * scale.x,
                (random.nextFloat() - 0.5f) * scale.y,
                (random.nextFloat() - 0.5f) * scale.z,
                q.x, q.y, q.z, q.w,
            )
        }
    }.join(List(n) {})
    mesh.cachedMaterials = bushMaterialList
    return mesh
}