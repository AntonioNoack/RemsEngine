package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.createCustomShader2
import me.anno.gpu.GFX.flat01
import me.anno.gpu.shader.Shader
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Texture2D
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.Transform.Companion.yAxis
import me.anno.objects.Transform.Companion.zAxis
import me.anno.utils.get
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*

/**
 * shader by Kleber Garcia, 'Kecho', 2017, MIT license (https://github.com/kecho/CircularDofFilterGenerator)
 * the shader was modified to work without ShaderToy, and the filter texture was uploaded directly
 *
 * // todo use the generator for larger kernels (more expensive, larger results without artifacts)
 * */
object BokehBlur {

    private const val KERNEL_RADIUS = 8
    private const val KERNEL_COUNT = KERNEL_RADIUS * 2 + 1

    var compositionShader: Shader? = null
    var perChannelShader: Shader? = null

    // do I need f32 pairs?
    fun fb() = Framebuffer(1, 1, 1, 1, true, Framebuffer.DepthBufferType.NONE)
    fun fbPair() = fb() to fb()

    val red = fbPair()
    val green = fbPair()
    val blue = fbPair()

    val filterTexture = Texture2D(KERNEL_COUNT, 1, 1)

    val result = fbPair()

    fun draw(buffer: Framebuffer, sizeRY: Float){

        val w = buffer.w
        val h = buffer.h

        if(compositionShader == null) init()

        glDisable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)

        val r = red[GFX.isFinalRendering]
        val g = green[GFX.isFinalRendering]
        val b = blue[GFX.isFinalRendering]
        val result = result[GFX.isFinalRendering]

        val filterRadius = sizeRY * h / KERNEL_COUNT

        filterTexture.bind(1, true)

        var shader = perChannelShader!!
        shader.use()
        shader.v2("stepVal", 1f/w, 1f/h)
        shader.v1("filterRadius", filterRadius)

        drawChannel(shader, r, w, h, xAxis)
        drawChannel(shader, g, w, h, yAxis)
        drawChannel(shader, b, w, h, zAxis)

        shader = compositionShader!!
        shader.use()
        shader.v2("stepVal", 1f/w, 1f/h)
        shader.v1("filterRadius", filterRadius)

        buffer.bind()
        //bind(w, h)
        // filter texture is bound correctly
        r.bindTexture0(1, false)
        g.bindTexture0(2, false)
        b.bindTexture0(3, false)
        flat01.draw(shader)

