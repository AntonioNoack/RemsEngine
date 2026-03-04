package me.anno.graph.visual.render.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.effects.CheckerboardHelperNode.Companion.flipPattern
import me.anno.maths.Maths.clamp
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4i
import org.lwjgl.opengl.GL46C.GL_SAMPLE_POSITION
import org.lwjgl.opengl.GL46C.glGetMultisamplefv

// use 2x MSAA at half resolution, vary per frame from left/right
// reconstruction: if top & bottom are similar, fill in vertically, if left & right, horizontally, else mix all four
// todo temporal reprojection of previous frame?
// todo enable per-sample shading for better texture sampling (test that!)
// todo slim mode, where we only calculate color?
/**
 * Uses checkerboard rendering for improved shading performance.
 * */
class CheckerboardResolveNode : TimedRenderingNode(
    "CheckerboardHelper",
    listOf("Texture", "Illuminated", "Texture", "Normal", "Texture", "Depth"),
    listOf("Texture", "Illuminated", "Texture", "Normal", "Texture", "Depth")
) {

    override fun executeAction() {
        val colorT = getInput(1) as? Texture ?: return
        val normalT = getInput(2) as? Texture ?: return
        val depthT = getInput(3) as? Texture ?: return
        timeRendering(name, timer) {

            val color = colorT.texMS
            val normal = normalT.texMS
            val depth = depthT.texMS
            check(color.samples == 2) { "Expected color to have 2 samples, but got $color" }
            check(normal.samples == 2) { "Expected normal to have 2 samples, but got $normal" }
            check(depth.samples == 2) { "Expected depth to have 2 samples, but got $depth" }

            val fb = FBStack[
                "CheckerboardHelper", 2 * color.width, 2 * color.height,
                targetTypes, 1, DepthBufferType.TEXTURE
            ]

            samplePattern.value

            useFrame(fb) {
                val shader = checkerShader
                shader.use()

                color.bindTrulyNearest(shader, "colorTex")
                normal.bindTrulyNearest(shader, "normalTex")
                depth.bindTrulyNearest(shader, "depthTex")

                shader.v1b("flip", flipPattern)
                val si = findSampleIndex(pos0)
                val ti = 3 - si
                shader.v1i("sample0", si)
                shader.v1i("sample1", ti)

                shader.v4i("duv0", patterns[si])
                shader.v4i("duv1", patterns[ti])

                flat01.draw(shader)
            }

            GFX.check()

            setOutput(1, Texture.texture(fb, 0))
            setOutput(2, Texture(fb.getTextureI(1), null, "xy", DeferredLayerType.NORMAL))
            setOutput(3, Texture.depth(fb))
        }
    }

    companion object {

        private val samplePattern = lazy {
            useFrame(FBStack["tmp", 8, 8, TargetType.UInt8x1, 2, DepthBufferType.NONE]) {
                glGetMultisamplefv(GL_SAMPLE_POSITION, 0, pos0)
                glGetMultisamplefv(GL_SAMPLE_POSITION, 1, pos1)
                GFX.check()
            }
        }

        private val targetTypes = listOf(TargetType.UInt8x4, TargetType.UInt8x2)
        private val patterns = arrayOf(
            Vector4i(1, 0, 0, 1),

            // think about what 1 and 2 have as their patterns
            // todo check this by using glFramebufferSampleLocationsfvNV/glFramebufferSampleLocationsfvARB
            //  -> we need an Nvidia GPU to test this...
            Vector4i(-1, 0, 0, +1),
            Vector4i(0, -1, +1, 0),

            Vector4i(0, -1, -1, 0),
        )

        private val pos0 = FloatArray(2)
        private val pos1 = FloatArray(2)

        private val checkerShader = Shader(
            "checkerResolve",
            emptyList(), ShaderLib.coordsUVVertexShader,
            uvList, listOf(
                Variable(GLSLType.V1B, "flip"),
                Variable(GLSLType.V1I, "sample0"),
                Variable(GLSLType.V1I, "sample1"),
                Variable(GLSLType.V4I, "duv0"),
                Variable(GLSLType.V4I, "duv1"),
                Variable(GLSLType.S2DMS, "colorTex"),
                Variable(GLSLType.S2DMS, "depthTex"),
                Variable(GLSLType.S2DMS, "normalTex"),
                Variable(GLSLType.V4F, "outColor", VariableMode.OUT),
                Variable(GLSLType.V4F, "outNormal", VariableMode.OUT),
            ), "" +
                    octNormalPacking +
                    "vec2 mixNormal2(vec2 a, vec2 b) {\n" +
                    "   vec3 na = UnpackNormal(a);\n" +
                    "   vec3 nb = UnpackNormal(b);\n" +
                    "   return PackNormal(na+nb);\n" +
                    "}\n" +
                    "vec2 mixNormal4(vec2 a, vec2 b, vec2 c, vec2 d) {\n" +
                    "   vec3 na = UnpackNormal(a);\n" +
                    "   vec3 nb = UnpackNormal(b);\n" +
                    "   vec3 nc = UnpackNormal(c);\n" +
                    "   vec3 nd = UnpackNormal(d);\n" +
                    "   return PackNormal(na+nb+nc+nd);\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   ivec2 globalUV = ivec2(gl_FragCoord.xy);\n" +
                    "   if (flip) globalUV++;\n" +
                    "   ivec2 localUV = globalUV & 1;\n" +
                    "   int localId = localUV.x + localUV.y * 2;\n" +

                    "   ivec2 sm1 = textureSize(colorTex)-1;\n" +
                    "   ivec2 srcUV  = clamp(globalUV >> 1, ivec2(0), sm1);\n" +
                    "   ivec2 srcUV0 = clamp(srcUV + (localId < 2 ? duv0.xy : duv0.zw), ivec2(0), sm1);\n" +
                    "   ivec2 srcUV1 = clamp(srcUV + (localId < 2 ? duv1.xy : duv1.zw), ivec2(0), sm1);\n" +

                    "   vec3  col0a = texelFetch(colorTex,  srcUV, 0).rgb;\n" +
                    "   vec3  col1a = texelFetch(colorTex,  srcUV, 1).rgb;\n" +
                    "   float dep0a = texelFetch(depthTex,  srcUV, 0).r;\n" +
                    "   float dep1a = texelFetch(depthTex,  srcUV, 1).r;\n" +
                    "   vec2  nor0a = texelFetch(normalTex, srcUV, 0).rg;\n" +
                    "   vec2  nor1a = texelFetch(normalTex, srcUV, 1).rg;\n" +

                    "   vec3  col0b = texelFetch(colorTex, srcUV0, 0).rgb;\n" +
                    "   vec3  col1b = texelFetch(colorTex, srcUV1, 1).rgb;\n" +
                    "   float dep0b = texelFetch(colorTex, srcUV0, 0).r;\n" +
                    "   float dep1b = texelFetch(colorTex, srcUV1, 1).r;\n" +
                    "   vec2  nor0b = texelFetch(colorTex, srcUV0, 0).rg;\n" +
                    "   vec2  nor1b = texelFetch(colorTex, srcUV1, 1).rg;\n" +

                    // todo include normal and depth in comparison
                    "   float aCloseness = dot(abs(col1a-col0a), vec3(1.0));\n" +
                    "   float bCloseness = dot(abs(col1b-col0b), vec3(1.0));\n" +

                    "   bool mixA = aCloseness < 0.3 && aCloseness <= bCloseness;\n" +
                    "   bool mixB = bCloseness < 0.3 && !mixA;\n" +

                    "   vec3 color = \n" +
                    "       sample0 == localId ? col0a :\n" +
                    "       sample1 == localId ? col1a :\n" +
                    "       mixA ? (col0a + col1a) * 0.5 :\n" +
                    "       mixB ? (col0b + col1b) * 0.5 :\n" +
                    "       (col0a + col0b + col1a + col1b) * 0.25;\n" +

                    "   vec2 normal = \n" +
                    "       sample0 == localId ? nor0a :\n" +
                    "       sample1 == localId ? nor1a :\n" +
                    "       mixA ? mixNormal2(nor0a, nor1a) :\n" +
                    "       mixB ? mixNormal2(nor0b, nor1b) :\n" +
                    "       mixNormal4(nor0a, nor0b, nor1a, nor1b);\n" +

                    "   float depth = \n" +
                    "       sample0 == localId ? dep0a :\n" +
                    "       sample1 == localId ? dep1a :\n" +
                    "       mixA ? (dep0a + dep1a) * 0.5 :\n" +
                    "       mixB ? (dep0b + dep1b) * 0.5 :\n" +
                    "       (dep0a + dep0b + dep1a + dep1b) * 0.25;\n" +

                    "   outColor = vec4(color, 1.0);\n" +
                    "   outNormal = vec4(normal, 0.0, 0.0);\n" +
                    "   gl_FragDepth = depth;\n" +
                    "}\n"
        )

        private fun findSampleIndex(pos: FloatArray): Int {
            val x = (clamp(pos[0]) > 0.5f).toInt()
            val y = (clamp(pos[1]) > 0.5f).toInt()
            return x + y * 2
        }
    }
}