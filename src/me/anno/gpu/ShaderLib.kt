package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Filtering
import me.anno.mesh.fbx.model.FBXShader
import me.anno.objects.effects.MaskType
import me.anno.objects.effects.types.GLSLLib
import me.anno.objects.modes.UVProjection
import me.anno.studio.rems.Scene.noiseFunc
import org.lwjgl.opengl.GL20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI

object ShaderLib {

    lateinit var flatShader: Shader
    lateinit var flatShaderStriped: Shader
    lateinit var flatShaderGradient: Shader
    lateinit var flatShaderTexture: Shader
    lateinit var subpixelCorrectTextShader: Shader
    lateinit var shader3DPolygon: Shader
    lateinit var shader3D: Shader
    lateinit var shader3DforText: Shader
    lateinit var shader3DOutlinedText: Shader
    lateinit var shader3DRGBA: Shader
    lateinit var shader3DYUV: Shader
    lateinit var shader3DARGB: Shader
    lateinit var shader3DBGRA: Shader
    lateinit var shader3DCircle: Shader
    lateinit var shader3DSVG: Shader
    lateinit var lineShader3D: Shader
    lateinit var shader3DMasked: Shader
    lateinit var shader3DGaussianBlur: Shader
    lateinit var shader3DBoxBlur: Shader
    lateinit var shaderObjMtl: Shader
    lateinit var shaderFBX: Shader
    lateinit var copyShader: Shader

    /**
     * our code only uses 3, I think
     * */
    const val maxOutlineColors = 6

    const val brightness = "" +
            "float brightness(vec3 color){\n" +
            "   return sqrt(0.299*color.r*color.r + 0.587*color.g*color.g + 0.114*color.b*color.b);\n" +
            "}\n" +
            "float brightness(vec4 color){\n" +
            "   return sqrt(0.299*color.r*color.r + 0.587*color.g*color.g + 0.114*color.b*color.b);\n" +
            "}\n"


    const val bicubicInterpolation = "" +
            // https://www.paulinternet.nl/?page=bicubic
            "vec4 cubicInterpolation(vec4 p0, vec4 p1, vec4 p2, vec4 p3, float x){\n" +
            "   return p1 + 0.5 * x*(p2 - p0 + x*(2.0*p0 - 5.0*p1 + 4.0*p2 - p3 + x*(3.0*(p1 - p2) + p3 - p0)));\n" +
            "}\n" +
            "vec4 cubicInterpolation(sampler2D tex, vec2 uv, float du, float x){\n" +
            "   vec4 p0 = texture(tex, vec2(uv.x - du, uv.y));\n" +
            "   vec4 p1 = texture(tex, vec2(uv.x     , uv.y));\n" +
            "   vec4 p2 = texture(tex, vec2(uv.x + du, uv.y));\n" +
            "   vec4 p3 = texture(tex, vec2(uv.x+2*du, uv.y));\n" +
            "   return cubicInterpolation(p0, p1, p2, p3, x);\n" +
            "}\n" +
            "vec4 bicubicInterpolation(sampler2D tex, vec2 uv, vec2 duv){\n" +
            "   uv -= 0.5 * duv;\n" +
            "   vec2 xy = fract(uv / duv);\n" +
            "   vec4 p0 = cubicInterpolation(tex, vec2(uv.x, uv.y - duv.y), duv.x, xy.x);\n" +
            "   vec4 p1 = cubicInterpolation(tex, vec2(uv.x, uv.y        ), duv.x, xy.x);\n" +
            "   vec4 p2 = cubicInterpolation(tex, vec2(uv.x, uv.y + duv.y), duv.x, xy.x);\n" +
            "   vec4 p3 = cubicInterpolation(tex, vec2(uv.x, uv.y+2*duv.y), duv.x, xy.x);\n" +
            "   return cubicInterpolation(p0, p1, p2, p3, xy.y);\n" +
            "}\n"

    // https://en.wikipedia.org/wiki/ASC_CDL
    // color grading with asc cdl standard
    const val ascColorDecisionList = "" +
            "uniform vec3 cgSlope, cgOffset, cgPower;\n" +
            "uniform float cgSaturation;\n" +
            "vec3 colorGrading(vec3 raw){\n" +
            "   vec3 color = pow(max(vec3(0), raw * cgSlope + cgOffset), cgPower);\n" +
            "   float gray = brightness(color);\n" +
            "   return mix(vec3(gray), color, cgSaturation);\n" +
            "}\n"

