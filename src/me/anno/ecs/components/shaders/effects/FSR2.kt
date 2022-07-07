package me.anno.ecs.components.shaders.effects

import me.anno.Engine
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.*
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import org.joml.Vector2i
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tan

class FSR2 {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // todo a small and simple test;
            // todo first for shader correctness
            HiddenOpenGLContext.createOpenGL()
            val inst = FSR2()
            val rw = 500
            val rh = 500
            val pw = rw * 3
            val ph = rh * 3
            inst.init(rw, rh, pw, ph)
            val fb = Framebuffer(
                "deferred", rw, rh, arrayOf(
                    TargetType.UByteTarget4, // color
                    TargetType.FP16Target2, // motion
                ), DepthBufferType.TEXTURE
            )
            fb.ensure()
            val colorBuffer = fb.getTextureI(0) as Texture2D
            val motion = fb.getTextureI(1) as Texture2D
            colorBuffer.ensurePointer()
            motion.ensurePointer()
            val previousOutput = Texture2D("prev", pw, ph, 1)
            previousOutput.create(TargetType.FP16Target4)
            val reactiveMask = null
            inst.calculate(fb.depthTexture!!, motion, reactiveMask, previousOutput, colorBuffer, rw, rh, pw, ph)
        }
    }

    // todo understand FSR 2.0 and then implement it
    // todo it this possible to be implemented in the GFX pipeline?
    // if not, change all framebuffers to be textures

    // main overview: https://raw.githubusercontent.com/GPUOpen-Effects/FidelityFX-FSR2/master/docs/media/super-resolution-temporal/algorithm-structure.svg
    // repo: https://github.com/GPUOpen-Effects/FidelityFX-FSR2

    var hdrColorInput = true

    // similar to Reinhard tonemapping
    val tonemap = "vec3 tonemap(vec3 rgb){ return rgb/(1.0 + max(max(0.0,rgb.r),max(rgb.g,rgb.b))); }\n"
    val invTonemap = "vec3 invTonemap(vec3 rgb){ return rgb / max(0.001, 1.0-max(rgb.r,max(rgb.g,rgb.b))); }\n"

    lateinit var prevDepth: Texture2D
    lateinit var dilatedDepth: Texture2D
    lateinit var dilatedMotion: Texture2D

    lateinit var disocclusionMask: Framebuffer

    lateinit var lumaHistory: Texture2D
    lateinit var colorBuffer2: Texture2D
    lateinit var luma: Framebuffer

    lateinit var currentLuminance: Framebuffer

    lateinit var lockStatus: Framebuffer

    lateinit var outputBuffer: Texture2D
    lateinit var reprojectedLocks: Texture2D
    lateinit var output: Framebuffer

    var currExposure = Framebuffer("exposure0", 1, 1, TargetType.FloatTarget1)
    var prevExposure = Framebuffer("exposure1", 1, 1, TargetType.FloatTarget1)

    fun Framebuffer.c() = getTexture0() as Texture2D
    fun Texture2D.c() = this

    // could be merged with declarations mostly
    fun init(rw: Int, rh: Int, pw: Int, ph: Int) {

        prevDepth = Texture2D("prev-depth", rw, rh, 1)
        prevDepth.create(TargetType.R32U) // special type

        dilatedDepth = Texture2D("dilated-depth", rw, rh, 1)
        dilatedDepth.create(TargetType.FP16Target1) // official FSR uses R16UI

        dilatedMotion = Texture2D("dilated-motion", rw, rh, 1)
        dilatedMotion.create(TargetType.FP16Target2) // official FSR uses RG16F

        disocclusionMask = Framebuffer("disocclusion", rw, rh, TargetType.UByteTarget1)
        luma = Framebuffer("luma", rw, rh, arrayOf(TargetType.UByteTarget4, TargetType.FP16Target4))
        luma.ensure()
        lumaHistory = luma.getTextureI(0) as Texture2D
        colorBuffer2 = luma.getTextureI(1) as Texture2D
        currentLuminance = Framebuffer("luma2", rw, rh, TargetType.FP16Target1)

        // todo lock status has previous stage presentation resolution??
        lockStatus = Framebuffer("lock", pw, ph, TargetType.FP16Target2)

        output = Framebuffer("output", pw, ph, arrayOf(TargetType.FP16Target4, TargetType.FP16Target2))
        output.ensure()
        outputBuffer = output.getTextureI(0) as Texture2D
        reprojectedLocks = output.getTextureI(1) as Texture2D

        prevExposure.ensure()
        currExposure.ensure()

    }

    fun calculate(
        depth: Texture2D,
        motion: Texture2D,
        reactiveMask: Texture2D?,
        previousOutput: Texture2D,
        colorBuffer: Texture2D,
        rw: Int, rh: Int,
        pw: Int, ph: Int
    ) {
        reconstructNDilate(depth, motion, prevDepth, dilatedDepth, dilatedMotion, rw, rh)
        depthClip(prevDepth, dilatedDepth.c(), dilatedMotion.c(), disocclusionMask, rw, rh)
        autoExposureSPD(
            colorBuffer,
            prevExposure.c(),
            currentLuminance,
            currExposure,
            max((rw + 1) / 2, 1),
            max((rh + 1) / 2, 1)
        )
        // todo on some platforms, we need a copy for luma history (because we read and write from/to it)
        adjustInputColor(colorBuffer, currExposure.c(), lumaHistory.c(), luma, rw, rh)
        // todo re-enable
        // updateLocks(colorBuffer2.c(), lockStatus, rw, rh)
        lockStatus.ensure()
        reprojectNAccumulate(
            dilatedMotion.c(), reactiveMask, previousOutput,
            currExposure.c(), disocclusionMask.c(), currentLuminance.c(), lumaHistory.c(), colorBuffer2.c(),
            lockStatus.c(), output, rw, rh, pw, ph
        )
        val tmp = prevExposure
        prevExposure = currExposure
        currExposure = tmp
    }

    fun generateReactiveMask() {
        // todo implement + why exactly do we need that?
    }

    val reconstructNDilateShader = ComputeShader(
        "rec&dilate", Vector2i(16), "" +

                // todo can we guarantee depthTex to be r32f? no, we need all cases...
                "uniform sampler2D depthTex;\n" +
                "layout(rgba32f, binding = 1) readonly  uniform  image2D motionTex;\n" +
                "layout(r32ui,   binding = 2) coherent  uniform uimage2D prevDepth;\n" +
                "layout(r32f,    binding = 3) writeonly uniform  image2D dilatedDepth;\n" +
                "layout(rg16f,   binding = 4) writeonly uniform  image2D dilatedMotion;\n" +

                "uniform vec2 renderSizeM1;\n" +
                "uniform ivec2 renderSize;\n" +
                "uniform float threshold;\n" +

                "ivec2 findNearestDepth(ivec2 iuv, out float nearestDepth){\n" +
                "   ivec2 nearestUV = iuv;\n" +
                "   nearestDepth = texture2D(depthTex,iuv).r;\n" +
                "   for(int j=-1;j<=1;j++){\n" +
                "       for(int i=-1;i<=1;i++){\n" +
                "           ivec2 iuv2 = iuv + ivec2(i,j);\n" +
                "           if(all(greaterThanEqual(iuv2, ivec2(0))) && all(lessThan(iuv2, renderSize))){\n" +
                "               float depth = texture2D(depthTex,iuv2).r;\n" +
                "               if(depth > nearestDepth){\n" + // because inverse depth buffer
                "                   nearestDepth = depth;\n" +
                "                   nearestUV = iuv2;\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "   return nearestUV;\n" +
                "}\n" +

                "void main(){\n" +
                "   ivec2 iuv = ivec2(gl_GlobalInvocationID.xy);\n" +
                "   if(all(lessThan(iuv,renderSize))){\n" +
                "       float nearestDepth = 0.0;\n" +
                "       ivec2 nearestUV = findNearestDepth(iuv, nearestDepth);\n" +
                // the docs say that the motion vector resolution shall be the render resolution
                // the code has the option to use higher quality motion vectors...
                "       vec2 motionVector = imageLoad(motionTex, nearestUV).xy;\n" +
                "       imageStore(dilatedMotion, iuv, vec4(motionVector, 0.0, 1.0));\n" +
                "       imageStore(dilatedDepth, iuv, vec4(nearestDepth, 0.0, 0.0, 1.0));\n" +
                // reconstruct prev depth
                "       vec2 motion2 = motionVector * renderSizeM1;\n" +
                "       ivec2 prevPos = iuv + ivec2(floor(motion2));\n" +
                "       vec2 f = fract(motion2), g = 1.0 - f;\n" +
                "       vec2 fx = vec2(g.x,f.x), fy = vec2(g.y,f.y);\n" +
                "       uint bits = floatBitsToUint(nearestDepth);\n" +
                "       for(int y=0;y<=1;y++){\n" +
                "           for(int x=0;x<=1;x++){\n" +
                // store reconstructed depth
                "               ivec2 storePos = prevPos + ivec2(x,y);\n" +
                "               if(fx[x]*fy[y] > threshold && \n" +
                "   all(greaterThanEqual(storePos, ivec2(0,0))) && \n" +
                "   all(lessThan(storePos, renderSize))){\n" +
                "                   imageAtomicMax(prevDepth, storePos, bits);\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}"
    ).apply {
        glslVersion = 330 // multiple outputs
        setTextureIndices("depthTex", "motionTex", "reactiveMaskTex")
    }

    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/c8fc17d281665927e414b6b8117873cccbac0990/src/ffx-fsr2-api/shaders/ffx_fsr2_callbacks_glsl.h
    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/c8fc17d281665927e414b6b8117873cccbac0990/src/ffx-fsr2-api/shaders/ffx_fsr2_reconstruct_dilated_velocity_and_previous_depth.h
    fun reconstructNDilate(
        depth: Texture2D, motion: Texture2D,
        dstPrevDepth: Texture2D, dstDilatedDepth: Texture2D, dstDilatedMotion: Texture2D,
        rw: Int, rh: Int
    ) {
        GFX.check()
        // todo fill dstPrevDepth with farthest values (0, because of inverse depth)
        dstPrevDepth.resize(rw, rh, TargetType.R32U)
        dstDilatedDepth.resize(rw, rh, TargetType.FloatTarget1)
        dstDilatedMotion.resize(rw, rh, TargetType.FP16Target2)
        depth.ensurePointer()
        val shader = reconstructNDilateShader
        shader.use()
        GFX.check()
        depth.bindTrulyNearest(0)
        GFX.check()
        shader.bindTexture(1, motion, ComputeTextureMode.READ)
        GFX.check()
        shader.bindTexture(2, dstPrevDepth, ComputeTextureMode.READ_WRITE)
        GFX.check()
        shader.bindTexture(3, dstDilatedDepth, ComputeTextureMode.WRITE)
        GFX.check()
        shader.bindTexture(4, dstDilatedMotion, ComputeTextureMode.WRITE)
        GFX.check()
        shader.v2f("renderSizeM1", rw - 1f, rh - 1f)
        shader.v2i("renderSize", rw, rh)
        // reconstructedDepthBilinearWeightThreshold
        shader.v1f("threshold", 0.05f)
        shader.runBySize(rw, rh)
        GFX.check()
    }

    val isOnScreen =
        "bool isOnScreen(ivec2 uv, ivec2 size){ return all(greaterThanEqual(uv,ivec2(0,0))) && all(lessThan(uv,size)); }\n"

    val depthClipShader = Shader(
        "depth-clip",
        ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
        listOf(
            Variable(GLSLType.S2D, "dilatedDepthTex"),
            Variable(GLSLType.S2D, "dilatedMotionTex"),
            Variable(GLSLType.V2I, "renderSizeI"),
            Variable(GLSLType.V2F, "renderSize"),
            Variable(GLSLType.V1F, "threshold"), // < 0.25
            Variable(GLSLType.V1F, "sepFactor"), // 1.37e-5 * TanHalfFoV() * halfViewportWidth
            Variable(GLSLType.V1F, "depthClipBaseScale"), // defined as a constant 4 ...
            Variable(GLSLType.V4F, "depthClip", VariableMode.OUT),
        ),
        "" +
                "uniform usampler2D prevDepthTex;\n" +
                isOnScreen +
                "float deviceDepthToViewSpace(float d){\n" +
                "   return 0.0;\n" + // todo implement it.. how?
                "}\n" +
                "float sampleDepthClip(ivec2 uv, float currDepth){\n" +
                "   float prevDepth = uintBitsToFloat(texelFetch(prevDepthTex, uv, 0).r);\n" +
                "   prevDepth = abs(deviceDepthToViewSpace(prevDepth));\n" +
                "   float depthThreshold = min(currDepth, prevDepth);\n" +
                "   float minSep = sepFactor * depthThreshold;\n" +
                "   float depthDiff = currDepth - prevDepth;\n" +
                "   float depthClipFactor = depthDiff > 0.0 ? clamp(minSep/depthDiff,0.0,1.0) : 1.0;\n" +
                "   return depthClipFactor * depthClipBaseScale;\n" +
                "}\n" +
                "void main(){\n" +
                "   vec2 motion = texture(dilatedMotionTex, uv).rg;\n" +
                "   vec2 uv2 = uv + motion;\n" +
                "   float currDepth = abs(deviceDepthToViewSpace(texture(dilatedDepthTex, uv).r));\n" +
                "   vec2 fuv = uv2 * renderSize - 0.5, flUV = floor(fuv);\n" +
                "   ivec2 iuv = ivec2(flUV);\n" +
                "   vec2 f = fuv - flUV, g = 1.0 - f;\n" +
                "   float depth = 0.0, weight = 0.0;\n" +
                "   float w; ivec2 c;\n" +
                "   w = g.x*g.y;\n" +
                "   if(isOnScreen(iuv,renderSizeI) && w>threshold){\n" +
                "       depth += w * sampleDepthClip(iuv,currDepth);\n" +
                "       weight += w;\n" +
                "   }\n" +
                "   w = f.x*g.y; c = iuv+ivec2(1,0);\n" +
                "   if(isOnScreen(c,renderSizeI) && w>threshold){\n" +
                "       depth += w * sampleDepthClip(c,currDepth);\n" +
                "       weight += w;\n" +
                "   }\n" +
                "   w = g.x*f.y; c = iuv+ivec2(0,1);\n" +
                "   if(isOnScreen(c,renderSizeI) && w>threshold){\n" +
                "       depth += w * sampleDepthClip(c,currDepth);\n" +
                "       weight += w;\n" +
                "   }\n" +
                "   w = f.x*f.y; c = iuv+ivec2(1,1);\n" +
                "   if(isOnScreen(c,renderSizeI) && w>threshold){\n" +
                "       depth += w * sampleDepthClip(c,currDepth);\n" +
                "       weight += w;\n" +
                "   }\n" +
                "   depthClip = vec4(weight > 0.0 ? depth / weight : depthClipBaseScale, 0.0, 0.0, 1.0);\n" +
                "}"
    ).apply {
        glslVersion = 330 // for uintBitsToFloat
        setTextureIndices("prevDepthTex", "dilatedDepthTex", "dilatedMotionTex")
    }

    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/master/src/ffx-fsr2-api/shaders/ffx_fsr2_depth_clip.h
    fun depthClip(
        prevDepth: Texture2D, dilatedDepth: Texture2D, dilatedMotion: Texture2D,
        dstDisocclusionMask: Framebuffer, rw: Int, rh: Int
    ) {
        GFX.check()
        useFrame(rw, rh, true, dstDisocclusionMask) {
            val shader = depthClipShader
            shader.use()
            dilatedMotion.bind(2)
            dilatedDepth.bind(1)
            prevDepth.bind(0)
            shader.v2i("renderSizeI", rw, rh)
            shader.v2f("renderSize", rw - 1f, rh - 1f)
            shader.v1f("threshold", 0.125f)
            shader.v1f("depthClipBaseScale", 4f)
            val fov = RenderState.fovYRadians
            shader.v1f("sepFactor", 1.37e-5f * tan(fov * 0.5f) * rw / 2f)
            flat01.draw(shader)
        }
        GFX.check()
    }

    val accumulateShader = Shader(
        "accumulate", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
        listOf(
            Variable(GLSLType.S2D, "prepInputColor"),
            Variable(GLSLType.V2F, "downScaleFactor"),
            Variable(GLSLType.V2F, "invRenderSize"),
            Variable(GLSLType.V2F, "renderSizeM1"),
            Variable(GLSLType.V2I, "renderSizeI"),
            Variable(GLSLType.V2F, "displaySize"),
            Variable(GLSLType.V1B, "hdrColorInput"),
            Variable(GLSLType.V4F, "internalColorNWeight", VariableMode.OUT)
        ),
        "" +
                invTonemap +
                isOnScreen +
                "struct RectificationBoxData {\n" +
                "   vec3 center;\n" +
                "   vec3 vec;\n" +
                "   vec3 min;\n" +
                "   vec3 max;\n" +
                "};\n" +
                "struct RectificationBox {\n" +
                "   RectificationBoxData data;\n" +
                "   float centerWeight;\n" +
                "};\n" +
                "#define accumulationMaxOnMotion 4.0\n" +
                "vec3 yCoCgToRGB(vec3 color){\n" +
                "   float tmp = color.x - color.z * 0.5;\n" +
                "   vec3 rgb;\n" +
                "   float g = color.z + tmp;\n" +
                "   float b = tmp - color.y * 0.5;\n" +
                "   float r = b + color.y;\n" +
                "   return vec3(r,g,b);\n" +
                "}\n" +
                "vec2 computeKernelWeight(float historyWeight, float depthClipFactor,  float reactivityFactor){\n" +
                "   float bias = clamp(max(0.0, historyWeight - 0.5)/3.0, 0.0, 1.0);\n" +
                "   float x = 1.0 - reactivityFactor;\n" +
                "   vec2 weight = 1.0 + (1.0 / downScaleFactor - 1.0) * bias * x;\n" +
                "   weight *= 0.5 + depthClipFactor * 0.5;\n" +
                "   return min(weight, 1.99);\n" +
                "}\n" +
                "void boxVariance(inout RectificationBox box){\n" +
                "   box.centerWeight = abs(box.centerWeight) > 0.001 ? box.centerWeight : 1.0;\n" +
                "   box.data.center /= box.centerWeight;\n" +
                "   box.data.vec /= box.centerWeight;\n" +
                "   box.data.vec = sqrt(abs(box.data.vec - box.data.center * box.data.center));\n" +
                "}\n" +
                "void boxAddSample(inout RectificationBox box, vec3 color, float weight){\n" +
                "   box.data.min = min(box.data.min, color);\n" +
                "   box.data.max = max(box.data.max, color);\n" +
                "   vec3 w = color * weight;\n" +
                "   box.data.center += w;\n" +
                "   box.data.vec += color * w;\n" +
                "   box.centerWeight += weight;\n" +
                "}\n" +
                "void boxReset(inout RectificationBox box, vec3 color){\n" +
                "   box.centerWeight = 0.0;\n" +
                "   box.data.center = vec3(0.0);\n" +
                "   box.data.vec = vec3(0.0);\n" +
                "   box.data.min = color;\n" +
                "   box.data.max = color;\n" +
                "}\n" +
                "void deringing(RectificationBoxData box, inout vec3 color){ color = clamp(color,box.min,box.max); }\n" +
                "vec3 loadPreparedInputColor(ivec2 pos){ return texelFetch(prepInputColor, pos, 0).rgb; }\n" +
                // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/c8fc17d281665927e414b6b8117873cccbac0990/src/ffx-fsr2-api/shaders/ffx_fsr2_upsample.h
                "#define lanczos2SampleCount 16\n" +
                "float lanczos2ApproxSqNoClamp(float x2){\n" +
                "   float a = x2-2.5, b = 0.125*x2-0.5;\n" +
                "   return (a*a-2.25)*b*b;\n" +
                "}\n" +
                "float lanczos2ApproxSq(float x2){ return lanczos2ApproxSqNoClamp(min(x2,4.0)); }\n" +
                "float getUpsampleLanczosWeight(vec3 srcSampleOffset, vec2 kernelWeight){\n" +
                "   vec2 offsetBiased = srcSampleOffset * kernelWeight;\n" +
                "   return lanczos2ApproxSq(dot(offsetBiased,offsetBiased));\n" +
                "}\n" +
                "vec4 upsampleColorNWeight(vec2 uv, vec2 kernelWeight, inout RectificationBoxData clippingBox){\n" +
                "   vec2 unjittered = uv + (0.5 - jitter()) * invRenderSize;\n" + // todo didn't we already compute that?
                "   ivec2 offsetTL = ()-2;\n" + // todo add same formula as previously (just with -2)
                "   RectificationBox box;\n" +
                "   bool flipRow = unjittered.y > uv.y;\n" +
                "   bool flipCol = unjittered.x > uv.x;\n" +
                "   vec2 offsetTLf = vec2(offsetTL);\n" +
                "   ivec2 srcUV = ivec2(round(uv * renderSizeM1));\n" + // should be correct, I think :)
                "   vec3 samples[lanczos2SampleCount];\n" +
                "   for(int row=0;row<4;row++){\n" +
                "       for(int col=0;col<4;col++){\n" +
                "           int sampleIndex = col + (row << 2);\n" +
                // todo implement that function
                "           samples[sampleIndex] = loadPreparedInputColor(clamp(srcUV + offsetTL + ivec2(flipCol ? 3-col:col, flipRow ? 3-row:row), 0, renderSizeI-1));\n" +
                "       }\n" +
                "   }\n" +
                "   boxReset(box);\n" +
                "   vec3 color = vec3(0.0);\n" +
                "   float weight = 0.0;\n" +
                "   ivec2 srcUV = ivec2(round(uv * renderSizeM1));\n" +
                "   vec2 baseSampleOffset = vec2(unjittered - uv) * renderSizeM1;\n" +
                "   for(int sampleIdx=0;sampleIdx<lanczos2SampleCount;sampleIdx++){\n" +
                "       int row = sampleIdx >> 2;\n" +
                "       int col = sampleIdx & 3;\n" +
                "       ivec2 sampleColRow = ivec2(flipCol ? 3-col:col, flipRow ? 3-row:row);\n" +
                "       vec2 fOffset = offsetTLf + vec2(sampleColRow);\n" +
                "       vec2 srcSampleOffset = baseSampleOffset + fOffset;\n" + // pixel coords
                "       ivec2 srcSamplePos = srcUV + offsetTL + sampleColRow;\n" +
                "       float sampleWeight = float(isOnScreen(srcSamplePos,renderSizeI)) * getUpsampleLanczosWeight(srcSampleOffset, kernelWeight);\n" +
                "       if(col < 3 && row < 3){\n" +
                "           float offSq = dot(srcSampleOffset,srcSampleOffset);\n" +
                "           float weight = pow(1.0 - clamp(offSq/3.0, 0.0, 1.0), 2.0);\n" +
                "           boxAddSample(box,samples[sampleIdx],weight);\n" +
                "       }\n" +
                "       weight += sampleWeight;\n" +
                "       color += sampleWeight * samples[sampleIdx];\n" +
                "   }\n" +
                "   color /= (abs(weight) > 0.001 ? weight : 1.0);\n" +
                "   boxVariance(box);\n" + // RectificationBoxComputeVarianceBoxData
                "   clippingBox = box.data;\n" +
                "   deringing(box.data, color);\n" +
                "   #define averageLanczosWeightPerFrame 0.74\n" +
                "   if(any(lessThan(kernelWeight, vec2(1.0)))){\n" +
                "       weight = averageLanczosWeightPerFrame;\n" +
                "   }\n" +
                "   return vec4(color, max(0.0, weight));\n" +
                "}\n" +
                "void accumulate(vec2 uv, inout vec4 history, vec4 upsampled, float depthClipFactor, float velocity){\n" +
                "   history.w += upsampled.w;\n" +
                "   upsampled.rgb = yCoCgToRGB(upsampled.rgb);\n" +
                "   float alpha = upsampled.w / history.w;\n" +
                "   history.rgb = mix(history.rgb, upsampled.rgb, alpha);\n" +
                "   float maxAvgWeight = mix(maxAccumulationWeight, accumulationMaxOnMotion, clamp(velocity*10.0, 0.0, 1.0));\n" +
                "   history.w = min(history.w, maxAvgWeight);\n" +
                "}\n" +
                "float pow3(float x){ return x*x*x; }\n" +
                "void reprojectHistoryColor(vec2 pos, vec2 reprojectedPos, out vec4 colorNWeight){\n" +
                "   colorNWeight = historySample(reprojectedPos, displaySizeI);\n" +
                "   colorNWeight.rgb *= exposure;\n" +
                "   if(hdrColorInput){\n" +
                "       colorNWeight.rgb = tonemap(colorNWeight.rgb);\n" +
                "   }\n" +
                "   colorNWeight.rgb = rgbToYCoCg(colorNWeight.rgb);\n" +
                "}\n" +
                "void reprojectHistoryLockStatus(vec2 pos, vec2 reprojectedPos, out vec3 lockStatus){\n" +
                "   float lifetime = texelFetch(lockTex, pos).x;\n" + // todo load rw?
                "   lockStatus = sampleLockStatus(reprojectedPos);\n" +
                "   if(lifetime < 0.0) lockStatus.x = lifetime;\n" +
                "}\n" +
                "void main(){\n" +
                // todo lock status: vec3
                "   vec2 sampleUnjittered = uv + (0.5 - jitter()) * invRenderSize;\n" +
                "   ivec2 renderUV = ivec2(floor(uv * renderSizeM1));\n" +
                "   vec3 jittered = uv + jitter() * invRenderSize;\n" +
                "   vec2 motion = texture(motionTex, uv);\n" +
                "   float velocity = length(motion * displaySize);\n" + // todo motion seems to be in [-1,1] or [0,1] space...
                "   float depthClipFactor = clamp(sampleDepthClip(jittered), 0.0, 1.0);\n" +
                "   float lumaStabilityFactor = getLumaStabilityFactor(uv, velocity);\n" +
                "   float accumulationMask = transparencyAndCompositionMask(jittered).r;\n" +
                "   ivec2 offsetTL = ivec2(step(sampleUnjittered, uv));" + // edge, x; should be correct
                "   float reactiveMax = 1.0 - pow3(1.0 - texelFetch(reactiveMaxTex, renderUV + offsetTL, 0));\n" + // load reactive tex
                "   vec4 historyColorNWeight = vec4(0.0);\n" +
                // todo lock_lifetime_rem = 0
                // todo lock_temporal_luma = 1
                // todo lock_trust = 2
                "   vec3 lockStatus = vec3(0.0,0.0,1.0);\n" +
                "   bool isExistingSample = true;\n" +
                "   vec2 reprojectedUV = compReprojectedUV(uv,motion,isExistingSample);\n" +
                "   if(isExistingSample){\n" +
                "       reprojectHistoryColor(uv, reprojectedUV, historyColorNWeight);\n" +
                "       reprojectHistoryLockStatus(uv, reprojectedUV, lockStatus);\n" +
                "   }\n" +
                "   float lumaDiff = 0.0;\n" +
                "   vec3 lockState = postProcessLockStatus(uv,jittered,depthClipFactor,velocity,historyColorNWeight.w, lockStatus, lumaDiff);\n" +
                "   historyColorNWeight.w = min(historyColorNWeight.w, maxAccumulationWeight(velocity, reactiveMax, depthClipFactor, lumaDiff, lockState));\n" +
                "   float normLockLifetime = getNormRemainingLockLifetime(lockStatus);\n" +
                // "kill" accumulation based on shading change
                "   historyColorNWeight.w = min(historyColorNWeight, min(max(0.0, maxAccumulationWeight * pow(1.0 - lumaDiff, 2.0))));\n" +
                "   RectificationBox clippingBox;\n" +
                "   float kernelBias = accumulationMask * clamp(max(0.0, historyColorNWeight.w-0.5)/3.0, 0.0, 1.0);\n" +
                "   float reactiveWeighted = 0.0;\n" +
                "   lockStatus.z = min(lockStatus.z, min(depthClipFactor, 1.0 - pow(reactiveMask, 1.0/3.0)));\n" +
                "   vec2 kernelWeight = computeKernelWeight(historyColorNWeight.w, depthClipFactor, max(1.0 - lockStatus.z, reactiveMask));\n" +
                "   vec4 upsampledColorNWeight = upsampleColorNWeight(uv, kernelWeight, clippingBox);\n" +
                "   float lockContribution = getLockContribution(uv, accumulationMask, reactiveMask, lockStatus);\n" +
                "   if(historyColorNWeight.w > 0.0){\n" +
                "       rectifyHistory(clippingBox, historyColorNWeight, lockStatus, depthClipFactor, lumaStabilityFactor, lumaDiff, upsampledColorNWeight.w, lockContribution);\n" +
                "       historyColorNWeight.rgb = yCoCgToRGB(historyColorNWeight.rgb);\n" +
                "   }\n" +
                "   accumulate(uv, historyColorNWeight, upsampledColorNWeight, depthClipFactor, velocity);\n" +
                // remove weight in reactive areas
                "   historyColorNWeight -= upsampledColorNWeight.w * reactiveMask;\n" +
                "   if(hdrColorInput){\n" +
                "       historyColorNWeight.rgb = invTonemap(historyColorNWeight.rgb);\n" +
                "   }\n" +
                "   historyColorNWeight.rgb /= exposure;\n" + // why are we dividing???
                "   finalizeLockStatus(uv,lockStatus,upsampledColorNWeight.w);\n" +
                "   internalColorNWeight = historyColorNWeight;\n" +
                // why is this written twice???
                // "upscaledOutput = historyColorNWeight;\n" +
                "}\n"
    ).apply { setTextureIndices("prepInputColor") }

    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/c8fc17d281665927e414b6b8117873cccbac0990/src/ffx-fsr2-api/shaders/ffx_fsr2_accumulate.h
    fun reprojectNAccumulate(
        dilatedMotion: Texture2D, reactiveMask: Texture2D?,
        previousOutput: Texture2D,
        exposure: Texture2D, disocclusionMask: Texture2D,
        currentLuminance: Texture2D, lumaHistory: Texture2D,
        prepInputColor: Texture2D, lockStatus: Texture2D,
        dst: Framebuffer, rw: Int, rh: Int, pw: Int, ph: Int,
        // dstOutputBuffer: Framebuffer, dstReprojectedLocks: Framebuffer,
    ) {
        GFX.check()
        useFrame(pw, ph, true, dst, Renderer.copyRenderer) {
            val shader = accumulateShader
            shader.use()
            prepInputColor.bindTrulyNearest(0)
            shader.v2f("renderSizeM1", rw - 1f, rh - 1f)
            shader.v2f("invRenderSize", 1f / (rw - 1f), 1f / (rh - 1f))
            shader.v1b("hdrColorInput", hdrColorInput)
            shader.v2f("displaySize", pw.toFloat(), ph.toFloat())
            shader.v2i("renderSizeI", rw, rh)
            shader.v2f(
                "downScaleFactor",
                pw.toFloat() / rw.toFloat(),
                ph.toFloat() / rh.toFloat()
            ) // todo correct, or inverse?
            flat01.draw(shader)
            TODO()
        }
        GFX.check()
    }

    val autoExposureShader = Shader(
        "auto-exposure",
        ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
        listOf(
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V4F, "exposure", VariableMode.OUT)
        ), "" +
                "float rgbToY(vec3 rgb){ return max(dot(rgb, vec3(0.2126, 0.7152, 0.0722)), 0.001); }\n" +
                "void main(){ exposure = vec4(log(rgbToY(texture(tex,uv).rgb)), 0.0, 0.0, 1.0); }"
    )

    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/master/src/ffx-fsr2-api/shaders/ffx_fsr2_accumulate_pass.glsl
    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/c8fc17d281665927e414b6b8117873cccbac0990/src/ffx-fsr2-api/shaders/ffx_spd.h
    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/master/src/ffx-fsr2-api/shaders/ffx_fsr2_compute_luminance_pyramid_pass.glsl

    val exposureShader = Shader(
        "exposure",
        ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
        listOf(
            Variable(GLSLType.S2D, "currTex"),
            Variable(GLSLType.S2D, "prevTex"),
            Variable(GLSLType.V1F, "mixFactor"),
            Variable(GLSLType.V4F, "exposure", VariableMode.OUT),
        ),
        "void main(){\n" +
                // prevTex.g, because we want the raw (log) value
                "   float prev = texture(prevTex, uv).g, curr = texture(currTex, uv).r;\n" +
                "   curr = mix(prev,curr,mixFactor);\n" +
                "   exposure = vec4(0.104166667 * exp(-curr), curr, 0.0, 1.0);\n" +
                "}"
    ).apply { setTextureIndices("currTex", "prevTex") }

    fun autoExposureSPD(
        colorBuffer: Texture2D,
        prevExposure: Texture2D,
        dstCurrentLuminance: Framebuffer,
        dstExposure: Framebuffer,
        rw2: Int,
        rh2: Int
    ) {
        GFX.check()
        // where else is the luminance used?, where is it written?
        // todo confirm that it indeed is log(rgb2y(rgb))
        useFrame(rw2, rh2, true, dstCurrentLuminance, Renderer.copyRenderer) {
            // compute average luminance as fp16, half resolution; in log
            val shader = autoExposureShader
            shader.use()
            // linear filtering for averaging of neighbors... should work, doesn't it 😅
            colorBuffer.bind(0, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
            flat01.draw(shader)
        }
        GFX.check()
        // compute average exposure over whole screen (like reduce)
        // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/c8fc17d281665927e414b6b8117873cccbac0990/src/ffx-fsr2-api/shaders/ffx_fsr2_compute_luminance_pyramid.h
        val currExposure = FBStack["exposure", 1, 1, 4, true, 1, false]
        Reduction.reduce(dstCurrentLuminance.getTexture0(), Reduction.AVG, currExposure)
        useFrame(dstExposure) {
            val shader = exposureShader
            shader.use()
            prevExposure.bindTrulyNearest(1)
            currExposure.getTexture0().bindTrulyNearest(0)
            shader.v1f("mixFactor", 1f - exp(-Engine.deltaTime))
            flat01.draw(shader)
        }
        GFX.check()
    }

    val adjustInputShader = Shader(
        "adjust-input", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
        listOf(
            Variable(GLSLType.S2D, "colorTex"),
            Variable(GLSLType.S2D, "exposureTex"),
            Variable(GLSLType.S2D, "prevLumaHistory"),
            Variable(GLSLType.V1I, "frameIndex"),
            Variable(GLSLType.V1B, "hdrColorInput"),
            Variable(GLSLType.V4F, "history", VariableMode.OUT),
            Variable(GLSLType.V4F, "color", VariableMode.OUT)
        ), "" +
                tonemap +
                "float minDivByMax(float v0, float v1){\n" +
                "   float m = max(v0,v1);\n" +
                "   return m != 0.0 ? min(v0,v1)/m : 0.0;\n" +
                "}\n" +
                "float maxDivByMin(float v0, float v1){\n" +
                "   float m = min(v0,v1);\n" +
                "   return m != 0.0 ? max(v0,v1)/m : 0.0;\n" +
                "}\n" +
                "vec3 rgbToYcoCg(vec3 rgb){\n" +
                "   return vec3(\n" +
                "       dot(rgb,vec3( 0.25, 0.50, 0.25)),\n" +
                "       dot(rgb,vec3( 0.50, 0.00,-0.50)),\n" +
                "       dot(rgb,vec3(-0.25, 0.50,-0.25))\n" +
                "   );\n" +
                "}\n" +
                "float rgbToLuma(vec3 rgb){\n" +
                "   return dot(rgb, vec3(0.2126, 0.7152, 0.0722));\n" +
                "}\n" +
                "float rgbToPerceivedLuma(vec3 rgb){\n" +
                "   float luma = rgbToLuma(rgb);\n" +
                "   return luma <= ${216 / 24389f} ? luma * ${243.89f / 27f} : pow(luma, 1.0/3.0) * 1.16 - 0.16;\n" +
                "}\n" +
                "void main(){\n" +
                "   vec3 rgb = max(texture(colorTex, uv).rgb, 0.0);\n" +
                "   rgb *= texture(exposureTex, vec2(0.0)).x;\n" +
                "   if(hdrColorInput) rgb = tonemap(rgb);\n" +
                "   vec4 yCoCg = vec4(rgbToYcoCg(rgb), 0.0);\n" +
                "   float perceivedLuma = rgbToPerceivedLuma(rgb);\n" +
                // compute luma stability
                "   vec4 lumaHistory = vec4(texture(prevLumaHistory, uv).rgb, 0.0);\n" +
                "   if(frameIndex > 3){\n" +
                "       float diffs0 = minDivByMax(lumaHistory.z, perceivedLuma);\n" +
                "       float diffs1 = max(minDivByMax(lumaHistory.x, perceivedLuma), minDivByMax(lumaHistory.y, perceivedLuma));\n" +
                "       lumaHistory.a = clamp(diffs1 - diffs0, 0.0, 1.0);\n" +
                "   }\n" +
                "   lumaHistory.xyz = vec3(lumaHistory.yz, perceivedLuma);\n" +
                "   history = lumaHistory;\n" +
                // luma stability end
                "   yCoCg.w = pow(perceivedLuma, 1.0/6.0);\n" +
                "   color = yCoCg;\n" +
                "}"
    ).apply { setTextureIndices("colorTex", "exposureTex", "prevLumaHistory") }

    var frameIndex = 0

    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/master/src/ffx-fsr2-api/shaders/ffx_fsr2_prepare_input_color.h
    fun adjustInputColor(
        colorBuffer: Texture2D, exposure: Texture2D,
        prevLumaHistory: Texture2D,
        dst: Framebuffer, rw: Int, rh: Int
        // dstLumaHistory: Framebuffer, dstColorBuffer2: Framebuffer
    ) {
        GFX.check()
        useFrame(rw, rh, true, dst, Renderer.copyRenderer) {
            // calculating stability
            // moving history by 1 (sth like rgba -> gba0)
            val shader = adjustInputShader
            shader.use()
            colorBuffer.bindTrulyNearest(0)
            exposure.bindTrulyNearest(1)
            prevLumaHistory.bindTrulyNearest(2)
            shader.v1b("hdrColorInput", hdrColorInput)
            shader.v1i("frameIndex", frameIndex)
            flat01.draw(shader)
        }
        GFX.check()
    }

    // https://github.com/GPUOpen-Effects/FidelityFX-FSR2/blob/c8fc17d281665927e414b6b8117873cccbac0990/src/ffx-fsr2-api/shaders/ffx_fsr2_lock.h
    fun updateLocks(colorBuffer2: Texture2D, dstLockStatus: Framebuffer, rw: Int, rh: Int) {
        GFX.check()
        // first ComputeLock
        // then PreProcessReactiveMask
        // todo there is a double arrow... do we need read-write on a texture?
        // todo or do we just swap this texture, and the previous one?
        useFrame(rw, rh, true, dstLockStatus) {
            TODO()
        }
        GFX.check()
    }

}