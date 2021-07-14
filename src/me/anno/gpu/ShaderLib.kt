package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.texture.Filtering
import me.anno.mesh.assimp.AnimGameItem
import me.anno.mesh.fbx.model.FBXShader
import me.anno.objects.effects.MaskType
import me.anno.objects.effects.types.GLSLLib
import me.anno.objects.modes.UVProjection
import me.anno.studio.rems.Scene.noiseFunc
import me.anno.utils.Clock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI

object ShaderLib {

    lateinit var flatShader: BaseShader
    lateinit var flatShaderStriped: BaseShader
    lateinit var flatShaderGradient: BaseShader
    lateinit var flatShaderTexture: BaseShader
    lateinit var subpixelCorrectTextShader: BaseShader
    lateinit var shader3DPolygon: BaseShader
    lateinit var shader3D: BaseShader
    lateinit var shader3DforText: BaseShader
    lateinit var shaderSDFText: BaseShader
    lateinit var shader3DRGBA: BaseShader
    lateinit var shader3DYUV: BaseShader
    lateinit var shader3DARGB: BaseShader
    lateinit var shader3DBGRA: BaseShader
    lateinit var shader3DCircle: BaseShader
    lateinit var shader3DSVG: BaseShader
    lateinit var lineShader3D: BaseShader
    lateinit var shader3DMasked: BaseShader
    lateinit var shader3DGaussianBlur: BaseShader
    lateinit var shader3DBoxBlur: BaseShader
    lateinit var shaderObjMtl: BaseShader
    lateinit var shaderAssimp: BaseShader
    lateinit var shaderFBX: BaseShader
    lateinit var copyShader: BaseShader

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


    // from http://www.java-gaming.org/index.php?topic=35123.0
    // https://stackoverflow.com/questions/13501081/efficient-bicubic-filtering-code-in-glsl
    private const val foreignBicubicInterpolation = "" +
            "vec4 cubic(float v){\n" +
            "    vec4 n = vec4(1.0, 2.0, 3.0, 4.0) - v;\n" +
            "    vec4 s = n * n * n;\n" +
            "    float x = s.x;\n" +
            "    float y = s.y - 4.0 * s.x;\n" +
            "    float z = s.z - 4.0 * s.y + 6.0 * s.x;\n" +
            "    float w = 6.0 - x - y - z;\n" +
            "    return vec4(x, y, z, w) * (1.0/6.0);\n" +
            "}\n" +
            "vec4 textureBicubic(sampler2D sampler, vec2 texCoords){\n" +

            "   vec2 texSize = textureSize(sampler, 0);\n" +
            "   vec2 invTexSize = 1.0 / texSize;\n" +

            "   texCoords = texCoords * texSize - 0.5;\n" +

            "    vec2 fxy = fract(texCoords);\n" +
            "    texCoords -= fxy;\n" +

            "    vec4 xCubic = cubic(fxy.x);\n" +
            "    vec4 yCubic = cubic(fxy.y);\n" +

            "    vec4 c = texCoords.xxyy + vec2(-0.5, +1.5).xyxy;\n" +

            "    vec4 s = vec4(xCubic.xz + xCubic.yw, yCubic.xz + yCubic.yw);\n" +
            "    vec4 offset = c + vec4(xCubic.yw, yCubic.yw) / s;\n" +

            "    offset *= invTexSize.xxyy;\n" +

            "    vec4 sample0 = texture(sampler, offset.xz);\n" +
            "    vec4 sample1 = texture(sampler, offset.yz);\n" +
            "    vec4 sample2 = texture(sampler, offset.xw);\n" +
            "    vec4 sample3 = texture(sampler, offset.yw);\n" +
            "    float sx = s.x / (s.x + s.y);\n" +
            "    float sy = s.z / (s.z + s.w);\n" +
            "    return mix(mix(sample3, sample2, sx), mix(sample1, sample0, sx), sy);\n" +
            "}"

    const val bicubicInterpolation = "" +
            // keine Artifakte mehr, aber deutlich weicher...
            foreignBicubicInterpolation +
            "vec4 bicubicInterpolation(sampler2D tex, vec2 uv, vec2 duv){\n" +
            "   return textureBicubic(tex, uv);\n" +
            "}\n"