    const val rgb2uv = "" +
            "vec2 RGBtoUV(vec3 rgb){\n" +
            "   vec4 rgba = vec4(rgb,1);\n" +
            "   return vec2(\n" +
            "       dot(rgba, vec4(-0.169, -0.331,  0.500, 0.5)),\n" +
            "       dot(rgba, vec4( 0.500, -0.419, -0.081, 0.5)) \n" +
            "   );\n" +
            "}\n"

    const val yuv2rgb ="" +
            "vec3 yuv2rgb(vec3 yuv){" +
            "   yuv -= vec3(${16f / 255f}, 0.5, 0.5);\n" +
            "   return vec3(" +
            "       dot(yuv, vec3( 1.164,  0.000,  1.596))," +
            "       dot(yuv, vec3( 1.164, -0.392, -0.813))," +
            "       dot(yuv, vec3( 1.164,  2.017,  0.000)));\n" +
            "}"

    val maxColorForceFields = DefaultConfig["objects.attractors.color.maxCount", 12]
    val getColorForceFieldLib = "" +
            // additional weights?...
            "uniform int forceFieldColorCount;\n" +
            "uniform vec4 forceFieldBaseColor;\n" +
            "uniform vec4[$maxColorForceFields] forceFieldColors;\n" +
            "uniform vec4[$maxColorForceFields] forceFieldPositionsNWeights;\n" +
            "uniform vec4[$maxColorForceFields] forceFieldColorPowerSizes;\n" +
            "vec4 getForceFieldColor(){\n" +
            "   float sumWeight = 0.25;\n" +
            "   vec4 sumColor = sumWeight * forceFieldBaseColor;\n" +
            "   for(int i=0;i<forceFieldColorCount;i++){\n" +
            "       vec4 positionNWeight = forceFieldPositionsNWeights[i];\n" +
            "       vec3 positionDelta = localPosition - positionNWeight.xyz;\n" +
            "       vec4 powerSize = forceFieldColorPowerSizes[i];\n" +
            "       float weight = positionNWeight.w / (1.0 + pow(dot(powerSize.xyz * positionDelta, positionDelta), powerSize.w));\n" +
            "       sumWeight += weight;\n" +
            "       vec4 localColor = forceFieldColors[i];\n" +
            "       sumColor += weight * localColor * localColor;\n" +
            "   }\n" +
            "   return sqrt(sumColor / sumWeight);\n" +
            "}\n"

    val colorForceFieldBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(4 * maxColorForceFields)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    val maxUVForceFields = DefaultConfig["objects.attractors.scale.maxCount", 12]
    val getUVForceFieldLib = "" +
            "uniform int forceFieldUVCount;\n" +
            "uniform vec3[$maxUVForceFields] forceFieldUVs;\n" + // xyz
            "uniform vec4[$maxUVForceFields] forceFieldUVSpecs;\n" + // size, power
            "vec3 getForceFieldUVs(vec3 uvw){\n" +
            "   vec3 sumUVs = uvw;\n" +
            "   for(int i=0;i<forceFieldUVCount;i++){\n" +
            "       vec3 position = forceFieldUVs[i];\n" +
            "       vec4 sizePower = forceFieldUVSpecs[i];\n" +
            "       vec3 positionDelta = uvw - position;\n" +
            "       float weight = sizePower.x / (1.0 + pow(sizePower.z * dot(positionDelta, positionDelta), sizePower.w));\n" +
            "       sumUVs += weight * positionDelta;\n" +
            "   }\n" +
            "   return sumUVs;\n" +
            "}\n" +
            "vec2 getForceFieldUVs(vec2 uv){\n" +
            "   vec2 sumUVs = uv;\n" +
            "   for(int i=0;i<forceFieldUVCount;i++){\n" +
            "       vec3 position = forceFieldUVs[i];\n" +
            "       vec4 sizePower = forceFieldUVSpecs[i];\n" +
            "       vec2 positionDelta = (uv - position.xy) * sizePower.xy;\n" +
            "       float weight = 1.0 / (1.0 + pow(sizePower.z * dot(positionDelta, positionDelta), sizePower.w));\n" +
            "       sumUVs += weight * positionDelta;\n" +
            "   }\n" +
            "   return sumUVs;\n" +
            "}\n"

