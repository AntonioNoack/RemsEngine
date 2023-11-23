package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.Maths.hasFlag
import me.anno.utils.OS.documents
import org.joml.Vector4f

class ParallaxMaterial : Material() {
    init {
        shader = ParallaxRoomShader
    }

    var roomDepth = Vector4f(0.1f, 0.4f, 0.7f, 1f)
        set(value) {
            field.set(value)
        }

    override fun bind(shader: Shader) {
        super.bind(shader)
        shader.v4f("roomDepth", roomDepth)
    }
}

object ParallaxRoomShader : ECSMeshShader("ParallaxRoom") {
    // test alpha against front
    //  - if passes through, find collision with 2nd, 3rd and 4th wall
    //  - color is compressed HDR... -> emissive
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key) + listOf(Variable(GLSLType.V4F, "roomDepth")),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            "" +
                                    normalTanBitanCalculation +
                                    "vec4 color = texture(diffuseMap, vec2(0.0,2.0/3.0) + uv/3.0);\n" +
                                    // todo glass-like material/reflections, for the first layer of transparency
                                    "if(color.a < 1.0){\n" +
                                    // continue traversal
                                    "   vec3 pos = vec3(uv,0.0), pos0=pos;\n" +
                                    // find camera direction, and transform it into uv-space
                                    // todo find correct transform for rotated planes
                                    "   mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                                    "   vec3 dir = normalize(tbn * finalPosition);\n" +
                                    // find the closest hit
                                    "   float distX = dir.x < 0.0 ? -pos.x/dir.x : (1.0-pos.x)/dir.x;\n" +
                                    "   float distY = dir.y < 0.0 ? -pos.y/dir.y : (1.0-pos.y)/dir.y;\n" +
                                    "   float invZ = 1.0/dir.z;\n" +
                                    "   float distZ = roomDepth.x*invZ;\n" +
                                    "   if(distZ < min(distX,distY)){\n" +
                                    "       pos = pos0 + dir * distZ;\n" +
                                    "       color = blendColor(color, texture(diffuseMap, pos.xy/3.0));\n" +
                                    "   }\n" +
                                    "   distZ = roomDepth.y*invZ;\n" +
                                    "   if(color.a < 1.0 && distZ < min(distX,distY)){\n" +
                                    "       pos = pos0 + dir * distZ;\n" +
                                    "       color = blendColor(color, texture(diffuseMap, vec2(2.0/3.0,0.0) + pos.xy/3.0));\n" +
                                    "   }\n" +
                                    "   distZ = roomDepth.z*invZ;\n" +
                                    "   if(color.a < 1.0 && distZ < min(distX,distY)){\n" +
                                    "       pos = pos0 + dir * distZ;\n" +
                                    "       color = blendColor(color, texture(diffuseMap, vec2(2.0/3.0) + pos.xy/3.0));\n" +
                                    "   }\n" +
                                    "   distZ = roomDepth.w*invZ;\n" +
                                    "   if(color.a < 1.0){\n" +
                                    "       vec4 color1;\n" +
                                    "       if(distX < min(distY,distZ)){\n" +
                                    "           pos = pos0 + dir * distX;\n" +
                                    "           pos.z /= roomDepth.w;\n" +
                                    "           color1 = texture(diffuseMap, vec2(dir.x < 0.0 ? 0.0 : 2.0/3.0,1.0/3.0) + pos.zy/3.0);\n" +
                                    "       } else if(distY < distZ){\n" +
                                    "           pos = pos0 + dir * distY;\n" +
                                    "           pos.z /= roomDepth.w;\n" +
                                    "           color1 = texture(diffuseMap, vec2(1.0/3.0,dir.y < 0.0 ? 0.0 : 2.0/3.0) + pos.xz/3.0);\n" +
                                    "       } else {\n" +
                                    "           pos = pos0 + dir * distZ;\n" +
                                    "           color1 = texture(diffuseMap, vec2(1.0/3.0,1.0/3.0) + pos.xy/3.0);\n" +
                                    "       }\n" +
                                    "       color = blendColor(color,color1);\n" +
                                    "   }\n" +
                                    "}\n" +
                                    "finalColor = vec3(0.0);\n" +
                                    "finalAlpha = diffuseBase.a;\n" +
                                    // normalMapCalculation +
                                    // emissiveCalculation +
                                    "finalEmissive = color.rgb/(1.05-max(color.r,max(color.g,color.b)));\n" +
                                    // occlusionCalculation +
                                    metallicCalculation +
                                    roughnessCalculation +
                                    v0 + // sheenCalculation +
                                    // clearCoatCalculation +
                                    reflectionCalculation
                        } else "") +
                        finalMotionCalculation
            ).add(ShaderLib.quatRot).add(ShaderLib.brightness)
                // .add(ShaderLib.parallaxMapping)
                .add(ShaderLib.blendColor)
        )
    }
}

/**
 * create Parallax Room material like on
 *  - https://wparallax.com/
 *  - in some current Spider-Man game
 *  - in some Unreal Engine tutorials
 *
 *  the texture looks like a cubemap
 *  1. +y 2.
 *  -x 4. +x
 *  0. -y 3.
 *  0.-4. are the wall-aligned layers, which are assigned roomDepth.0xyzw
 * */
fun main() {
    // todo create a mesh with lots of planes...
    // (skyscraper ideally)
    val testMesh = PlaneModel.createPlane()
    testMesh.material = ParallaxMaterial().apply {
        diffuseMap = documents.getChild("ParallaxRoom.png")
        roomDepth.set(0.05f, 0.2f, 0.5f, 1f)
    }.ref
    val entity = Entity(MeshComponent(testMesh))
    testSceneWithUI("Parallax Room", entity)
}