    // https://en.wikipedia.org/wiki/ASC_CDL
    // color grading with asc cdl standard
    const val ascColorDecisionList = "" +
            "uniform vec3 cgSlope, cgOffset, cgPower;\n" +
            "uniform float cgSaturation;\n" +
            "vec3 colorGrading(vec3 raw){\n" +
            "   vec3 color = pow(max(vec3(0.0), raw * cgSlope + cgOffset), cgPower);\n" +
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

    const val yuv2rgb = "" +
            "vec3 yuv2rgb(vec3 yuv){" +
            "   yuv -= vec3(${16f / 255f}, 0.5, 0.5);\n" +
            "   return vec3(" +
            "       dot(yuv, vec3( 1.164,  0.000,  1.596))," +
            "       dot(yuv, vec3( 1.164, -0.392, -0.813))," +
            "       dot(yuv, vec3( 1.164,  2.017,  0.000)));\n" +
            "}\n"

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

    val flatNormal = "" +
            "   normal = vec3(0,0,1);\n"

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
            flatNormal +
            "}"

    val y3D = "" +
            "varying vec2 uv;\n" +
            "varying vec3 uvw;\n" +
            "varying vec3 localPosition;\n" +
            "varying float zDistance;\n" +
            "varying vec3 normal;\n"

    val f3D = "" +
            "uniform sampler2D tex;\n" +
            getTextureLib +
            getColorForceFieldLib +
            "void main(){\n" +
            "   vec4 color = getTexture(tex, getProjectedUVs(uv, uvw));\n" +
            "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
            "   vec3 finalColor = color.rgb;\n" +
            "   float finalAlpha = color.a;\n" +
            "}"