    val uvForceFieldBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(3 * maxUVForceFields)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    const val hasForceFieldColor = "(forceFieldColorCount > 0)"
    const val hasForceFieldUVs = "(forceFieldUVCount > 0)"

    val getTextureLib = "" +
            bicubicInterpolation +
            getUVForceFieldLib +
            // the uvs correspond to the used mesh
            // used meshes are flat01 and cubemapBuffer
            "uniform vec2 textureDeltaUV;\n" +
            "uniform int filtering, uvProjection;\n" +
            /*"vec2 getProjectedUVs(vec2 uv){\n" +
            "   switch(uvProjection){\n" +
            "       case ${UVProjection.TiledCubemap.id}:\n" +
            "           return uv;\n" + // correct???
            "       default:\n" +
            "           return uv;\n" +
            "   }\n" +
            "}\n" +*/
            "vec2 getProjectedUVs(vec2 uv){ return uv; }\n" +
            "vec2 getProjectedUVs(vec3 uvw){\n" +
            //"   switch(uvProjection){\n" +
            //"       case ${UVProjection.Equirectangular.id}:\n" +
            //"       default:\n" +
            "           float u = atan(uvw.z, uvw.x)*${0.5 / PI}+0.5;\n " +
            "           float v = atan(uvw.y, length(uvw.xz))*${1.0 / PI}+0.5;\n" +
            "           return vec2(u, v);\n" +
            //"   }\n" +
            "}\n" +
            "vec2 getProjectedUVs(vec2 uv, vec3 uvw){\n" +
            "   return uvProjection == ${UVProjection.Equirectangular.id} ?\n" +
            "       ($hasForceFieldUVs ? getProjectedUVs(getForceFieldUVs(uvw)) : getProjectedUVs(uvw)) :\n" +
            "       ($hasForceFieldUVs ? getProjectedUVs(getForceFieldUVs(uv))  : getProjectedUVs(uv));\n" +
            "}\n" +
            "vec4 getTexture(sampler2D tex, vec2 uv, vec2 duv){\n" +
            "   switch(filtering){\n" +
            "       case ${Filtering.NEAREST.id}:\n" +
            "       case ${Filtering.LINEAR.id}:\n" +
            "           return texture(tex, uv);\n" +
            "       case ${Filtering.CUBIC.id}:\n" +
            "           return bicubicInterpolation(tex, uv, duv);\n" +
            "   }\n" +
            "}\n" +
            "vec4 getTexture(sampler2D tex, vec2 uv){\n" +
            "   switch(filtering){\n" +
            "       case ${Filtering.NEAREST.id}:\n" +
            "       case ${Filtering.LINEAR.id}:\n" +
            "           return texture(tex, uv);\n" +
            "       case ${Filtering.CUBIC.id}:\n" +
            "           return bicubicInterpolation(tex, uv, textureDeltaUV);\n" +
            "   }\n" +
            "}\n"

    fun init() {

        // make this customizable?

        // color only for a rectangle
        // (can work on more complex shapes)
        flatShader = Shader(
            "flatShader",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.-1., 0.0, 1.0);\n" +
                    "}", "", "" +
                    "u4 color;\n" +
                    "void main(){\n" +
                    "   gl_FragColor = color;\n" +
                    "}"
        )

