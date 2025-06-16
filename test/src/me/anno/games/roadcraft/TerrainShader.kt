package me.anno.games.roadcraft

import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.FileReference
import me.anno.utils.OS.pictures
import me.anno.utils.types.Booleans.hasFlag

/**
 * terrain,
 * using special shader for all materials;
 * materials are looked up using color attribute...
 * */
object TerrainShader : ECSMeshShader("Terrain") {

    val textureList: List<FileReference>

    init {

        val sandyTex = pictures.getChild("Textures/wavy-sand.jpg")
        val grassTex = pictures.getChild("Textures/grass2.jpg")
        val dirtTex = pictures.getChild("Textures/dirt2.webp")

        val sandTex = pictures.getChild("Textures/sand.avif")
        val gravelTex = pictures.getChild("Textures/gravel.jpg")
        val stoneTex = pictures.getChild("Textures/pattern-9.jpg")

        val roughTarmacTex = pictures.getChild("Textures/dark-tarmac.webp")
        val smoothTarmacTex = pictures.getChild("Textures/smooth-tarmac.jpg")
        val oldTarmacTex = pictures.getChild("Textures/hardened-dirt.jpg")

        textureList = listOf(
            sandyTex, dirtTex, grassTex,
            sandTex, gravelTex, stoneTex,
            roughTarmacTex, smoothTarmacTex, oldTarmacTex
        )
    }

    val material = Material().apply {
        shader = TerrainShader
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key) + listOf(
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.S2DA, "colorTextures"),
                ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        "uv = localPosition.xz * 0.1;\n" +

                        "vec4 vc = vertexColor0 * 5.0;\n" +
                        "float w = 0.0; vec3 color = vec3(0.0); vec2 rm = vec2(0.0,0.0);\n" +

                        // natural
                        "addTexture(0, vec2(1.0,0.0), texWeight(vc.xw,0,0), uv, color, rm, w);\n" + // sandy
                        "addTexture(1, vec2(1.0,0.0), texWeight(vc.xw,1,0), uv, color, rm, w);\n" + // dirt
                        "addTexture(2, vec2(1.0,0.0), texWeight(vc.xw,2,0), uv, color, rm, w);\n" + // grass

                        // before road
                        "addTexture(3, vec2(1.0,0.0), texWeight(vc.yw,0,1), uv, color, rm, w);\n" +
                        "addTexture(4, vec2(1.0,0.0), texWeight(vc.yw,1,1), uv, color, rm, w);\n" +
                        "addTexture(5, vec2(1.0,0.0), texWeight(vc.yw,2,1), uv, color, rm, w);\n" +

                        // actual road
                        "addTexture(6, vec2(1.0,0.0), texWeight(vc.zw,0,2), uv, color, rm, w);\n" +
                        "addTexture(7, vec2(0.0,0.1), texWeight(vc.zw,1,2), uv, color, rm, w);\n" +
                        "addTexture(8, vec2(1.0,0.0), texWeight(vc.zw,2,2), uv, color, rm, w);\n" +

                        "w = 1.0 / w;\n" +
                        "finalColor = color * w;\n" +
                        "finalRoughness = rm.x * w;\n" +
                        "finalMetallic = rm.y * w;\n" +

                        "finalAlpha = 1.0;\n" +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            normalTanBitanCalculation + v0 +
                                    reflectionCalculation
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity)
                .add(
                    "" +
                            "float texWeight(float x) {\n" +
                            "   return max(1.0 - abs(x), 0.0);\n" +
                            "}\n" +
                            "float texWeight(vec2 vc, int id, int group) {\n" +
                            "   return texWeight(vc.x-float(id)) * texWeight(vc.y-float(group));\n" +
                            "}\n" +
                            "void addTexture(int texId, vec2 matStats, float weight, vec2 uv, " +
                            "   inout vec3 colorSum, inout vec2 matStatsSum, inout float weightSum){\n" +
                            "   if (weight > 0.0) {\n" +
                            "       colorSum    += texture(colorTextures,vec3(uv,float(texId))).rgb * weight;\n" +
                            "       matStatsSum += matStats * weight;\n" +
                            "       weightSum   += weight;\n" +
                            "   }\n" +
                            "}\n"
                )
        )
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)

        val res = 1024
        val texture = TextureCache.getTextureArray(textureList, res, res)
        (texture ?: TextureLib.whiteTex2da).bind(shader, "colorTextures", Filtering.LINEAR, Clamping.REPEAT)
    }
}