    fun init() {

        val tick = Clock()

        // make this customizable?

        // color only for a rectangle
        // (can work on more complex shapes)
        flatShader = BaseShader(
            "flatShader",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                    "}", "", "" +
                    "u4 color;\n" +
                    "void main(){\n" +
                    "   gl_FragColor = color;\n" +
                    "}"
        )

        flatShaderStriped = BaseShader(
            "flatShader",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
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
                    yuv2rgb +
                    "u4 lColor, rColor;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                    "   color = attr0.x < 0.5 ? lColor : rColor;\n" +
                    "   uv = mix(uvs.xy, uvs.zw, attr0);\n" +
                    "}", "" +
                    "varying vec4 color;\n" +
                    "varying vec2 uv;\n", "" +
                    "uniform int code;\n" +
                    "uniform sampler2D tex0,tex1,tex2;\n" +
                    yuv2rgb +
                    "void main(){\n" +
                    "   vec4 texColor;\n" +
                    "   if(uv.x >= 0.0 && uv.x <= 1.0){\n" +
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
                    "}", listOf("tex0", "tex1", "tex2")
        )

        flatShaderTexture = BaseShader(
            "flatShaderTexture",
            "" +
                    "a2 attr0;\n" +
                    "u2 pos, size;\n" +
                    "u4 tiling;\n" +
                    "void main(){\n" +
                    "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
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
                    "   gl_FragColor = (1.0-am1) * texture(tex, uv);\n" +
                    "}", listOf("tex")
        )

        // with texture
        subpixelCorrectTextShader = BaseShader(
            "subpixelCorrectTextShader",
            "" +
                    "a2 attr0;\n" +
                    "uniform vec2 pos, size;\n" +
                    "uniform vec2 windowSize;\n" +
                    "void main(){\n" +
                    "   vec2 localPos = pos + attr0 * size;\n" +
                    "   gl_Position = vec4(localPos*2.0-1.0, 0.0, 1.0);\n" +
                    "   position = localPos * windowSize;\n" +
                    "   uv = attr0;\n" +
                    "}", "" +
                    "varying v2 uv, position;\n", "" +
                    "uniform vec4 textColor, backgroundColor;\n" +
                    "uniform vec2 windowSize;\n" +
                    "uniform sampler2D tex;\n" +
                    brightness +
                    "void main(){\n" +
                    "   vec3 textMask = texture(tex, uv).rgb;\n" +
                    "   vec3 mixing = brightness(textColor) > brightness(backgroundColor) ? textMask.rgb : textMask.rgb;\n" +
                    "   mixing *= textColor.a;\n" +
                    "   float mixingAlpha = brightness(mixing);\n" +
                    // todo we only need to check the axis, which is affected by subpixel-rendering, e.g. x on my screen
                    "   if(position.x < 1.0 || position.y < 1.0 || position.x > windowSize.x - 1.0 || position.y > windowSize.y - 1.0)\n" +
                    "       mixing = vec3(mixingAlpha);\n" + // on the border; color seams would become apparent here
                    "   vec4 color = mix(backgroundColor, textColor, vec4(mixing, mixingAlpha));\n" +
                    "   if(color.a < 0.001) discard;\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = 1.0;\n" +
                    "}"
        )
        subpixelCorrectTextShader.setTextureIndices(listOf("tex"))

        shader3D = createShaderPlus("3d", v3D, y3D, f3D, listOf("tex"))
        shader3DforText = createShaderPlus(
            "3d-text", v3DBase +
                    "a3 attr0;\n" +
                    "a2 attr1;\n" +
                    "u3 offset;\n" +
                    getUVForceFieldLib +
                    "void main(){\n" +
                    "   vec3 localPos0 = attr0 + offset;\n" +
                    "   vec2 pseudoUV2 = getForceFieldUVs(localPos0.xy*.5+.5);\n" +
                    "   localPosition = $hasForceFieldUVs ? vec3(pseudoUV2*2.0-1.0, attr0.z + offset.z) : localPos0;\n" +
                    "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                    flatNormal +
                    positionPostProcessing +
                    "   vertexId = gl_VertexID;\n" +
                    "}", y3D + "" +
                    "flat varying int vertexId;\n", "" +
                    noiseFunc +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 finalColor2 = ($hasForceFieldColor) ? getForceFieldColor() : vec4(1.0);\n" +
                    "   vec3 finalColor = finalColor2.rgb;\n" +
                    "   float finalAlpha = finalColor2.a;\n" +
                    "}", listOf()
        )
        shader3DforText.ignoreUniformWarnings(listOf("tiling", "forceFieldUVCount"))

        shaderSDFText = createShaderPlus(
            "3d-text-withOutline", v3DBase +
                    "a3 attr0;\n" +
                    "a2 attr1;\n" +
                    "u2 offset, scale;\n" +
                    getUVForceFieldLib +
                    "void main(){\n" +
                    "   uv = attr0.xy * 0.5 + 0.5;\n" +
                    "   vec2 localPos0 = attr0.xy * scale + offset;\n" +
                    "   vec2 pseudoUV2 = getForceFieldUVs(localPos0*.5+.5);\n" +
                    "   localPosition = vec3($hasForceFieldUVs ? pseudoUV2*2.0-1.0 : localPos0, 0);\n" +
                    "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                    positionPostProcessing +
                    "}", y3D, "" +
                    noiseFunc +
                    getTextureLib +
                    getColorForceFieldLib +
                    "uniform vec4 tint;\n" +
                    "uniform sampler2D tex;\n" +
                    "uniform vec4[$maxOutlineColors] colors;\n" +
                    "uniform vec2[$maxOutlineColors] distSmoothness;\n" +
                    "uniform float depth;\n" +
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
                    "       float mixingFactor = clamp(mixingFactor0, 0.0, 1.0);\n" +
                    "       color = mix(color, colorHere, mixingFactor);\n" +
                    "   }\n" +
                    "   if(depth != 0.0) gl_FragDepth = gl_FragCoord.z * (1.0 + distance * depth);\n" +
                    "   if(color.a <= 0.001) discard;\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = color.a;\n" +
                    "}", listOf("tex")
        )
        shaderSDFText.ignoreUniformWarnings(
            listOf(
                "tiling",
                "filtering",
                "uvProjection",
                "forceFieldUVCount",
                "textureDeltaUV",
                "attr1"
            )
        )

        val v3DPolygon = v3DBase +
                "a3 attr0;\n" +
                "in vec2 attr1;\n" +
                "uniform float inset;\n" +
                "void main(){\n" +
                "   vec2 betterUV = attr0.xy;\n" +
                "   betterUV *= mix(1.0, attr1.r, inset);\n" +
                "   localPosition = vec3(betterUV, attr0.z);\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                flatNormal +
                positionPostProcessing +
                "   uv = attr1.yx;\n" +
                "}"
        shader3DPolygon = createShaderPlus("3d-polygon", v3DPolygon, y3D, f3D, listOf("tex"))
        shader3DPolygon.ignoreUniformWarnings(listOf("tiling", "forceFieldUVCount"))

        val v3DMasked = v3DBase +
                "a2 attr0;\n" +
                "void main(){\n" +
                "   localPosition = vec3(attr0*2.0-1.0, 0.0);\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                "   uv = gl_Position.xyw;\n" +
                flatNormal +
                positionPostProcessing +
                "}"

        val y3DMasked = "" +
                "varying vec3 uv;\n" +
                "varying vec3 localPosition;\n" +
                "varying float zDistance;\n"

        val f3DMasked = "" +
                "precision highp float;\n" +
                "uniform sampler2D maskTex, tex, tex2;\n" +
                "uniform float useMaskColor;\n" +
                "uniform float invertMask;\n" +
                "uniform vec2 pixelating;\n" +
                "uniform vec2 windowSize, offset;\n" +
                "uniform int maskType;\n" +
                "uniform float maxSteps;\n" +
                "uniform vec4 settings;\n" +
                brightness +
                getColorForceFieldLib +
                rgb2uv +
                "float maxV3(vec3 rgb){return max(rgb.r, max(rgb.g, rgb.b));}\n" +
                "float minV3(vec3 rgb){return min(rgb.r, min(rgb.g, rgb.b));}\n" +
                "void main(){\n" +
                "   vec2 uv1 = uv.xy/uv.z;\n" +
                "   vec2 uv2 = uv1 * 0.5 + 0.5;\n" +
                "   vec4 mask = texture(maskTex, uv2);\n" +
                "   vec4 color;\n" +
                "   float effect, inverseEffect;\n" +
                "   switch(maskType){\n" +
                GLSLLib.case(MaskType.MASKING.id, "me/anno/objects/effects/types/Masking.glsl") +
                GLSLLib.case(MaskType.TRANSITION.id, "me/anno/objects/effects/types/Transition.glsl") +
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
                "   vec3 finalColor = color.rgb;\n" +
                "   float finalAlpha = min(color.a, 1.0);\n" +
                "}"
        shader3DMasked =
            createShaderPlus("3d-masked", v3DMasked, y3DMasked, f3DMasked, listOf("maskTex", "tex", "tex2"))
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
                "           color += vec4(max(vec3(0.0), colorHere.rgb - threshold), colorHere.a) * weight;\n" +
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
                    "   gl_Position = vec4(attr0*2.0-1.0, 0.0, 1.0);\n" +
                    "   uv = attr0;\n" +
                    "}", "varying vec2 uv;\n", "" +
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

        val vSVG = v3DBase +
                "a3 aLocalPosition;\n" +
                "a2 aLocalPos2;\n" +
                "a4 aFormula0;\n" +
                "a1 aFormula1;\n" +
                "a4 aColor0, aColor1, aColor2, aColor3;\n" +
                "a4 aStops;\n" +
                "a1 aPadding;\n" +
                "void main(){\n" +
                "   localPosition = aLocalPosition;\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                flatNormal +
                positionPostProcessing +
                "   color0 = aColor0;\n" +
                "   color1 = aColor1;\n" +
                "   color2 = aColor2;\n" +
                "   color3 = aColor3;\n" +
                "   stops = aStops;\n" +
                "   padding = aPadding;\n" +
                "   localPos2 = aLocalPos2;\n" +
                "   formula0 = aFormula0;\n" +
                "   formula1 = aFormula1;\n" +
                "}"

        val ySVG = y3D +
                "varying v4 color0, color1, color2, color3, stops;\n" +
                "varying v4 formula0;\n" +
                "varying v1 formula1;\n" +
                "varying v1 padding;\n" +
                "varying v2 localPos2;\n"

        val fSVG = "" +
                "uniform sampler2D tex;\n" +
                "uniform vec4 uvLimits;\n" +
                getTextureLib +
                getColorForceFieldLib +
                brightness +
                ascColorDecisionList +
                "bool isInLimits(float value, vec2 minMax){\n" +
                "   return value >= minMax.x && value <= minMax.y;\n" +
                "}\n" + // sqrt and ² for better color mixing
                "vec4 mix(vec4 a, vec4 b, float stop, vec2 stops){\n" +
                "   float f = clamp((stop-stops.x)/(stops.y-stops.x), 0, 1);\n" +
                "   return vec4(sqrt(mix(a.rgb*a.rgb, b.rgb*b.rgb, f)), mix(a.a, b.a, f));\n" +
                "}\n" +
                "void main(){\n" +
                // apply the formula; polynomial of 2nd degree
                "   vec2 delta = localPos2 - formula0.xy;\n" +
                "   vec2 dir = formula0.zw;\n" +
                "   vec2 deltaXDir = delta * dir;\n" +
                "   float stopValue = formula1 > 0.5 ? length(deltaXDir) : dot(dir, delta);\n" +
                "   if(stopValue < 0.0 || stopValue > 1.0){" +
                "       if(padding < 0.5){\n" + // clamp
                "           stopValue = clamp(stopValue, 0.0, 1.0);\n" +
                "       } else if(padding < 1.5){\n" + // repeat mirrored, and yes, it looks like magic xD
                "           stopValue = 1.0 - abs(fract(stopValue*0.5)*2.0-1.0);\n" +
                "       } else {\n" + // repeat
                "           stopValue = fract(stopValue);\n" +
                "       }\n" +
                "   }\n" +
                // find the correct color
                "   vec4 color = \n" +
                "       stopValue <= stops.x ? color0:\n" +
                "       stopValue >= stops.w ? color3:\n" +
                "       stopValue <  stops.y ? mix(color0, color1, stopValue, stops.xy):\n" +
                "       stopValue <  stops.z ? mix(color1, color2, stopValue, stops.yz):\n" +
                "                              mix(color2, color3, stopValue, stops.zw);\n" +
                // "   color.rgb = fract(vec3(stopValue));\n" +
                "   color.rgb = colorGrading(color.rgb);\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                "   vec3 finalColor;\n" +
                "   float finalAlpha;\n" +
                "   if(isInLimits(uv.x, uvLimits.xz) && isInLimits(uv.y, uvLimits.yw)){" +
                "       vec4 color2 = color * getTexture(tex, uv * 0.5 + 0.5);\n" +
                "       finalColor = color2.rgb;\n" +
                "       finalAlpha = color2.a;\n" +
                "   } else {" +
                "       finalColor = vec3(0);\n" +
                "       finalAlpha = 0.0;\n" +
                "   }" +
                "}"

        shader3DSVG = createShaderPlus("3d-svg", vSVG, ySVG, fSVG, listOf("tex"))

        val v3DCircle = v3DBase +
                "a2 attr0;\n" + // angle, inner/outer
                "u3 circleParams;\n" + // 1 - inner r, start, end
                "void main(){\n" +
                "   float angle = mix(circleParams.y, circleParams.z, attr0.x);\n" +
                "   vec2 betterUV = vec2(cos(angle), -sin(angle)) * (1.0 - circleParams.x * attr0.y);\n" +
                "   localPosition = vec3(betterUV, 0.0);\n" +
                "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                flatNormal +
                positionPostProcessing +
                "}"

        val f3DCircle = "" +
                getColorForceFieldLib +
                "void main(){\n" +
                "   gl_FragColor = ($hasForceFieldColor) ? getForceFieldColor() : vec4(1);\n" +
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
                    "uniform sampler2D tex;\n" +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = getTexture(tex, uv);\n" +
                    "   color.rgb *= 0.5 + 0.5 * dot(vec3(-1.0, 0.0, 0.0), normal);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = color.a;\n" +
                    "}", listOf("tex")
        )

