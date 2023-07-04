package me.anno.tests.mesh.hexagons

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.dither2x2
import me.anno.gpu.shader.ShaderLib.positionPostProcessing
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCPUCache
import me.anno.utils.OS.pictures

object HSMCShader : ECSMeshShader("hexagons") {

    val texture by lazy {
        val image = ImageCPUCache[pictures.getChild("atlas.webp"), false]!!
        val images = image.split(16, 1) // create stripes
        val texture = Texture2DArray("atlas", 16, 16, 256)
        texture.create(images, true)
        texture
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val ti = shader.getTextureIndex("diffuseMapStack")
        if (ti >= 0) texture.bind(ti, GPUFiltering.NEAREST, Clamping.REPEAT)
    }

    override fun createVertexStages(flags: Int): List<ShaderStage> {
        val defines = createDefines(flags)
        val variables = createVertexVariables(flags)
            .filter {
                when (it.name) {
                    "uvs", "tangents" -> false
                    else -> true
                }
            }
        return listOf(
            ShaderStage(
                "vertex",
                variables, defines.toString() +
                        "localPosition = coords;\n" + // is output, so no declaration needed
                        motionVectorInit +
                        instancedInitCode +
                        "#define tangents vec4(0,0,0,1)\n" +
                        normalInitCode +
                        // calculate uv from color to save memory and bandwidth
                        "#ifdef COLORS\n" +
                        "   int idx = int(colors0.r*65535.0+colors0.g*255.0+0.5);\n" +
                        "   vertexColor0 = colors0;\n" +
                        "   const vec2 uvArray[11] = vec2[](" +
                        "${uv6.joinToString { "vec2(${it.x},${1f - it.y})" }}, " +
                        "${uv5.joinToString { "vec2(${it.x},${1f - it.y})" }});\n" +
                        "   uv = idx < 11 ? uvArray[idx] : vec2(bool(idx&1) ? 0.0 : 0.5, -0.125 * float((idx-11)>>1));\n" +
                        "#endif\n" +
                        applyTransformCode +
                        // colorInitCode +
                        "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                        motionVectorCode +
                        positionPostProcessing
            )
        )
    }

    override fun createFragmentStages(flags: Int): List<ShaderStage> {
        return listOf(ShaderStage(
            "material",
            createFragmentVariables(flags).filter {
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
        ).add(dither2x2))
    }
}