package me.anno.gpu.shader.effects

import me.anno.cache.ICacheData
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.maths.Maths.posMod
import me.anno.utils.structures.maps.LazyMap
import org.joml.Matrix4f
import kotlin.math.round

/**
 * implement the ideas of FSR2, but just in principle and much easier
 * */
class FSR2v2 : ICacheData {
    companion object {
        private val dataTargetTypes = listOf(TargetType.UInt8x4, TargetType.Float32x1)
        val updateShader = LazyMap { depthMaskI: Int ->
            val depthMask = "xyzw"[depthMaskI]
            Shader(
                "reproject", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
                listOf(
                    Variable(GLSLType.V2F, "currJitter"),
                    Variable(GLSLType.V2F, "prevJitter"),
                    Variable(GLSLType.S2D, "colorTex"),
                    Variable(GLSLType.S2D, "depthTex"),
                    Variable(GLSLType.V4F, "depthMask"),
                    Variable(GLSLType.S2D, "motionTex"),
                    Variable(GLSLType.S2D, "prevColors"),
                    Variable(GLSLType.S2D, "prevDepths"),
                    Variable(GLSLType.V2F, "renderSizeF"), // small size
                    Variable(GLSLType.V2I, "renderSizeI"),
                    Variable(GLSLType.V2I, "displaySizeI"), // big size
                    Variable(GLSLType.V2F, "displaySizeF"),
                    Variable(GLSLType.V2I, "chosenPixel"),
                    Variable(GLSLType.V2I, "scaleI"),
                    Variable(GLSLType.V1F, "sharpness"),
                    Variable(GLSLType.V1F, "maxWeight"),
                    Variable(GLSLType.V4F, "colorResult", VariableMode.OUT),
                    Variable(GLSLType.V4F, "depthResult", VariableMode.OUT)
                ) + DepthTransforms.depthVars, "" +
                        "float sq(float x) { return x*x; }\n" +
                        "float dot2(vec3 x) { return dot(x,x); }\n" +
                        DepthTransforms.rawToDepth +
                        ShaderLib.octNormalPacking +
                        "void main(){\n" +
                        "   ivec2 dstUV = ivec2(uv * displaySizeF);\n" +
                        "   bool isExact = dstUV % scaleI == chosenPixel;\n" +
                        "   ivec2 srcUV = ivec2(uv * renderSizeF);\n" +
                        "   float depth = texelFetch(depthTex,srcUV,0).$depthMask;\n" +
                        "   if(!isExact){\n" +
                        // use motion to reproject depth and color
                        // find best matching motion (by depth,normal?) to reduce ghosting
                        "       float bestMotionError = 1e38; vec3 bestMotion = vec3(0.0);\n" +
                        "       float minDepth = 1e38, maxDepth = 0.0;\n" +
                        "       for(int y=max(srcUV.y-1,0);y<min(srcUV.y+2,renderSizeI.y);y++){\n" +
                        "           for(int x=max(srcUV.x-1,0);x<min(srcUV.x+2,renderSizeI.x);x++){\n" +
                        "                   float depthI = texelFetch(depthTex,ivec2(x,y),0).$depthMask;\n" +
                        "                   minDepth = min(depthI,minDepth);\n" +
                        "                   maxDepth = max(depthI,maxDepth);\n" +
                        "                   float error = abs(depthI - depth);\n" +
                        "                   if(error < bestMotionError){\n" +
                        "                       bestMotionError = error;\n" +
                        "                       bestMotion = texelFetch(motionTex,ivec2(x,y),0).xyz;\n" +
                        "                   }\n" +
                        "               " +
                        "           }\n" +
                        "       }\n" +
                        "       vec2 motion = 0.5 * bestMotion.xy + (currJitter-prevJitter) / renderSizeF;\n" +
                        "       ivec2 dstUV1 = ivec2((uv - motion) * displaySizeF);\n" +
                        "       isExact = dstUV1.x < 0 || dstUV1.y < 0 || dstUV1.x >= displaySizeI.x || dstUV1.y >= displaySizeI.y;\n" +
                        "       if(!isExact){\n" +
                        // if all neighbors have vastly different depth, discard our pixel
                        "           colorResult = texelFetch(prevColors,dstUV1,0);\n" +
                        "           depthResult = texelFetch(prevDepths,dstUV1,0);\n" +
                        // collect depth inside region; if out of range, kill pixel
                        "           float d0 = min(rawToDepth(minDepth),1e37);\n" +
                        "           float d1 = min(rawToDepth(maxDepth),1e37);\n" +
                        // our ghosting prevention removes small details :/
                        "           minDepth = min(d0,d1) * 0.8; maxDepth = max(d0,d1) * 1.25;\n" +
                        "           float z0 = min(rawToDepth(depthResult.x),1e37);\n" +
                        "           float z1 = min(rawToDepth(depthResult.x+bestMotion.z),1e37);\n" +
                        "           float dz = abs(z1-z0)/min(z0,z1);\n" +
                        "           colorResult.w = max(colorResult.w - 5.0 * dz, 0.0);\n" +
                        "           isExact = colorResult.w < 0.1 || z0 < minDepth || depthResult.x > maxDepth;\n" +
                        "           depthResult.x += bestMotion.z;\n" +
                        "       }\n" +
                        "   }\n" +
                        "   if(isExact){\n" +
                        "       vec3 color = texelFetch(colorTex,srcUV,0).rgb;\n" +
                        "       colorResult = vec4(color,1.0);\n" +
                        "       depthResult = vec4(depth,0.0,0.0,1.0);\n" +
                        "   }\n" +
                        "}\n"
            ).apply { ignoreNameWarnings("d_camRot,d_uvCenter,cameraMatrixInv") }
        }
    }