        val maxBones = AnimGameItem.maxBones
        shaderAssimp = createShaderPlus(
            "assimp",
            v3DBase +
                    "a3 coords;\n" +
                    "a2 uvs;\n" +
                    "a3 normals;\n" +
                    "a4 colors;\n" +
                    "a4 weights;\n" +
                    "ai4 indices;\n" +
                    "uniform float hasAnimation;\n" +
                    "uniform mat4x3 localTransform;\n" +
                    "uniform mat4x3 jointTransforms[$maxBones];\n" +
                    "void main(){\n" +
                    "   localPosition = coords;\n" +
                    "   normal = normals;\n" +
                    "   if(hasAnimation > 0.5){\n" +
                    "       mat4x3 jointMat;\n" +
                    "       jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                    "       jointMat += jointTransforms[indices.y] * weights.y;\n" +
                    "       jointMat += jointTransforms[indices.z] * weights.z;\n" +
                    "       jointMat += jointTransforms[indices.w] * weights.w;\n" +
                    "       localPosition = jointMat * vec4(localPosition, 1.0);\n" +
                    "       normal = jointMat * vec4(normal, 0.0);\n" +
                    "   }" +
                    "   normal = localTransform * vec4(normal, 0.0);\n" +
                    "   normal = normalize(normal);\n" +
                    "   localPosition = localTransform * vec4(localPosition, 1.0);\n" +
                    "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                    "   uv = uvs;\n" +
                    "   weight = weights;\n" +
                    "   vertexColor = colors;\n" +
                    positionPostProcessing +
                    "}", y3D + "" +
                    // "varying vec3 normal;\n" + // by default, in y3D, included
                    "varying vec4 weight;\n" +
                    "varying vec4 vertexColor;\n", "" +
                    "uniform sampler2D tex;\n" +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = vec4(vertexColor.rgb,1) * getTexture(tex, uv);\n" +
                    "   color.rgb *= 0.5 + 0.5 * dot(vec3(-1.0, 0.0, 0.0), normal);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = color.a;\n" +
                    "}", listOf("tex")
        )
        shaderAssimp.glslVersion = 330

