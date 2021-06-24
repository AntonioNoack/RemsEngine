package ofx.mio

import me.anno.gpu.GFX.flat01
import me.anno.gpu.RenderSettings.useFrame
import me.anno.gpu.ShaderLib.createShader
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D

object OpticalFlow {

    fun run(lambda: Float, blurAmount: Float, displacement: Float, t0: Texture2D, t1: Texture2D): Framebuffer {

        val w = t0.w
        val h = t0.h

        val flowT = FBStack["flow", w, h, 4, false, 1]

        // flow process

        useFrame(flowT, Renderer.colorRenderer){
            Frame.bind()
            val flow = flowShader.value.value
            flow.use()
            flow.v2("scale", 1f, 1f)
            flow.v2("offset", 1f/w, 1f/h)
            flow.v1("lambda", lambda)
            t0.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
            t1.bind(1, GPUFiltering.LINEAR, Clamping.CLAMP)
            flat01.draw(flow)
        }

        // blur process

        val blur = blurShader.value.value
        blur.use()
        blur.v1("blurSize", blurAmount)
        blur.v1("sigma", blurAmount*0.5f)
        blur.v2("texOffset", 2f, 2f)

        val blurH = FBStack["blurH", w, h, 4, false, 1]
        useFrame(blurH, Renderer.colorRenderer){
            Frame.bind()
            flowT.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            blur.v1("horizontalPass", 1f)
            flat01.draw(blur)
        }

        val blurV = FBStack["blurV", w, h, 4, false, 1]
        useFrame(blurV, Renderer.colorRenderer){
            Frame.bind()
            blurH.bindTexture0(0, GPUFiltering.LINEAR, Clamping.CLAMP)
            blur.v1("horizontalPass", 0f)
            flat01.draw(blur)
        }

        val result = FBStack["reposition", w, h, 4, false, 1]
        useFrame(result, Renderer.colorRenderer){

            Frame.bind()

            // reposition
            val repos = repositionShader.value.value
            repos.use()
            repos.v2("amt", displacement * 0.25f)

            t0.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
            blurV.bindTextures(1, GPUFiltering.LINEAR, Clamping.CLAMP)
            flat01.draw(repos)

        }

        return result

    }

    val flowShader = lazy {
        createShader("flow", "" +
                "attribute vec2 attr0;\n" +
                "void main(){" +
                "   gl_Position = vec4(attr0*2.-1., 0., 1.);\n" +
                "   texCoord = attr0;\n" +
                "}", "varying vec2 texCoord;\n", "" +
                "uniform sampler2D tex0, tex1;\n" +
                "uniform vec2 scale, offset;\n" +
                "uniform float lambda;\n" +
                "vec4 getColorCoded(float x, float y, vec2 scale) {\n" +
                "   vec2 xOut = vec2(max(x,0.),max(-x,0.))*scale.x;\n" +
                "   vec2 yOut = vec2(max(y,0.),max(-y,0.))*scale.y;\n" +
                "   float dirY = 1;\n" +
                "   if (yOut.x > yOut.y) dirY = 0.90;\n" +
                "   return vec4(xOut.xy, max(yOut.x, yOut.y), dirY);\n" +
                "}\n" +
                "void main(){\n" +

                "   vec4 a = texture(tex0, texCoord);\n" +
                "   vec4 b = texture(tex1, texCoord);\n" +
                "   vec2 x1 = vec2(offset.x,0.);\n" +
                "   vec2 y1 = vec2(0.,offset.y);\n" +

                "   // get the difference\n" +
                "   vec4 diffByTime = b-a;\n" +

                "   // calculate the gradient\n" +
                "   // for X\n" +
                "   float gradX = texture(tex1, texCoord+x1).r-texture(tex1, texCoord-x1).r;\n" +
                "   gradX += texture(tex0, texCoord+x1).r-texture(tex0, texCoord-x1).r;\n" +

                "   // for Y\n" +
                "   float gradY = texture(tex1, texCoord+y1).r-texture(tex1, texCoord-y1).r;\n" +
                "   gradY += texture(tex0, texCoord+y1).r-texture(tex0, texCoord-y1).r;\n" +

                "   vec2 grad = vec2(gradX, gradY);\n" +
                "   float gradMagnitude = sqrt(dot(grad,grad) + lambda);\n" +
                "   vec2 vxy = diffByTime.rg * (grad / gradMagnitude);\n" +
                "   gl_FragColor = getColorCoded(vxy.r, vxy.g, scale);\n" +

                "}  " +
                "", listOf("tex0", "tex0")
        )
    }

