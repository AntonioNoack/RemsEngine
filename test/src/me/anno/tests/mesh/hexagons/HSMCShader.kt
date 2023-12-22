package me.anno.tests.mesh.hexagons

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.dither2x2
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib.whiteTex2da
import me.anno.image.ImageCache
import me.anno.utils.OS.pictures
import org.apache.logging.log4j.LogManager

object HSMCShader : ECSMeshShader("hexagons") {

    private val LOGGER = LogManager.getLogger(HSMCShader::class)

    val texture by lazy {
        val source = pictures.getChild("atlas.webp")
        val image = ImageCache[source, false]
        if (image == null) {
            LOGGER.warn("Missing $source")
            whiteTex2da
        } else {
            val images = image.split(16, 1) // create stripes
            val texture = Texture2DArray("atlas", 16, 16, 256)
            texture.create(images, true)
            texture
        }
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val ti = shader.getTextureIndex("diffuseMapStack")
        if (ti >= 0) texture.bind(ti, Filtering.NEAREST, Clamping.REPEAT)
    }

    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        val colorOverrideStage = ShaderStage(
            "vertex",
            listOf(
                Variable(GLSLType.V4F, "colors0"),
                Variable(GLSLType.V4F, "vertexColor0", VariableMode.OUT),
                Variable(GLSLType.V2F, "uv", VariableMode.OUT)
            ), "" +
                    // calculate uv from color to save memory and bandwidth
                    "#ifdef COLORS\n" +
                    "   int idx = int(colors0.r*65535.0+colors0.g*255.0+0.5);\n" +
                    "   vertexColor0 = colors0;\n" +
                    "   const vec2 uvArray[11] = vec2[](" +
                    "${uv6.joinToString { "vec2(${it.x},${1f - it.y})" }}, " +
                    "${uv5.joinToString { "vec2(${it.x},${1f - it.y})" }});\n" +
                    "   uv = idx < 11 ? uvArray[idx] : vec2(bool(idx&1) ? 0.0 : 0.5, -0.125 * float((idx-11)>>1));\n" +
                    "#endif\n"
        )
        return createDefines(key) +
                loadVertex(key) +
                colorOverrideStage +
                transformVertex(key) +
                finishVertex(key)
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        // return super.createFragmentStages(key)
        return listOf(ShaderStage(
            "material",
            createFragmentVariables(key).filter {
                when (it.name) {
                    "emissiveMap", "diffuseMap", "normalMap", "metallicMap",
                    "roughnessMap" -> false
                    else -> true
                }
            } + listOf(
                Variable(GLSLType.S2DA, "diffuseMapStack"),
                Variable(GLSLType.V3F, "localPosition"),
            ),
            discardByCullingPlane +
                    // step by step define all material properties
                    // baseColorCalculation +
                    "ivec3 ts = textureSize(diffuseMapStack,0);\n" +
                    "float uvW = int(vertexColor0.b*255.0+0.5);\n" +
                    "vec4 color;\n" +
                    // todo optional edge smoothing like in Rem's Studio
                    "bool up = abs(dot(normal,normalize(localPosition)))>0.9;\n" +
                    "if(dot(abs(vec4(dFdx(uv),dFdy(uv))),vec4(1.0)) * ts.x < 1.41) {\n" +
                    "   vec2 uv3 = uv + dFdx(uv) * (gl_SamplePosition.x-0.5) + dFdy(uv) * (gl_SamplePosition.y-0.5);\n" +
                    "   ivec2 uv2;\n" +
                    "   if(up) {\n" +
                    // looks great :3, but now the quads are too large, even if they match
                    "      vec2 uv1 = fract(uv3) * ts.x;\n" +
                    "      uv2 = ivec2(uv1);\n" +
                    "      if(dot(fract(uv1),vec2(1.0))>=1.0) uv2.x = (uv2.x + (ts.x>>1)) % ts.x;\n" +
                    "   } else {\n" +
                    "      vec2 uv1 = fract(uv3) * ts.xy;\n" +
                    "      vec2 uvl = mod(uv1, 2.0) - 1.0;\n" +
                    "      if(abs(uvl.x)+abs(uvl.y)>=1.0) uv1 -= 2.0 * uvl;\n" +
                    "      uv2 = ivec2(uv1);\n" +
                    "   }\n" +
                    "   color = texelFetch(diffuseMapStack, ivec3(uv2, uvW), 0);\n" +
                    "} else if(up) {\n" +
                    // anisotropic filtering or similar for the distance ^^
                    "   color = (texture(diffuseMapStack,vec3(uv,uvW)) + texture(diffuseMapStack,vec3(uv+vec2(0.5,0.0),uvW))) * 0.5;\n" +
                    "} else {\n" +
                    "   color = texture(diffuseMapStack,vec3(uv,uvW));\n" +
                    "}\n" +
                    "if(dither2x2(color.a)) discard;\n" +
                    "finalColor = color.rgb;\n" +
                    "finalAlpha = 1.0;//color.a;\n" +
                    normalTanBitanCalculation +
                    // normalMapCalculation +
                    // emissiveCalculation +
                    // occlusionCalculation +
                    // metallicCalculation +
                    // roughnessCalculation +
                    // v0 + sheenCalculation +
                    // clearCoatCalculation +
                    reflectionCalculation +
                    finalMotionCalculation
        ).add(dither2x2))
    }
}