package me.anno.gpu.shader.effects

import me.anno.cache.ICacheData
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.LightShaders
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.graph.render.Texture
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.random.Random

// implement the ideas of FSR2, but just in principle and much easier
// todo this is currently broken :/
class FSR2v2 : ICacheData {

    val dataTargetTypes = arrayOf(TargetType.Float16x4, TargetType.Float32x4)
    var data0 = Framebuffer("data", 1, 1, dataTargetTypes)
    var data1 = Framebuffer("data", 1, 1, dataTargetTypes)

    var previousDepth = Framebuffer("depth", 1, 1, TargetType.Float32x1)

    override fun destroy() {
        data0.destroy()
        data1.destroy()
        previousDepth.destroy()
    }

    companion object {

        val updateShader = Shader(
            "reproject", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V2F, "currJitter"),
                Variable(GLSLType.V2F, "prevJitter"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.S2D, "prevColorNWeights"),
                Variable(GLSLType.S2D, "prevDepths"),
                Variable(GLSLType.V2F, "renderSizeF"),
                Variable(GLSLType.V2I, "renderSizeI"),
                Variable(GLSLType.V2I, "displaySizeI"),
                Variable(GLSLType.V2F, "displaySizeF"),
                Variable(GLSLType.V1F, "sharpness"),
                Variable(GLSLType.V1F, "maxWeight"),
                Variable(GLSLType.V4F, "colorNWeightResult", VariableMode.OUT),
                Variable(GLSLType.V4F, "depthResult", VariableMode.OUT)
            ), "" +
                    "float sq(float x) { return x*x; }\n" +
                    "float dot2(vec3 x) { return dot(x,x); }\n" +
                    ShaderLib.octNormalPacking +
                    "void main(){\n" +
                    // iterate over all 4 old neighbors
                    "   vec4 colorNWeight = vec4(0.0);\n" +
                    "   vec4 weightedDepth = vec4(0.0);\n" +
                    "   vec2 motion2 = (currJitter - prevJitter) / renderSizeF;\n" +
                    // display space
                    "   vec2  srcUV0  = uv * renderSizeF - currJitter - 0.5, uvi01 = floor(srcUV0), duv0 = srcUV0-uvi01;\n" +
                    "   ivec2 srcUV0f = ivec2(uvi01);\n" +
                    "   for(int y=-1;y<=2;y++){\n" +
                    "       for(int x=-1;x<=2;x++){\n" +
                    "           ivec2 srcUV = srcUV0f + ivec2(x,y);\n" +
                    "           if(any(lessThan(srcUV, ivec2(0))) || any(greaterThanEqual(srcUV,displaySizeI))) continue;\n" +
                    "           vec3 fullMotion = texelFetch(motionTex,srcUV,0).xyz;\n" +
                    "           float depth0 = dot(texelFetch(depthTex,srcUV,0),depthMask);\n" +
                    "           float depth = log2(max(1e-16, depth0 - fullMotion.z));\n" +
                    "           vec4 normal0 = texelFetch(normalTex,srcUV,0);\n" +
                    "           vec3 normal = UnpackNormal(normalZW ? normal0.zw : normal0.xy);\n" +
                    "           vec2 motion = fullMotion.xy * 0.5 + motion2;\n" +
                    "           vec2 uviX = (uv - motion) * displaySizeF - 0.5,  uviX1 = floor(uviX), duvX = uviX-uviX1;\n" +
                    "           ivec2 uvx = ivec2(uviX1);\n" +
                    // iterate over all 4 hit pixels
                    "           for(int y2=0;y2<=1;y2++){\n" +
                    "               for(int x2=0;x2<=1;x2++){\n" +
                    "                   ivec2 dstUV = uvx + ivec2(x2,y2);\n" +
                    // accumulate old pixels
                    "                   if(all(greaterThanEqual(dstUV, ivec2(0))) && all(lessThan(dstUV, displaySizeI))){\n" +
                    "                       vec2 fg2 = mix(1.0-duvX, duvX, vec2(x2,y2));\n" +
                    "                       vec4 prevColorNWeight = texelFetch(prevColorNWeights,dstUV,0);\n" +
                    "                       if(prevColorNWeight.w > 0.001){\n" +
                    "                           vec4  depthData = texelFetch(prevDepths,dstUV,0);\n" +
                    "                           float prevDepth = depthData.x/prevColorNWeight.w;\n" +
                    "                           vec3  prevNormal = depthData.yzw/prevColorNWeight.w;\n" +
                    "                           float depthFactor = 1.0/(1.0+50.0*sq(prevDepth-depth));\n" +
                    "                           float normalFactor = 1.0/(1.0+50.0*dot2(prevNormal - normal));\n" +
                    "                           float w = fg2.x * fg2.y * depthFactor * normalFactor;\n" + // weight depending on distance to pixel
                    // todo better depth reconstruction; maybe near+far (<single edge>-optimized)
                    // todo by color difference (?)
                    "                           colorNWeight += prevColorNWeight * w;\n" +
                    "                           weightedDepth += depthData * w;\n" +
                    "                       }\n" +
                    "                   }\n" +
                    "               }\n" +
                    "           }\n" +
                    "       }\n" +
                    "   }\n" +
                    "   colorNWeight *= 0.125; weightedDepth *= 0.125;\n" +
                    "   if(colorNWeight.w > 1.0){\n" +
                    "       float factor = 1.0/colorNWeight.w;\n" +
                    "       colorNWeight *= factor;\n" +
                    "       weightedDepth *= factor;\n" +
                    "   }\n" +
                    "   for(int y=0;y<=1;y++){\n" +
                    "       for(int x=0;x<=1;x++){\n" +
                    // accumulate new pixels
                    "           ivec2 srcUV = srcUV0f + ivec2(x,y);\n" +
                    "           if(all(greaterThanEqual(srcUV, ivec2(0))) && all(lessThan(srcUV, renderSizeI))){\n" +
                    "               vec2 fg = mix(duv0, 1.0-duv0, vec2(x,y));\n" +
                    "               float w = 1.0/((1.0+sharpness*dot(fg,fg)) * maxWeight);\n" + // weight depending on distance to pixel
                    "               vec3 color = texelFetch(colorTex,srcUV,0).rgb;\n" +
                    "               float depth0 = dot(texelFetch(depthTex,srcUV,0), depthMask);\n" +
                    "               float depth = log2(max(1e-16, depth0));\n" +
                    "               vec4 normal0 = texelFetch(normalTex,srcUV,0);\n" +
                    "               vec3 normal = UnpackNormal(normalZW ? normal0.zw : normal0.xy);\n" +
                    "               colorNWeight += vec4(color * w, w);\n" +
                    "               weightedDepth += vec4(depth, normal) * w;\n" +
                    "           }\n" +
                    "       }\n" +
                    "   }\n" +
                    "   colorNWeightResult = colorNWeight;\n" +
                    "   depthResult = weightedDepth;\n" +
                    "}\n"
        ).apply {
            setTextureIndices(
                "colorTex",
                "depthTex",
                "normalTex",
                "motionTex",
                "prevColorNWeights",
                "prevDepths"
            )
        }

        val displayShader = Shader(
            "display", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "colorNWeights"),
                Variable(GLSLType.S2D, "depths"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   ivec2 uvi = ivec2(uv*vec2(textureSize(colorNWeights,0)));\n" +
                    "   vec4 colorNWeight = texelFetch(colorNWeights,uvi,0);\n" +
                    "   vec4 depth = texelFetch(depths,uvi,0)/colorNWeight.w;\n" +
                    "   result = vec4(colorNWeight.rgb/colorNWeight.w, 1.0);\n" +
                    // todo depth seems to be completely wrong... how???
                    // "   gl_FragDepth = pow(2.0,depth.x);\n" +
                    "}\n"
        ).apply { setTextureIndices("colorNWeights", "depths") }
    }

    var jx = 0f
    var jy = 0f
    var pjx = 0f
    var pjy = 0f
    val random = Random(1234L)
    var idx = 0
    val phaseCount get() = ceil(8f * lastScaleX * lastScaleY).toInt()
    var randomness = 1f

    fun halton(index: Int, base: Int): Float {
        var f = 1f
        var result = 0f
        var currIndex = index
        while (currIndex > 0) {
            f /= base
            result += f * (currIndex % base)
            currIndex /= base
        }
        return result
    }

    // todo unjitter gizmos

    fun jitter(m: Matrix4f, pw: Int, ph: Int) {
        pjx = jx
        pjy = jy
        jx = halton(idx + 1, 2) - 0.5f
        jy = halton(idx + 1, 3) - 0.5f
        // jx = random.nextFloat() - 0.5f
        // jy = random.nextFloat() - 0.5f
        jx *= randomness
        jy *= randomness
        m.m20(m.m20 + jx * 2f * lastScaleX / pw) // = /rw
        m.m21(m.m21 + jy * 2f * lastScaleY / ph) // = /rh
        if (++idx > phaseCount) idx = 0
    }

    private val tmpV = Vector3f()
    fun unjitter(m: Matrix4f, rot: Quaterniond, pw: Int, ph: Int) {
        val v = tmpV.set(jx * 2f * lastScaleX / pw, jy * 2f * lastScaleY / ph, 0f)
        rot.transform(v)
        m.m20(m.m20 - v.x)
        m.m21(m.m21 - v.y)
        m.m22(m.m22 - v.y)
    }

    var lastScaleX = 1f
    var lastScaleY = 1f

    fun calculate(
        color: Texture2D,
        depth: Texture2D,
        depthMask: String,
        normal: Texture2D,
        normalZW: Boolean,
        motion: Texture2D, // motion in 3d
        pw: Int, ph: Int,
    ) {

        val rw = color.width
        val rh = color.height
        lastScaleX = pw.toFloat() / rw
        lastScaleY = ph.toFloat() / rh
        if (previousDepth.pointer == 0) {
            previousDepth.width = rw
            previousDepth.height = rh
        }
        previousDepth.ensure()
        data1.ensure()
        GFXState.useFrame(pw, ph, true, data0) {
            val shader = updateShader
            shader.use()
            shader.v2f("prevJitter", pjx, pjy)
            shader.v2f("currJitter", jx, jy)
            shader.v2i("renderSizeI", rw, rh)
            shader.v2f("renderSizeF", rw.toFloat(), rh.toFloat())
            color.bindTrulyNearest(shader, "colorTex")
            depth.bindTrulyNearest(shader, "depthTex")
            normal.bindTrulyNearest(shader, "normalTex")
            motion.bindTrulyNearest(shader, "motionTex")
            data1.bindTextures(4, Filtering.TRULY_NEAREST, Clamping.CLAMP)
            shader.v2i("displaySizeI", data1.width, data1.height)
            shader.v2f("displaySizeF", data1.width.toFloat(), data1.height.toFloat())
            shader.v1f("sharpness", 3f * lastScaleX * lastScaleY) // a guess
            shader.v1b("normalZW", normalZW)
            shader.v4f("depthMask", DeferredSettings.singleToVector[depthMask]!!)
            shader.v1f("maxWeight", 5f)
            SimpleBuffer.flat01.draw(shader)
        }
        // render result
        val shader = displayShader
        shader.use()
        data0.bindTextures(0, Filtering.TRULY_NEAREST, Clamping.CLAMP)
        SimpleBuffer.flat01.draw(shader)
        val tmp = data0
        data0 = data1
        data1 = tmp
    }

    fun render(
        view: RenderView,
        w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int,
        buffer: IFramebuffer, deferred: DeferredSettings,
        lightNBuffer1: IFramebuffer,
        baseSameDepth1: IFramebuffer
    ) {

        val motion = FBStack["motion", w, h, 4, BufferQuality.HIGH_16, 1, DepthBufferType.INTERNAL]
        val motionRenderer = Renderers.rawAttributeRenderers[DeferredLayerType.MOTION]
        view.drawScene(w, h, motionRenderer, motion, changeSize = false, hdr = true)

        val tex = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
        view.drawSceneLights(buffer, tex.tex as Texture2D, tex.mask!!, lightNBuffer1)

        val ssao = TextureLib.blackTexture
        val pw = x1 - x0
        val ph = y1 - y0

        // use the existing depth buffer for the 3d ui
        val skybox = view.pipeline.bakedSkybox?.getTexture0() ?: TextureLib.blackCube
        GFXState.useFrame(w, h, true, baseSameDepth1) {
            // don't write depth
            GFXState.depthMask.use(false) {
                LightShaders.combineLighting(
                    deferred, true, buffer,
                    lightNBuffer1, ssao, skybox
                )
            }
        }

        val tmp1 = FBStack["fsr", pw, ph, 4, false, 1, DepthBufferType.INTERNAL]
        GFXState.useFrame(tmp1) {
            GFXState.depthMode.use(DepthMode.CLOSE) {
                tmp1.clearDepth()
                GFXState.blendMode.use(null) {
                    val depth = Texture.texture(buffer, deferred, DeferredLayerType.DEPTH)
                    calculate(
                        baseSameDepth1.getTexture0() as Texture2D,
                        depth.tex as Texture2D, depth.mapping,
                        deferred.findTexture(buffer, DeferredLayerType.NORMAL) as Texture2D,
                        deferred.zw(DeferredLayerType.NORMAL),
                        motion.getTexture0() as Texture2D,
                        pw, ph
                    )
                }
                // todo why is it still jittering?
                val tmp = JomlPools.mat4f.create()
                tmp.set(view.cameraMatrix)
                unjitter(view.cameraMatrix, view.cameraRotation, pw, ph)
                // setRenderState()
                view.drawGizmos(GFXState.currentBuffer, true, drawDebug = true)
                view.cameraMatrix.set(tmp)
                // setRenderState()
                JomlPools.mat4f.sub(1)
            }
        }

        DrawTextures.drawTexture(x0, y0 + ph, pw, -ph, tmp1.getTexture0(), true)
    }
}