        // return result.textures[0]

    }

    fun drawChannel(shader: Shader, target: Framebuffer, w: Int, h: Int, channel: Vector3f){
        target.bind(w, h)
        shader.v3("channelSelection", channel)
        flat01.draw(shader)
    }

    fun init(){

        val vertexShader = "" +
                "in vec2 attr0;\n" +
                "void main(){" +
                "   gl_Position = vec4(attr0*2.0-1.0, 0.0, 1.0);\n" +
                "   uv = attr0;\n" +
                "}"

        val varyingShader = "varying vec2 uv;\n"

        perChannelShader = createCustomShader2(vertexShader, varyingShader, "" +

                "uniform float filterRadius;\n" +
                "uniform vec2 stepVal;\n" + // 1/resolution
                "uniform sampler2D image, filterTexture;\n" +
                "uniform vec3 channelSelection;\n" +

                "vec4 getFilters(int x){\n" +
                "    float u = float(x)*${1.0/(KERNEL_COUNT-1.0)};\n" +
                "    return texture(filterTexture, vec2(u, 0.0));\n" +
                "}\n" +

                "void main(){\n" +
                "    vec4 val = vec4(0,0,0,0);\n" +
                "    for (int i=-$KERNEL_RADIUS; i <= $KERNEL_RADIUS; ++i){\n" +
                "        vec2 coords = uv + vec2(stepVal.x*float(i)*filterRadius,0.0);\n" +
                "        float imageTexelR = dot(texture(image, coords).rgb, channelSelection);\n" +
                "        vec4 c0_c1 = getFilters(i+$KERNEL_RADIUS);\n" +
                "        val += imageTexelR * c0_c1;\n" +
                "    }\n" +
                "    gl_FragColor = val;\n" +
                "}", listOf("image", "filterTexture"))

        val kernel0 = floatArrayOf(
            0.014096f, -0.022658f, 0.055991f, 0.004413f,
            -0.020612f, -0.025574f, 0.019188f, 0.000000f,
            -0.038708f, 0.006957f, 0.000000f, 0.049223f,
            -0.021449f, 0.040468f, 0.018301f, 0.099929f,
            0.013015f, 0.050223f, 0.054845f, 0.114689f,
            0.042178f, 0.038585f, 0.085769f, 0.097080f,
            0.057972f, 0.019812f, 0.102517f, 0.068674f,
            0.063647f, 0.005252f, 0.108535f, 0.046643f,
            0.064754f, 0.000000f, 0.109709f, 0.038697f,
            0.063647f, 0.005252f, 0.108535f, 0.046643f,
            0.057972f, 0.019812f, 0.102517f, 0.068674f,
            0.042178f, 0.038585f, 0.085769f, 0.097080f,
            0.013015f, 0.050223f, 0.054845f, 0.114689f,
            -0.021449f, 0.040468f, 0.018301f, 0.099929f,
            -0.038708f, 0.006957f, 0.000000f, 0.049223f,
            -0.020612f, -0.025574f, 0.019188f, 0.000000f,
            0.014096f, -0.022658f, 0.055991f, 0.004413f
        )
        val kernel1 = floatArrayOf(
            0.000115f, 0.009116f, 0.000000f, 0.051147f,
            0.005324f, 0.013416f, 0.009311f, 0.075276f,
            0.013753f, 0.016519f, 0.024376f, 0.092685f,
            0.024700f, 0.017215f, 0.043940f, 0.096591f,
            0.036693f, 0.015064f, 0.065375f, 0.084521f,
            0.047976f, 0.010684f, 0.085539f, 0.059948f,
            0.057015f, 0.005570f, 0.101695f, 0.031254f,
            0.062782f, 0.001529f, 0.112002f, 0.008578f,
            0.064754f, 0.000000f, 0.115526f, 0.000000f,
            0.062782f, 0.001529f, 0.112002f, 0.008578f,
            0.057015f, 0.005570f, 0.101695f, 0.031254f,
            0.047976f, 0.010684f, 0.085539f, 0.059948f,
            0.036693f, 0.015064f, 0.065375f, 0.084521f,
            0.024700f, 0.017215f, 0.043940f, 0.096591f,
            0.013753f, 0.016519f, 0.024376f, 0.092685f,
            0.005324f, 0.013416f, 0.009311f, 0.075276f,
            0.000115f, 0.009116f, 0.000000f, 0.051147f
        )

        val kernelTexture = FloatArray(KERNEL_COUNT * 4)
        for(i in 0 until 4*KERNEL_COUNT step 4){
            kernelTexture[i  ] = kernel0[i  ]
            kernelTexture[i+1] = kernel0[i+1]
            kernelTexture[i+2] = kernel1[i  ]
            kernelTexture[i+3] = kernel1[i+1]
        }

        filterTexture.create(kernelTexture)

        compositionShader = createCustomShader2(vertexShader, varyingShader, "" +

                "uniform float filterRadius;\n" +
                "uniform vec2 stepVal;\n" + // 1/resolution
                "uniform sampler2D inputRed, inputGreen, inputBlue, filterTexture;\n" +
                "const vec2 Kernel0Weights_RealX_ImY = vec2(0.411259, -0.548794);\n" +
                "const vec2 Kernel1Weights_RealX_ImY = vec2(0.513282, 4.561110);\n" +

                "vec2 multComplex(vec2 p, vec2 q){\n" +
                "    return vec2(p.x*q.x-p.y*q.y, p.x*q.y+p.y*q.x);\n" +
                "}\n" +

                "vec4 getFilters(int x){\n" +
                "    float u = float(x)*${1.0/KERNEL_COUNT};\n" +
                "    return texture(filterTexture, vec2(u,0.0));\n" +
                "}\n" +

                "void main(){\n" +

                "    vec4 valR = vec4(0,0,0,0);\n" +
                "    vec4 valG = vec4(0,0,0,0);\n" +
                "    vec4 valB = vec4(0,0,0,0);\n" +

                "    for (int i = -$KERNEL_RADIUS; i <= $KERNEL_RADIUS; ++i){\n" +
                "        vec2 coords = uv + vec2(0.0,stepVal.y*float(i)*filterRadius);\n" +
                "        vec4 imageTexelR = texture(inputRed, coords);  \n" +
                "        vec4 imageTexelG = texture(inputGreen, coords);  \n" +
                "        vec4 imageTexelB = texture(inputBlue, coords);  \n" +

                "        vec4 c0_c1 = getFilters(i+$KERNEL_RADIUS);\n" +

                "        valR.xy += multComplex(imageTexelR.xy,c0_c1.xy);\n" +
                "        valR.zw += multComplex(imageTexelR.zw,c0_c1.zw);\n" +

                "        valG.xy += multComplex(imageTexelG.xy,c0_c1.xy);\n" +
                "        valG.zw += multComplex(imageTexelG.zw,c0_c1.zw);\n" +

                "        valB.xy += multComplex(imageTexelB.xy,c0_c1.xy);\n" +
                "        valB.zw += multComplex(imageTexelB.zw,c0_c1.zw);\n" +
                "    }\n" +

                "    float redChannel   = dot(valR,vec4(Kernel0Weights_RealX_ImY,Kernel1Weights_RealX_ImY));\n" +
                "    float greenChannel = dot(valG,vec4(Kernel0Weights_RealX_ImY,Kernel1Weights_RealX_ImY));\n" +
                "    float blueChannel  = dot(valB,vec4(Kernel0Weights_RealX_ImY,Kernel1Weights_RealX_ImY));\n" +
                "    gl_FragColor = vec4(redChannel, greenChannel, blueChannel,1.0);\n" +
                "}", listOf("filterTexture", "inputRed", "inputGreen", "inputBlue"))

    }

}