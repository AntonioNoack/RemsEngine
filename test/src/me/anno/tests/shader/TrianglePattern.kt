package me.anno.tests.shader

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures

fun main() {
    // given a triangle, and uv coordinates of a triangle,
    //  draw a pixelated image such that it appears to have triangular pixels
    val mesh = MeshComponent(documents.getChild("triangle.obj"))
    mesh.materials = listOf(Material().apply {
        diffuseMap = pictures.getChild("Textures/grass.jpg")
        linearFiltering = false
        shader = object : ECSMeshShader("triangular-pixels") {
            override fun createFragmentStages(flags: Int): List<ShaderStage> {
                return listOf(
                    ShaderStage(
                        "material",
                        createFragmentVariables(flags),
                        discardByCullingPlane +
                                // step by step define all material properties
                                // baseColorCalculation +
                                "vec2  uv1 = fract(uv) * textureSize(diffuseMap,0);\n" +
                                "ivec2 uv2 = ivec2(uv1);\n" +
                                "if(dot(fract(uv1),vec2(1.0))>=1.0) uv2.x = (uv2.x + 8) % textureSize(diffuseMap,0).x;\n" +
                                "vec4 texDiffuseMap = texelFetch(diffuseMap, uv2, 0);\n" +
                                "vec4 color = vec4(vertexColor0.rgb, 1.0) * diffuseBase * texDiffuseMap;\n" +
                                "if(color.a < ${1f / 255f}) discard;\n" +
                                "finalColor = color.rgb;\n" +
                                "finalAlpha = color.a;\n" +
                                normalTanBitanCalculation +
                                // normalMapCalculation +
                                // emissiveCalculation +
                                // occlusionCalculation +
                                // metallicCalculation +
                                // roughnessCalculation +
                                reflectionPlaneCalculation +
                                // v0 + sheenCalculation +
                                // clearCoatCalculation +
                                (if (motionVectors) finalMotionCalculation else "")
                    )
                )
            }
        }
    }.ref)
    testSceneWithUI(mesh)
}