    class PerViewData {
        var data0 = IFramebuffer.createFramebuffer("fsr2-0", 1, 1, 1, dataTargetTypes, DepthBufferType.NONE)
        var data1 = IFramebuffer.createFramebuffer("fsr2-1", 1, 1, 1, dataTargetTypes, DepthBufferType.NONE)
    }

    val views = LazyMap { _: Int -> PerViewData() }

    override fun destroy() {
        for (view in views.values) {
            view.data0.destroy()
            view.data1.destroy()
        }
    }

    private var jx = 0f
    private var jy = 0f
    private var pjx = 0f
    private var pjy = 0f
    private var sequenceIndex = 0

    // todo unjitter gizmos

    var randomMap = IntArray(0)
    fun jitter(m: Matrix4f, pw: Int, ph: Int) {
        pjx = jx
        pjy = jy
        val scaleX = round(lastScaleX).toInt()
        val scaleY = round(lastScaleX).toInt()
        if (randomMap.size != scaleX * scaleY) {
            randomMap = IntArray(scaleX * scaleY) { it }
            randomMap.shuffle() // todo good shuffle pattern?
        }
        val si = randomMap[posMod(sequenceIndex++, scaleX * scaleY)]
        jx = (si % scaleX + 0.5f) / scaleX - 0.5f
        jy = (si / scaleX + 0.5f) / scaleY - 0.5f
        val renderSizeX = pw / scaleX
        val renderSizeY = ph / scaleY
        tmpM.set(m)
        m.m20(m.m20 + jx * 2f / renderSizeX)
        m.m21(m.m21 + jy * 2f / renderSizeY)
    }

    val tmpM = Matrix4f()
    fun unjitter(m: Matrix4f) {
        m.set(tmpM).rotateInv(RenderState.cameraRotation)
    }

    var lastScaleX = 1f
    var lastScaleY = 1f

    fun calculate(
        color: ITexture2D,
        depth: ITexture2D,
        depthMask: Int,
        motion: ITexture2D, // motion in 3d
        pw: Int, ph: Int,
        scaleX: Int, scaleY: Int
    ) {
        val rw = color.width
        val rh = color.height
        lastScaleX = scaleX.toFloat()
        lastScaleY = scaleY.toFloat()
        val view = views[RenderState.viewIndex]
        val data1 = view.data1
        val data0 = view.data0
        data1.ensure()
        GFXState.useFrame(pw, ph, true, data0) {
            val shader = updateShader[depthMask]
            shader.use()
            shader.v2f("prevJitter", pjx, pjy)
            shader.v2f("currJitter", jx, jy)
            shader.v2i("scaleI", scaleX, scaleY)
            shader.v2i("renderSizeI", rw, rh)
            shader.v2f("renderSizeF", rw.toFloat(), rh.toFloat())
            color.bindTrulyNearest(shader, "colorTex")
            depth.bindTrulyNearest(shader, "depthTex")
            motion.bindTrulyNearest(shader, "motionTex")
            data1.getTextureI(0).bindTrulyNearest(shader, "prevColors")
            data1.getTextureI(1).bindTrulyNearest(shader, "prevDepths")
            shader.v2i("displaySizeI", data1.width, data1.height)
            shader.v2f("displaySizeF", data1.width.toFloat(), data1.height.toFloat())
            shader.v2i(
                "chosenPixel",
                ((jx + 0.5f) * scaleX).toInt(),
                ((jy + 0.5f) * scaleY).toInt(),
            )
            DepthTransforms.bindDepthUniforms(shader)
            SimpleBuffer.flat01.draw(shader)
        }
        view.data0 = data1
        view.data1 = data0
    }
}