        flatShaderStriped = Shader(
            "flatShader",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.-1., 0.0, 1.0);\n" +
                    "}", "", "" +
                    "u4 color;\n" +
                    "uniform int offset, stride;\n" +
                    "void main(){\n" +
                    "   int x = int(gl_FragCoord.x);\n" +
                    "   if(x % stride != offset) discard;\n" +
                    "   gl_FragColor = color;\n" +
                    "}"
        )

        flatShaderGradient = createShader(
            "flatShaderGradient",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "u4 uvs;\n" +
                    "u4 lColor, rColor;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.-1., 0.0, 1.0);\n" +
                    "   color = attr0.x < 0.5 ? lColor : rColor;\n" +
                    "   uv = mix(uvs.xy, uvs.zw, attr0);\n" +
                    "}", "" + // mixing is done by varying
                    yuv2rgb +
                    "varying vec4 color;\n" +
                    "varying vec2 uv;\n", "" +
                    "uniform int code;\n" +
                    "uniform sampler2D tex0,tex1,tex2;\n" +
                    "void main(){\n" +
                    "   vec4 texColor;\n" +
                    "   if(uv.x >= 0 && uv.x <= 1){\n" +
                    "       switch(code){" +
                    "           case 0: texColor = texture(tex0, uv).gbar;break;\n" + // ARGB
                    "           case 1: texColor = texture(tex0, uv).bgra;break;\n" + // BGRA
                    "           case 2: \n" +
                    "               vec3 yuv = vec3(texture(tex0, uv).r, texture(tex1, uv).r, texture(tex2, uv).r);\n" +
                    "               texColor = vec4(yuv2rgb(yuv), 1.0);\n" +
                    "               break;\n" + // 420
                    "           default: texColor = texture(tex0, uv);\n" +
                    "       }" +
                    "   }\n" +
                    "   else texColor = vec4(1.0);\n" +
                    "   gl_FragColor = color * texColor;\n" +
                    "}", listOf("tex0","tex1","tex2")
        )

        flatShaderTexture = Shader(
            "flatShaderTexture",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "u4 tiling;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.-1., 0.0, 1.0);\n" +
                    "   uv = (attr0-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
                    "}", "" +
                    "varying vec2 uv;\n", "" +
                    "uniform sampler2D tex;\n" +
                    "u4 color;\n" +
                    "void main(){\n" +
                    "   gl_FragColor = color * texture(tex, uv);\n" +
                    "}"
        )

        copyShader = createShader(
            "copy", "in vec2 attr0;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4(attr0*2.0-1.0, 0.5, 1.0);\n" +
                    "   uv = attr0;\n" +
                    "}\n", "varying vec2 uv;\n", "" +
                    "uniform sampler2D tex;\n" +
                    "uniform float am1;\n" +
                    "void main(){\n" +
                    "   gl_FragColor = (1-am1) * texture(tex, uv);\n" +
                    "}", listOf("tex")
        )

        // with texture
        subpixelCorrectTextShader = Shader(
            "subpixelCorrectTextShader",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.-1., 0.0, 1.0);\n" +
                    "   uv = attr0;\n" +
                    "}", "" +
                    "varying v2 uv;\n", "" +
                    "uniform vec4 textColor;" +
                    "uniform vec4 backgroundColor;\n" +
                    "uniform sampler2D tex;\n" +
                    brightness +
                    "void main(){\n" +
                    "   vec3 textMask = texture(tex, uv).rgb;\n" +
                    "   vec3 mixing = brightness(textColor) > brightness(backgroundColor) ? textMask.rgb : textMask.rgb;\n" +
                    "   mixing *= textColor.a;\n" +
                    "   vec4 color = mix(backgroundColor, textColor, vec4(mixing, dot(mixing,vec3(${1f / 3f}))));\n" +
                    "   if(color.a < 0.001) discard;\n" +
                    "   gl_FragColor = vec4(color.rgb, 1.0);\n" +
                    "}"
        )

        subpixelCorrectTextShader.use()
        GL20.glUniform1i(subpixelCorrectTextShader["tex"], 0)

        val positionPostProcessing = "" +
                "   zDistance = gl_Position.w;\n"
        // this mapping only works with well tesselated geometry
        // or we need to add it to the fragment shader instead
        //"   const float far = 1000;\n" +
        //"   const float near = 0.001;\n" +
        //"   gl_Position.z = 2.0*log(gl_Position.w*near + 1)/log(far*near + 1) - 1;\n" +
        //"   gl_Position.z *= gl_Position.w;"

        val v3DBase = "" +
                "u4x4 transform;\n"

        val v3D = v3DBase +
                "a3 attr0;\n" +
                "a2 attr1;\n" +
                "u4 tiling;\n" +
                "void main(){\n" +
                "   localPosition = attr0;\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                positionPostProcessing +
                "   uv = (attr1-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
                "   uvw = attr0;\n" +
                "}"

        val y3D = "" +
                "varying v2 uv;\n" +
                "varying v3 uvw;\n" +
                "varying v3 localPosition;\n" +
                "varying float zDistance;\n"

        val f3D = "" +
                "u4 tint;" +
                "uniform sampler2D tex;\n" +
                getTextureLib +
                getColorForceFieldLib +
                "void main(){\n" +
                "   vec4 color = getTexture(tex, getProjectedUVs(uv, uvw));\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                "   gl_FragColor = tint * color;\n" +
                "}"

        shader3D = createShaderPlus("3d", v3D, y3D, f3D, listOf("tex"))
        shader3DforText = createShaderPlus(
            "3d-text", v3DBase +
                    "a3 attr0;\n" +
                    "a2 attr1;\n" +
                    "u3 offset;\n" +
                    "void main(){\n" +
                    "   localPosition = attr0 + offset;\n" +
                    "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                    positionPostProcessing +
                    "   vertexId = gl_VertexID;\n" +
                    "}",
            y3D + "" +
                    "flat varying int vertexId;\n", "" +
                    "u4 tint;" +
                    noiseFunc +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = vec4(1);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   gl_FragColor = tint * color;\n" +
                    "}", listOf()
        )
        shader3DforText.ignoreUniformWarnings(listOf("tiling", "forceFieldUVCount"))

        shader3DOutlinedText = createShaderPlus(
            "3d-text-withOutline", v3DBase +
                    "a3 attr0;\n" +
                    "a2 attr1;\n" +
                    "u2 offset, scale;\n" +
                    "void main(){\n" +
                    "   localPosition = vec3(attr0.xy * scale + offset, 0);\n" +
                    "   gl_Position = transform * vec4(attr0, 1.0);\n" +
                    positionPostProcessing +
                    "   uv = attr0.xy * 0.5 + 0.5;\n" +
                    "}",
            y3D, "" +
                    "u4 tint;" +
                    noiseFunc +
                    getTextureLib +
                    getColorForceFieldLib +
                    "uniform sampler2D tex;\n" +
                    "uniform vec4[$maxOutlineColors] colors;\n" +
                    "uniform vec2[$maxOutlineColors] distSmoothness;\n" +
                    "uniform int colorCount;\n" +
                    "void main(){\n" +
                    "   float distance = texture(tex, uv).r;\n" +
                    "   float gradient = length(vec2(dFdx(distance), dFdy(distance)));\n" +
                    "   vec4 color = tint;\n" +
                    "   for(int i=0;i<colorCount;i++){" +
                    "       vec4 colorHere = colors[i];\n" +
                    "       vec2 distSmooth = distSmoothness[i];\n" +
                    "       float offset = distSmooth.x;\n" +
                    "       float smoothness = distSmooth.y;\n" +
                    "       float appliedGradient = max(smoothness, gradient);\n" +
                    "       float mixingFactor0 = (distance-offset)*0.5/appliedGradient;\n" +
                    "       float mixingFactor = clamp(mixingFactor0,0,1);\n" +
                    "       color = mix(color, colorHere, mixingFactor);\n" +
                    "   }\n" +
                    "   gl_FragDepth = gl_FragCoord.z * (1 + distance * 0.000001);\n" +
                    "   if(color.a <= 0.001) discard;\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   gl_FragColor = color;\n" +
                    "}", listOf("tex")
        )
        shader3DOutlinedText.ignoreUniformWarnings(listOf("tiling", "filtering", "uvProjection", "forceFieldUVCount", "textureDeltaUV", "attr1"))

        val v3DPolygon = v3DBase +
                "a3 attr0;\n" +
                "in vec2 attr1;\n" +
                "uniform float inset;\n" +
                "void main(){\n" +
                "   vec2 betterUV = attr0.xy*2.-1.;\n" +
                "   betterUV *= mix(1.0, attr1.r, inset);\n" +
                "   localPosition = vec3(betterUV, attr0.z);\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                positionPostProcessing +
                "   uv = attr1.yx;\n" +
                "}"
        shader3DPolygon = createShaderPlus("3d-polygon", v3DPolygon, y3D, f3D, listOf("tex"))
        shader3DPolygon.ignoreUniformWarnings(listOf("tiling", "forceFieldUVCount"))

        val v3DMasked = v3DBase +
                "a2 attr0;\n" +
                "void main(){\n" +
                "   localPosition = vec3(attr0 * 2 - 1, 0.0);\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                "   uv = gl_Position.xyw;\n" +
                positionPostProcessing +
                "}"

        val y3DMasked = "" +
                "varying v3 uv;\n" +
                "varying v3 localPosition;\n" +
                "varying float zDistance;\n"

        val f3DMasked = "" +
                "precision highp float;\n" +
                "uniform vec4 tint;" +
                "uniform sampler2D maskTex, tex, tex2;\n" +
                "uniform float useMaskColor;\n" +
                "uniform float invertMask;\n" +
                "uniform vec2 pixelating;\n" +
                "uniform vec2 windowSize, offset;\n" +
                "uniform int maskType;\n" +
                "uniform float maxSteps;\n" +
                "uniform vec3 greenScreenSettings;\n" +
                brightness +
                getColorForceFieldLib +
                rgb2uv +
                "float max(vec3 rgb){return max(rgb.r, max(rgb.g, rgb.b));}\n" +
                "float min(vec3 rgb){return min(rgb.r, min(rgb.g, rgb.b));}\n" +
                "void main(){\n" +
                "   vec2 uv1 = uv.xy/uv.z;\n" +
                "   vec2 uv2 = uv1 * 0.5 + 0.5;\n" +
                "   vec4 mask = texture(maskTex, uv2);\n" +
                "   vec4 color;\n" +
                "   float effect, inverseEffect;\n" +
                "   switch(maskType){\n" +
                GLSLLib.case(MaskType.MASKING.id, "me/anno/objects/effects/types/Masking.glsl") +
                GLSLLib.case(MaskType.PIXELATING.id, "me/anno/objects/effects/types/Pixelating.glsl") +
                GLSLLib.case(MaskType.RADIAL_BLUR_1.id, "me/anno/objects/effects/types/RadialBlur1.glsl") +
                GLSLLib.case(MaskType.RADIAL_BLUR_2.id, "me/anno/objects/effects/types/RadialBlur2.glsl") +
                GLSLLib.case(MaskType.GREEN_SCREEN.id, "me/anno/objects/effects/types/GreenScreen.glsl") +
                "       case ${MaskType.GAUSSIAN_BLUR.id}:\n" +
                "       case ${MaskType.BOKEH_BLUR.id}:\n" +
                "       case ${MaskType.BLOOM.id}:\n" + // just mix two images
                "           effect = mix(mask.a, dot(vec3(0.3), mask.rgb), useMaskColor);\n" +
                "           effect = mix(effect, 1.0 - effect, invertMask);\n" +
                "           color = mix(texture(tex, uv2), texture(tex2, uv2), effect);\n" +
                "           break;\n" +
                "       case ${MaskType.UV_OFFSET.id}:\n" +
                "           vec2 offset = (mask.rg-mask.gb) * pixelating;\n" +
                "           color = texture(tex, uv2 + offset);\n" +
                "           break;\n" +
                "   }\n" +
                "   if(color.a <= 0.001) discard;\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                "   gl_FragColor = tint * color;\n" +
                "   gl_FragColor.a = min(gl_FragColor.a, 1.0);\n" +
                "}"
        shader3DMasked = createShaderPlus("3d-masked", v3DMasked, y3DMasked, f3DMasked, listOf("maskTex", "tex", "tex2"))
        shader3DMasked.ignoreUniformWarnings(listOf("tiling"))

        val f3DGaussianBlur = "" +
                "uniform sampler2D tex;\n" +
                "uniform vec2 stepSize;\n" +
                "uniform float steps;\n" +
                "uniform float threshold;\n" +
                brightness +
                "void main(){\n" +
                "   vec2 uv2 = uv.xy/uv.z * 0.5 + 0.5;\n" +
                "   vec4 color;\n" +
                "   float sum = 0.0;\n" +
                // test all steps for -pixelating*2 .. pixelating*2, then average
                "   int iSteps = max(0, int(2.7 * steps));\n" +
                "   if(iSteps == 0){\n" +
                "       color = texture(tex, uv2);\n" +
                "   } else {\n" +
                "       color = vec4(0.0);\n" +
                "       for(int i=-iSteps;i<=iSteps;i++){\n" +
                "           float fi = float(i);\n" +
                "           float relativeX = fi/steps;\n" +
                "           vec4 colorHere = texture(tex, uv2 + fi * stepSize);\n" +
                "           float weight = i == 0 ? 1.0 : exp(-relativeX*relativeX);\n" +
                "           sum += weight;\n" +
                "           color += vec4(max(vec3(0), colorHere.rgb - threshold), colorHere.a) * weight;\n" +
                "       }\n" +
                "       color /= sum;\n" +
                "   }\n" +
                "   gl_FragColor = color;\n" +
                "}"
        shader3DGaussianBlur = createShader("3d-blur", v3DMasked, y3DMasked, f3DGaussianBlur, listOf("tex"))

        // somehow becomes dark for large |steps|-values
        shader3DBoxBlur = createShader(
            "3d-blur", "" +
                    "a2 attr0;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4(2*attr0-1, 0.0, 1.0);\n" +
                    "   uv = attr0;\n" +
                    "}", "varying vec2 uv;\n",  "" +
                    "precision highp float;\n" +
                    "uniform sampler2D tex;\n" +
                    "uniform vec2 stepSize;\n" +
                    "uniform int steps;\n" +
                    "void main(){\n" +
                    "   vec4 color;\n" +
                    "   if(steps < 2){\n" +
                    "       color = texture(tex, uv);\n" +
                    "   } else {\n" +
                    "       color = vec4(0.0);\n" +
                    "       for(int i=-steps/2;i<(steps+1)/2;i++){\n" +
                    "           color += texture(tex, uv + float(i) * stepSize);\n" +
                    "       }\n" +
                    "       color /= float(steps);\n" +
                    "   }\n" +
                    "   gl_FragColor = color;\n" +
                    "}", listOf("tex")
        )

        val v3DSVG = v3DBase +
                "a3 attr0;\n" +
                "a4 attr1;\n" +
                "void main(){\n" +
                "   localPosition = attr0;\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                positionPostProcessing +
                "   uv = attr0.xy;\n" +
                "   color0 = attr1;\n" +
                "}"

        val y3DSVG = y3D +
                "varying v4 color0;\n"

        val f3DSVG = "" +
                "uniform vec4 tint;" +
                "uniform sampler2D tex;\n" +
                "uniform vec4 uvLimits;\n" +
                getTextureLib +
                getColorForceFieldLib +
                brightness +
                ascColorDecisionList +
                "bool isInLimits(float value, vec2 minMax){\n" +
                "   return value >= minMax.x && value <= minMax.y;\n" +
                "}\n" +
                "void main(){\n" +
                "   vec4 color = color0;\n" +
                "   color.rgb = colorGrading(color.rgb);\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                "   gl_FragColor = isInLimits(uv.x, uvLimits.xz) && isInLimits(uv.y, uvLimits.yw) ?\n" +
                "       tint * color * getTexture(tex, uv * 0.5 + 0.5) : vec4(0.0);\n" +
                "}"

        shader3DSVG = createShaderPlus("3d-svg", v3DSVG, y3DSVG, f3DSVG, listOf("tex"))

        val v3DCircle = v3DBase +
                "a2 attr0;\n" + // angle, inner/outer
                "u3 circleParams;\n" + // 1 - inner r, start, end
                "void main(){\n" +
                "   float angle = mix(circleParams.y, circleParams.z, attr0.x);\n" +
                "   vec2 betterUV = vec2(cos(angle), -sin(angle)) * (1.0 - circleParams.x * attr0.y);\n" +
                "   localPosition = vec3(betterUV, 0.0);\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                positionPostProcessing +
                "}"

        val f3DCircle = "" +
                "u4 tint;\n" + // rgba
                getColorForceFieldLib +
                "void main(){\n" +
                "   vec4 color = vec4(1.0);\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                "   gl_FragColor = tint * color;\n" +
                "}"

        shader3DCircle = createShaderPlus("3dCircle", v3DCircle, y3D, f3DCircle, listOf())
        shader3DCircle.ignoreUniformWarnings(
            listOf(
                "filtering",
                "textureDeltaUV",
                "tiling",
                "uvProjection",
                "forceFieldUVCount"
            )
        )

        // create the obj+mtl shader
        shaderObjMtl = createShaderPlus(
            "obj/mtl",
            v3DBase +
                    "a3 coords;\n" +
                    "a2 uvs;\n" +
                    "a3 normals;\n" +
                    "void main(){\n" +
                    "   localPosition = coords;\n" +
                    "   gl_Position = transform * vec4(coords, 1.0);\n" +
                    "   uv = uvs;\n" +
                    "   normal = normals;\n" +
                    positionPostProcessing +
                    "}", y3D + "" +
                    "varying vec3 normal;\n", "" +
                    "uniform vec4 tint;" +
                    "uniform sampler2D tex;\n" +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = getTexture(tex, uv);\n" +
                    "   color.rgb *= 0.5 + 0.5 * dot(vec3(1.0, 0.0, 0.0), normal);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   gl_FragColor = tint * color;\n" +
                    "}", listOf("tex")
        )

        // create the fbx shader
        shaderFBX = FBXShader.getShader(v3DBase, positionPostProcessing, y3D, getTextureLib)

        shader3DYUV = createShaderPlus(
            "3d-yuv",
            v3D, y3D, "" +
                    "uniform vec4 tint;\n" +
                    "uniform sampler2D texY, texU, texV;\n" +
                    "uniform vec2 uvCorrection;\n" +
                    getTextureLib +
                    getColorForceFieldLib +
                    brightness +
                    ascColorDecisionList +
                    yuv2rgb +
                    "void main(){\n" +
                    "   vec2 uv2 = getProjectedUVs(uv, uvw);\n" +
                    "   vec2 correctedUV = uv2*uvCorrection;\n" +
                    "   vec2 correctedDUV = textureDeltaUV*uvCorrection;\n" +
                    "   vec3 yuv = vec3(" +
                    "       getTexture(texY, uv2).r, " +
                    "       getTexture(texU, correctedUV, correctedDUV).r, " +
                    "       getTexture(texV, correctedUV, correctedDUV).r);\n" +
                    "   vec4 color = vec4(yuv2rgb(yuv), 1.0);\n" +
                    "   color.rgb = colorGrading(color.rgb);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   gl_FragColor = tint * color;\n" +
                    "}", listOf("texY", "texU", "texV")
        )

        fun createSwizzleShader(swizzle: String): Shader {
            return createShaderPlus(
                "3d-${if (swizzle.isBlank()) "rgba" else swizzle}",
                v3D, y3D, "" +
                        "uniform vec4 tint;\n" +
                        "uniform sampler2D tex;\n" +
                        getTextureLib +
                        getColorForceFieldLib +
                        brightness +
                        ascColorDecisionList +
                        "void main(){\n" +
                        "   vec4 color = getTexture(tex, getProjectedUVs(uv, uvw))$swizzle;\n" +
                        "   color.rgb = colorGrading(color.rgb);\n" +
                        "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                        "   gl_FragColor = tint * color;\n" +
                        "}", listOf("tex")
            )
        }

        shader3DRGBA = createSwizzleShader("")
        shader3DARGB = createSwizzleShader(".gbar")
        shader3DBGRA = createSwizzleShader(".bgra")

        lineShader3D = Shader(
            "3d-lines",
            "in vec3 attr0;\n" +
                    "uniform mat4 transform;\n" +
                    "void main(){" +
                    "   gl_Position = transform * vec4(attr0, 1.0);\n" +
                    positionPostProcessing +
                    "}", "" +
                    "varying float zDistance;\n", "" +
                    "uniform vec4 color;\n" +
                    "void main(){" +
                    "   gl_FragColor = color;\n" +
                    "}"

        )

    }

    fun createShaderNoShorts(
        shaderName: String,
        v3D: String,
        y3D: String,
        f3D: String,
        textures: List<String>
    ): Shader {
        val shader = Shader(shaderName, v3D, y3D, f3D, true)
        shader.use()
        textures.forEachIndexed { index, name ->
            GL20.glUniform1i(shader[name], index)
        }
        return shader
    }

    fun createShaderPlus(
        shaderName: String,
        v3D: String,
        y3D: String,
        f3D: String,
        textures: List<String>
    ): Shader {
        val shader = ShaderPlus.create(shaderName, v3D, y3D, f3D)
        shader.use()
        textures.forEachIndexed { index, name ->
            GL20.glUniform1i(shader[name], index)
        }
        return shader
    }

    fun createShader(shaderName: String, v3D: String, y3D: String, f3D: String, textures: List<String>): Shader {
        val shader = Shader(shaderName, v3D, y3D, f3D)
        shader.use()
        textures.forEachIndexed { index, name ->
            GL20.glUniform1i(shader[name], index)
        }
        return shader
    }


}