        // create the fbx shader
        shaderFBX = FBXShader.getShader(v3DBase, positionPostProcessing, y3D, getTextureLib)

        shader3DYUV = createShaderPlus(
            "3d-yuv",
            v3D, y3D, "" +
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
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = color.a;\n" +
                    "}", listOf("texY", "texU", "texV")
        )

        fun createSwizzleShader(swizzle: String): BaseShader {
            return createShaderPlus(
                "3d-${if (swizzle.isEmpty()) "rgba" else swizzle}",
                v3D, y3D, "" +
                        "uniform sampler2D tex;\n" +
                        getTextureLib +
                        getColorForceFieldLib +
                        brightness +
                        ascColorDecisionList +
                        "void main(){\n" +
                        "   vec4 color = getTexture(tex, getProjectedUVs(uv, uvw))$swizzle;\n" +
                        "   color.rgb = colorGrading(color.rgb);\n" +
                        "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                        "   vec3 finalColor = color.rgb;\n" +
                        "   float finalAlpha = color.a;\n" +
                        "}", listOf("tex")
            )
        }

        shader3DRGBA = createSwizzleShader(".rgba")
        shader3DARGB = createSwizzleShader(".gbar")
        shader3DBGRA = createSwizzleShader(".bgra")

        lineShader3D = BaseShader(
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

        tick.stop("creating default shaders")

    }

    // once was used to skip over abbreviation symbols;
    // however, the overhead shouldn't be a problem -> ignored
    fun createShaderNoShorts(
        shaderName: String,
        v3D: String,
        y3D: String,
        f3D: String,
        textures: List<String>
    ): BaseShader {
        val shader = BaseShader(shaderName, v3D, y3D, f3D)
        shader.setTextureIndices(textures)
        return shader
    }

    fun createShaderPlus(
        shaderName: String,
        v3D: String,
        y3D: String,
        f3D: String,
        textures: List<String>
    ): BaseShader {
        val shader = BaseShader(shaderName, v3D, y3D, f3D)
        shader.setTextureIndices(textures)
        return shader
    }

    fun createShader(shaderName: String, v3D: String, y3D: String, f3D: String, textures: List<String>): BaseShader {
        val shader = BaseShader(shaderName, v3D, y3D, f3D)
        shader.setTextureIndices(textures)
        return shader
    }


}