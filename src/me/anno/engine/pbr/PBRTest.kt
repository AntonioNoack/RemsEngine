package me.anno.engine.pbr

import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2End
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Reduction
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.utils.OS.desktop
import me.anno.utils.types.Vectors.print
import java.util.*

// the BRDF model is sometimes awkward -> implementation test

private val testShader = Shader(
    "brdf-baseline", coordsList, coordsVShader, uvList,
    listOf(
        Variable(GLSLType.S2D, "randomTex"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT)
    ), "" +
            "const float M_PI = 3.141592653589793;\n" +
            "void main(){\n" +
            // generate baseColor, metallic, roughness, VNL randomly
            "   vec4 data0 = texture(randomTex, uv);\n" +
            "   vec3 finalColor = vec3(1);//data0.rgb;\n" +
            "   float NdotH = data0.r;\n" + // error does not depend on this
            "   float NdotV = data0.g;\n" + // todo error depends on this
            "   float NdotL = data0.b;\n" + // todo error depends on this
            "   float HdotV = data0.a;\n" + // error does not depend on this
            "   float finalRoughness = uv.x;\n" +
            "   float finalMetallic = uv.y;\n" + // todo error does depend on this
            "#define DOTS\n" +
            // calculate H and result
            "   vec3 diffuseColor  = finalColor * (1.0 - finalMetallic);\n" +
            "   vec3 specularColor = finalColor * finalMetallic;\n" +
            // our engine, todo which is wrong
            "   vec3 ambientLight = vec3(0.0);\n" +
            "   vec3 effectiveSpecular = vec3(1.0), effectiveDiffuse = vec3(1.0);\n" + // light strength
            specularBRDFv2NoDivInlined2Start +
            specularBRDFv2NoDivInlined2 +
            "   vec3 specularLight = computeSpecularBRDF;\n" +
            specularBRDFv2NoDivInlined2End +
            "   vec3 engineColor = specularLight;\n" +
            // baseline
            "   float alpha9 = finalRoughness * finalRoughness;\n" +
            "   float Dx9 = alpha9 * alpha9;\n" +
            "   float NdotH_squared9 = NdotH * NdotH;\n" +
            "   float x9 = NdotH_squared9 * (Dx9 - 1.0) + 1.0;\n" + // done
            "   vec3 specularColor9 = (finalColor * finalMetallic);\n" +
            "   vec3 f9 = specularColor9 + (1.0 - specularColor9) * pow(1.0 - HdotV, 5.0);\n" + // static noise, maybe numeric errors
            "   float k9 = (finalRoughness + 1.0) * (finalRoughness + 1.0) / 8.0;\n" + // done
            "   float GL9 = NdotL * (1.0 - k9) + k9;\n" + // done
            "   float GV9 = NdotV * (1.0 - k9) + k9;\n" + // done
            "   vec3 spec = (Dx9 * f9) / (4.0 * M_PI * GL9 * GV9 * x9 * x9);\n" +
            "   vec3 baseline = specularColor * spec;\n" +
            "   vec3 color;" +
            // "   color = fract(log2(baseline / engineColor));\n" +
            "   color = uv.x < 0.5 ? baseline : engineColor;\n" +
            // "   color = uv.x < 0.5 ? diff2 : diffuseLight;\n" + // diff is correct, so specular is wrong :)
            // "   color = abs(diff2 - diffuseLight) * 100.0;\n" +
            // "   color = abs(diff2 - diffuseLight);\n" +
            // "   if(uv.y > 0.5) color = abs(baseline - engineColor);\n" +
            // "   color = vec3(abs(x9-x));\n" +
            // "   color = baseline;\n" +
            // "   color = vec3(finalMetallic, finalRoughness, finalColor.r);\n" +
            // "   color = finalColor;\n" +
            // "   if(color.r < 0.0) color = vec3(1,0,0);\n" +
            "   result = vec4(color, 1.0);\n" +
            "}\n"
)

fun main() {
    val size = 512
    HiddenOpenGLContext.createOpenGL(size, size)
    val randomTex = Texture2D("random", size, size, 1)
    val random = Random(1234L)
    val randomData = FloatArray(size * size * 4) {
        // ((it * (it % 4 + 1)) % size) * 1f / size
        random.nextFloat()
    }
    randomTex.createRGBA(randomData, false)
    val target = FBStack["test", size, size, TargetType.FloatTarget4, 1, false]
    useFrame(target) {
        val shader = testShader
        shader.use()
        println(shader.fragmentSource)
        randomTex.bind(0)
        flat01.draw(shader)
    }
    FramebufferToMemory.createImage(target, flipY = false, withAlpha = true)
        .write(desktop.getChild("brdf.png"))
    println(Reduction.reduce(target.getTexture0(), Reduction.AVG).print())
}
