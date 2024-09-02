package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.gpu.texture.TextureLib.grayTexture
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.isZWMapping
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max

// inspired by https://threejs.org/examples/?q=post#webgl_postprocessing_pixel
class PixelationNode : RenderViewNode(
    "Pixelation",
    listOf(
        "Int", "Pixel Size", // keep this small, and rather reduce render resolution
        "Float", "NormalEdge Strength", // brightening of edges in normals
        "Float", "NormalEdge Threshold",
        "Float", "DepthEdge Strength", // darkening of edges in depth
        "Texture", "Illuminated",
        "Texture", "Normal",
        "Texture", "Depth",
    ), listOf(
        "Texture", "Illuminated",
        "Int", "Width",
        "Int", "Height"
    )
) {

    init {
        setInput(1, 6) // pixel size
        setInput(2, 0.3f) // normal strength
        setInput(3, 0.2f) // normal threshold
        setInput(4, 0.4f) // depth strength
    }

    override fun executeAction() {
        val pixelSize = clamp(getIntInput(1), 1, 32)
        val normalStrength = max(getFloatInput(2), 0f)
        val depthStrength = clamp(getFloatInput(3))
        val normalThreshold = max(getFloatInput(3), 1e-7f)
        val color0 = getInput(5) as? Texture
        val normal0 = getInput(6) as? Texture
        val depth0 = getInput(7) as? Texture
        val color = color0.texOrNull ?: grayTexture
        val normal = normal0.texOrNull ?: blackTexture
        val depth = depth0.texOrNull ?: depthTexture
        if (pixelSize == 1 && normalStrength == 0f && depthStrength == 0f) {
            setOutput(1, color0 ?: Texture(missingTexture))
        } else {
            timeRendering(name, timer) {
                val width0 = max(color.width, max(normal.width, depth.width))
                val height0 = max(color.height, max(normal.height, depth.height))
                val width1 = max(width0 / pixelSize, 1)
                val height1 = max(height0 / pixelSize, 1)
                val result = FBStack[name, width1, height1, 3, true, 1, DepthBufferType.NONE]
                useFrame(result, copyRenderer) {
                    val shader = shader
                    shader.use()
                    shader.v1f("normalStrength", if (normal == blackTexture) 0f else normalStrength)
                    shader.v1f("depthStrength", if (depth == depthTexture) 0f else depthStrength)
                    color.bindTrulyNearest(shader, "colorTex")
                    normal.bindTrulyNearest(shader, "normalTex")
                    depth.bindTrulyNearest(shader, "depthTex")
                    shader.v1b("normalZW", normal0.isZWMapping)
                    shader.v4f("depthMask", depth0.mask)
                    shader.v2f("duv0", 1f / width0, 1f / height0)
                    shader.v2f("duv", 1f / width1, 1f / height1)
                    shader.v1i("pixelSize", pixelSize)
                    shader.v1f("normalThreshold", normalThreshold)
                    bindDepthUniforms(shader)
                    flat01.draw(shader)
                }
                setOutput(1, Texture(result.getTexture0()))
                setOutput(2, width1)
                setOutput(3, height1)
            }
        }
    }

    companion object {
        val shader = Shader(
            "pixelation", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F, "normalStrength"),
                Variable(GLSLType.V1F, "normalThreshold"),
                Variable(GLSLType.V1F, "depthStrength"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "depthMask"),
                Variable(GLSLType.V2F, "duv"),
                Variable(GLSLType.V2F, "duv0"),
                Variable(GLSLType.V1I, "pixelSize"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthVars, rawToDepth + octNormalPacking + "" +
                    "ivec2 getUVs(int dx, int dy, ivec2 size){\n" +
                    "   vec2 uv1 = uv + duv * vec2(dx,dy);\n" +
                    "   ivec2 uv2 = max(ivec2(uv1 * vec2(size)),ivec2(0));\n" +
                    "   uv2 = uv2 - uv2 % pixelSize;\n" +
                    "   return min(uv2 + (pixelSize >> 1), size-1);\n" +
                    "}\n" +
                    "float getDepth(int dx, int dy){\n" +
                    "   ivec2 size = textureSize(depthTex,0);\n" +
                    "   vec4 raw = texelFetch(depthTex,getUVs(dx,dy,size),0);\n" +
                    "   return dot(depthMask, raw);\n" +
                    "}\n" +
                    "vec3 getNormal(int dx, int dy){\n" +
                    "   ivec2 size = textureSize(normalTex,0);\n" +
                    "   vec4 raw4 = texelFetch(normalTex,getUVs(dx,dy,size),0);\n" +
                    "   vec2 raw2 = normalZW ? raw4.zw : raw4.xy;\n" +
                    "   return UnpackNormal(raw2);\n" +
                    "}\n" +
                    // https://threejs.org/examples/jsm/postprocessing/RenderPixelatedPass.js
                    "float depthEdgeIndicator() {\n" +
                    "   float diff = 0.0;\n" +
                    "   float px = getDepth(1, 0), mx = getDepth(-1, 0);\n" +
                    "   float py = getDepth(0, 1), my = getDepth(0, -1);\n" +
                    "   if(getDepth(0,0) > min(min(px,py),min(mx,my))){\n" +
                    "       float dx = px-mx, dy = py-my;\n" +
                    "       float scale = 1.0 / max(1e-7, max(max(px,py),max(mx,my)));\n" +
                    "       return smoothstep(0.01, 0.02, (dx*dx+dy*dy)*(scale*scale));\n" +
                    "   } else return 0.0;\n" +
                    "}\n" +
                    "float neighborNormalEdgeIndicator(int x, int y, float depth, vec3 normal) {\n" +
                    "   float normalDiff = max(1.0 - dot(normal, getNormal(x, y)), 0.0);\n" +
                    // Only the shallower pixel should detect the normal edge.
                    "   return depth > getDepth(x, y) ? normalDiff : 0.0;\n" +
                    "}\n" +
                    "float normalEdgeIndicator() {\n" +
                    "   float indicator = 0.0, depth = getDepth(0,0);\n" +
                    "   vec3 normal = getNormal(0,0);\n" +
                    "   indicator += neighborNormalEdgeIndicator(0, -1, depth, normal);\n" +
                    "   indicator += neighborNormalEdgeIndicator(0, 1, depth, normal);\n" +
                    "   indicator += neighborNormalEdgeIndicator(-1, 0, depth, normal);\n" +
                    "   indicator += neighborNormalEdgeIndicator(1, 0, depth, normal);\n" +
                    "   return step(normalThreshold, indicator);\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   float dei = depthStrength > 0.0 ? depthEdgeIndicator() : 0.0;\n" +
                    "   float nei = normalStrength > 0.0 && dei <= 0.0 ? normalEdgeIndicator() : 0.0;\n" +
                    "   float strength = dei > 0.0 ? 1.0 - dei * depthStrength : 1.0 + nei * normalStrength;\n" +
                    // average color over that area...
                    //  might be expensive, but we compute few pixels, soo...
                    "   vec3 color = vec3(0.0);\n" +
                    "   int di=pixelSize>>1;\n" +
                    "   for(int dy=0;dy<pixelSize;dy++){\n" +
                    "       for(int dx=0;dx<pixelSize;dx++){\n" +
                    "           vec2 uvi = uv+duv0*vec2(dx-di,dy-di);\n" +
                    "           color += texture(colorTex,uvi).xyz;\n" +
                    "       }\n" +
                    "   }\n" +
                    "   color *= 1.0 / float(pixelSize * pixelSize);\n" +
                    "   result = vec4(color * vec3(strength), 1.0);\n" +
                    "}\n"
        )
    }
}