    val blurShader = lazy {
        createShader("blur", "" +
                "attribute vec2 attr0;\n" +
                "void main(){" +
                "   gl_Position = vec4(attr0*2.-1., 0., 1.);\n" +
                "   texCoord = attr0;\n" +
                "}", "varying vec2 texCoord;\n", "" +
                "uniform sampler2D tex;\n" +
                "uniform vec2 texOffset;\n" +
                "\n" +
                "uniform float blurSize;\n" +
                "uniform float horizontalPass;// 0 or 1 to indicate vertical or horizontal pass\n" +
                "uniform float sigma;// The sigma value for the gaussian function: higher value means more blur\n" +
                "// A good value for 9x9 is around 3 to 5\n" +
                "// A good value for 7x7 is around 2.5 to 4\n" +
                "// A good value for 5x5 is around 2 to 3.5\n" +
                "// ... play around with this based on what you need :)\n" +
                "const float pi = 3.14159265;\n" +
                "vec4 get2DOff(sampler2D tex, vec2 coord) {\n" +
                "   vec4 col = texture(tex, coord);\n" +
                "   if (col.w >0.95) col.z = -col.z;\n" +
                "   return vec4(col.y-col.x, col.z, 1, 1);\n" +
                "}\n" +
                "vec4 getColorCoded(float x, float y, vec2 scale) {\n" +
                "   vec2 xOut = vec2(max(x,0.),max(-x,0.))*scale.x;\n" +
                "   vec2 yOut = vec2(max(y,0.),max(-y,0.))*scale.y;\n" +
                "   float dirY = 1;\n" +
                "   if (yOut.x > yOut.y) dirY = 0.90;\n" +
                "   return vec4(xOut.yx,max(yOut.x,yOut.y),dirY);\n" +
                "}\n" +
                "void main() {  \n" +

                "   float numBlurPixelsPerSide = float(blurSize / 2); \n" +
                "   vec2 blurMultiplyVec = 0 < horizontalPass ? vec2(1.0, 0.0) : vec2(0.0, 1.0);\n" +

                // Incremental Gaussian Coefficent Calculation (See GPU Gems 3 pp. 877 - 889)
                "   vec3 incrementalGaussian;\n" +
                "   incrementalGaussian.x = 1.0 / (sqrt(2.0 * pi) * sigma);\n" +
                "   incrementalGaussian.y = exp(-0.5 / (sigma * sigma));\n" +
                "   incrementalGaussian.z = incrementalGaussian.y * incrementalGaussian.y;\n" +

                "   vec4 avgValue = vec4(0.0, 0.0, 0.0, 0.0);\n" +
                "   float coefficientSum = 0.0;\n" +

                "   // Take the central sample first...\n" +
                "   avgValue += get2DOff(tex, texCoord.st) * incrementalGaussian.x;\n" +
                "   coefficientSum += incrementalGaussian.x;\n" +
                "   incrementalGaussian.xy *= incrementalGaussian.yz;\n" +

                // Go through the remaining 8 vertical samples (4 on each side of the center)
                "       for (float i = 1.0; i <= numBlurPixelsPerSide; i++) { \n" +
                "       avgValue += get2DOff(tex, texCoord.st - i * texOffset * \n" +
                "           blurMultiplyVec) * incrementalGaussian.x;         \n" +
                "       avgValue += get2DOff(tex, texCoord.st + i * texOffset * \n" +
                "           blurMultiplyVec) * incrementalGaussian.x;         \n" +
                "       coefficientSum += 2.0 * incrementalGaussian.x;\n" +
                "       incrementalGaussian.xy *= incrementalGaussian.yz;\n" +
                "   }\n" +
                "   vec4 finColor = avgValue / coefficientSum;\n" +
                "   gl_FragColor = getColorCoded(finColor.x, finColor.y, vec2(1,1));\n" +
                "}", listOf("tex")
        )
    }

    val repositionShader = lazy {
        createShader("reposition", "" +
                "attribute vec2 attr0;\n" +
                "void main(){" +
                "   gl_Position = vec4(attr0*2.-1., 0., 1.);\n" +
                "   texCoord = attr0;\n" +
                "}", "varying vec2 texCoord;\n", "" +
                "uniform vec2 amt;\n" +
                "uniform sampler2D tex0, tex1;\n" +
                "vec2 get2DOff(sampler2D tex, vec2 coord) {\n" +
                "   vec4 col = texture(tex, coord);\n" +
                "   if (col.w > 0.95) col.z = -col.z;\n" +
                "   return vec2(col.x-col.y, col.z);\n" +
                "}\n" +
                "void main(){\n" +
                "   vec2 coord = get2DOff(tex1, texCoord) * amt + texCoord;// relative coordinates\n" +
                "   vec4 repos = texture(tex0, coord);\n" +
                "   gl_FragColor = repos;\n" +
                "}", listOf("tex0", "tex1")
        )
    }

}