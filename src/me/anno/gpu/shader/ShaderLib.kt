package me.anno.gpu.shader

import me.anno.config.DefaultConfig
import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.engine.ui.render.ECSMeshShader.Companion.getAnimMatrix
import me.anno.gpu.drawing.UVProjection
import me.anno.gpu.shader.OpenGLShader.Companion.attribute
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Filtering
import me.anno.mesh.assimp.AnimGameItem
import me.anno.utils.Clock
import me.anno.utils.pooling.ByteBufferPool
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sqrt

@Suppress("MemberVisibilityCanBePrivate", "unused")
object ShaderLib {

    lateinit var subpixelCorrectTextShader: BaseShader
    lateinit var shader3DPolygon: BaseShader
    lateinit var shader3D: BaseShader
    lateinit var shader3DText: BaseShader
    lateinit var shaderSDFText: BaseShader
    lateinit var shader3DRGBA: BaseShader
    lateinit var shader3DYUV: BaseShader
    lateinit var shader3DARGB: BaseShader
    lateinit var shader3DBGRA: BaseShader
    lateinit var shader3DCircle: BaseShader
    lateinit var shader3DSVG: BaseShader
    lateinit var lineShader3D: BaseShader
    lateinit var shader3DBoxBlur: BaseShader
    lateinit var shaderObjMtl: BaseShader
    lateinit var shaderAssimp: BaseShader
    lateinit var shader3DGaussianBlur: BaseShader
    lateinit var monochromeModelShader: BaseShader

    /**
     * our code only uses 3, I think
     * */
    const val maxOutlineColors = 6

    val coordsList = listOf(Variable(GLSLType.V2F, "coords", VariableMode.ATTR))
    const val coordsVShader = "" +
            "void main(){\n" +
            "   gl_Position = vec4(coords*2.0-1.0,0.5,1.0);\n" +
            "   uv = coords;\n" +
            "}"

    val simplestVertexShader = "" +
            "$attribute vec2 coords;\n" +
            "void main(){\n" +
            "   gl_Position = vec4(coords*2.0-1.0,0.5,1.0);\n" +
            "   uv = coords;\n" +
            "}"

    val simplestVertexShader2 = "" +
            "$attribute vec2 coords;\n" +
            "void main(){\n" +
            "   gl_Position = vec4(coords*2.0-1.0,0.5,1.0);\n" +
            "}"

    val uvList = listOf(Variable(GLSLType.V2F, "uv"))
    val simpleVertexShader = "" +
            "$attribute vec2 coords;\n" +
            "uniform vec2 pos, size;\n" +
            "uniform vec4 tiling;\n" +
            "uniform mat4 transform;\n" +
            "void main(){\n" +
            "   gl_Position = transform * vec4((pos + coords * size)*2.0-1.0, 0.5, 1.0);\n" +
            "   uv = (coords-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
            "}"

    const val brightness = "" +
            "float brightness(vec3 color){\n" +
            "   return sqrt(0.299*color.r*color.r + 0.587*color.g*color.g + 0.114*color.b*color.b);\n" +
            "}\n" +
            "float brightness(vec4 color){\n" +
            "   return sqrt(0.299*color.r*color.r + 0.587*color.g*color.g + 0.114*color.b*color.b);\n" +
            "}\n"

    fun brightness(r: Float, g: Float, b: Float) = sqrt(sqrt(0.299f * r * r + 0.587f * g * g + 0.114f * b * b))

    // https://community.khronos.org/t/quaternion-functions-for-glsl/50140/3
    const val quaternionTransform = "" +
            "vec3 quaternionTransform(vec4 q, vec3 v){\n" +
            "   return v + 2.0*cross(cross(v, q.xyz) + q.w*v, q.xyz);\n" +
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

            "   vec2 texSize = vec2(textureSize(sampler, 0));\n" +
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
            // no more artifacts, but much smoother
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
            "       vec3 positionDelta = finalPosition - positionNWeight.xyz;\n" +
            "       vec4 powerSize = forceFieldColorPowerSizes[i];\n" +
            "       float weight = positionNWeight.w / (1.0 + pow(dot(powerSize.xyz * positionDelta, positionDelta), powerSize.w));\n" +
            "       sumWeight += weight;\n" +
            "       vec4 localColor = forceFieldColors[i];\n" +
            "       sumColor += weight * localColor * localColor;\n" +
            "   }\n" +
            "   return sqrt(sumColor / sumWeight);\n" +
            "}\n"

    val colorForceFieldBuffer: FloatBuffer = ByteBufferPool
        .allocateDirect(4 * maxColorForceFields)
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

    val uvForceFieldBuffer: FloatBuffer = ByteBufferPool
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


    const val positionPostProcessing = "" +
            "   zDistance = gl_Position.w;\n"

    // this mapping only works with well tessellated geometry
    // or we need to add it to the fragment shader instead
    //"   const float far = 1000;\n" +
    //"   const float near = 0.001;\n" +
    //"   gl_Position.z = 2.0*log(gl_Position.w*near + 1)/log(far*near + 1) - 1;\n" +
    //"   gl_Position.z *= gl_Position.w;"

    const val v3DBase = "" +
            "uniform mat4 transform;\n"

    const val flatNormal = "" +
            "   normal = vec3(0.0, 0.0, 1.0);\n"

    val v3D = v3DBase +
            "$attribute vec3 coords;\n" +
            "$attribute vec2 attr1;\n" +
            "uniform vec4 tiling;\n" +
            "void main(){\n" +
            "   finalPosition = coords;\n" +
            "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
            positionPostProcessing +
            "   uv = (attr1-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
            "   uvw = coords;\n" +
            flatNormal +
            "}"

    val y3D = listOf(
        Variable(GLSLType.V2F, "uv"),
        Variable(GLSLType.V3F, "uvw"),
        Variable(GLSLType.V3F, "finalPosition"),
        Variable(GLSLType.V1F, "zDistance"),
        Variable(GLSLType.V3F, "normal")
    )

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

    val v3DMasked = v3DBase +
            "$attribute vec2 coords;\n" +
            "void main(){\n" +
            "   finalPosition = vec3(coords*2.0-1.0, 0.0);\n" +
            "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
            "   uv = gl_Position.xyw;\n" +
            positionPostProcessing +
            "}"

    val y3DMasked = listOf(
        Variable(GLSLType.V3F, "uv"),
        Variable(GLSLType.V3F, "finalPosition"),
        Variable(GLSLType.V1F, "zDistance")
    )

    // make this customizable?
    val blacklist = listOf(
        "cgSlope", "cgOffset", "cgPower", "cgSaturation",
        "forceFieldUVCount", "forceFieldColorCount"
    )

    fun init() {

        val tick = Clock()

        // with texture
        subpixelCorrectTextShader = BaseShader(
            "subpixelCorrectTextShader",
            "" +
                    "$attribute vec2 coords;\n" +
                    "uniform vec2 pos, size;\n" +
                    "uniform mat4 transform;\n" + // not really supported, since subpixel layouts would be violated for non-integer translations, scales, skews or perspective
                    "uniform vec2 windowSize;\n" +
                    "void main(){\n" +
                    "   vec2 localPos = pos + coords * size;\n" +
                    "   gl_Position = transform * vec4(localPos*2.0-1.0, 0.0, 1.0);\n" +
                    "   position = localPos * windowSize;\n" +
                    "   uv = coords;\n" +
                    "}", listOf(Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V2F, "position")), "" +
                    "uniform vec4 textColor, backgroundColor;\n" +
                    "uniform vec2 windowSize;\n" +
                    "uniform sampler2D tex;\n" +
                    brightness +
                    "void main(){\n" +
                    "   vec3 textMask = texture(tex, uv).rgb;\n" +
                    "   vec3 mixing = brightness(textColor) > brightness(backgroundColor) ? textMask.rgb : textMask.rgb;\n" +
                    "   mixing *= textColor.a;\n" +
                    "   float mixingAlpha = brightness(mixing);\n" +
                    // theoretically, we only need to check the axis, which is affected by subpixel-rendering, e.g. x on my screen
                    "   if(position.x < 1.0 || position.y < 1.0 || position.x > windowSize.x - 1.0 || position.y > windowSize.y - 1.0)\n" +
                    "       mixing = vec3(mixingAlpha);\n" + // on the border; color seams would become apparent here
                    "   vec4 color = mix(backgroundColor, textColor, vec4(mixing, mixingAlpha));\n" +
                    "   if(color.a < 0.001) discard;\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = 1.0;\n" +
                    "}"
        )
        subpixelCorrectTextShader.setTextureIndices(listOf("tex"))

        shader3D = createShader("3d", v3D, y3D, f3D, listOf("tex"))
        shader3DText = createShader(
            "3d-text", v3DBase +
                    "$attribute vec3 coords;\n" +
                    "$attribute vec2 attr1;\n" +
                    "uniform vec3 offset;\n" +
                    getUVForceFieldLib +
                    "void main(){\n" +
                    "   vec3 localPos0 = coords + offset;\n" +
                    "   vec2 pseudoUV2 = getForceFieldUVs(localPos0.xy*.5+.5);\n" +
                    "   finalPosition = $hasForceFieldUVs ? vec3(pseudoUV2*2.0-1.0, coords.z + offset.z) : localPos0;\n" +
                    "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    flatNormal +
                    positionPostProcessing +
                    "   vertexId = gl_VertexID;\n" +
                    "}", y3D + listOf(Variable(GLSLType.V1I, "vertexId").flat()), "" +
                    noiseFunc +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 finalColor2 = ($hasForceFieldColor) ? getForceFieldColor() : vec4(1.0);\n" +
                    "   vec3 finalColor = finalColor2.rgb;\n" +
                    "   float finalAlpha = finalColor2.a;\n" +
                    "}", listOf()
        )
        shader3DText.ignoreUniformWarnings("tiling", "forceFieldUVCount")

        shaderSDFText = createShader(
            "3d-text-withOutline", v3DBase +
                    "$attribute vec3 coords;\n" +
                    "$attribute vec2 attr1;\n" +
                    "uniform vec2 offset, scale;\n" +
                    getUVForceFieldLib +
                    "void main(){\n" +
                    "   uv = coords.xy * 0.5 + 0.5;\n" +
                    "   vec2 localPos0 = coords.xy * scale + offset;\n" +
                    "   vec2 pseudoUV2 = getForceFieldUVs(localPos0*.5+.5);\n" +
                    "   finalPosition = vec3($hasForceFieldUVs ? pseudoUV2*2.0-1.0 : localPos0, 0);\n" +
                    "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    positionPostProcessing +
                    "}", y3D, "" +
                    noiseFunc +
                    getTextureLib +
                    getColorForceFieldLib +
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
                    "   gl_FragDepth = gl_FragCoord.z * (1.0 + distance * depth);\n" +
                    "   if(color.a <= 0.001) discard;\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = 1.0;//color.a;\n" +
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
                "$attribute vec3 coords;\n" +
                "$attribute vec2 attr1;\n" +
                "uniform float inset;\n" +
                "void main(){\n" +
                "   vec2 betterUV = coords.xy;\n" +
                "   betterUV *= mix(1.0, attr1.r, inset);\n" +
                "   finalPosition = vec3(betterUV, coords.z);\n" +
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                flatNormal +
                positionPostProcessing +
                "   uv = attr1.yx;\n" +
                "}"
        shader3DPolygon = createShader("3d-polygon", v3DPolygon, y3D, f3D, listOf("tex"))
        shader3DPolygon.ignoreUniformWarnings("tiling", "forceFieldUVCount")

        // somehow becomes dark for large |steps|-values
        shader3DBoxBlur = createShader(
            "3d-blur", coordsList, coordsVShader, uvList, listOf(), "" +
                    "precision highp float;\n" + // why?
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
                "$attribute vec3 aLocalPosition;\n" +
                "$attribute vec2 aLocalPos2;\n" +
                "$attribute vec4 aFormula0;\n" +
                "$attribute float aFormula1;\n" +
                "$attribute vec4 aColor0, aColor1, aColor2, aColor3;\n" +
                "$attribute vec4 aStops;\n" +
                "$attribute float aPadding;\n" +
                "void main(){\n" +
                "   finalPosition = aLocalPosition;\n" +
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
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

        val ySVG = y3D + listOf(
            Variable(GLSLType.V4F, "color0"),
            Variable(GLSLType.V4F, "color1"),
            Variable(GLSLType.V4F, "color2"),
            Variable(GLSLType.V4F, "color3"),
            Variable(GLSLType.V4F, "stops"),
            Variable(GLSLType.V4F, "formula0"),
            Variable(GLSLType.V1F, "formula1"),
            Variable(GLSLType.V1F, "padding"),
            Variable(GLSLType.V2F, "localPos2"),
        )

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
                "vec4 mix2(vec4 a, vec4 b, float stop, vec2 stops){\n" +
                "   float f = clamp((stop-stops.x)/(stops.y-stops.x), 0.0, 1.0);\n" +
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
                "       stopValue <  stops.y ? mix2(color0, color1, stopValue, stops.xy):\n" +
                "       stopValue <  stops.z ? mix2(color1, color2, stopValue, stops.yz):\n" +
                "                              mix2(color2, color3, stopValue, stops.zw);\n" +
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

        shader3DSVG = createShader("3d-svg", vSVG, ySVG, fSVG, listOf("tex"))

        val v3DCircle = v3DBase +
                "$attribute vec2 coords;\n" + // angle, inner/outer
                "uniform vec3 circleParams;\n" + // 1 - inner r, start, end
                "void main(){\n" +
                "   float angle = mix(circleParams.y, circleParams.z, coords.x);\n" +
                "   vec2 betterUV = vec2(cos(angle), -sin(angle)) * (1.0 - circleParams.x * coords.y);\n" +
                "   finalPosition = vec3(betterUV, 0.0);\n" +
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                flatNormal +
                positionPostProcessing +
                "}"

        val f3DCircle = "" +
                getColorForceFieldLib +
                "void main(){\n" +
                "   gl_FragColor = ($hasForceFieldColor) ? getForceFieldColor() : vec4(1);\n" +
                "}"

        shader3DCircle = createShader("3dCircle", v3DCircle, y3D, f3DCircle, listOf())
        shader3DCircle.ignoreUniformWarnings(
            listOf(
                "filtering",
                "textureDeltaUV",
                "tiling",
                "uvProjection",
                "forceFieldUVCount",
                "cgOffset", "cgSlope", "cgPower", "cgSaturation"
            )
        )

        // create the obj+mtl shader
        shaderObjMtl = createShader(
            "obj/mtl",
            v3DBase +
                    "$attribute vec3 coords;\n" +
                    "$attribute vec2 uvs;\n" +
                    "$attribute vec3 normals;\n" +
                    "void main(){\n" +
                    "   finalPosition = coords;\n" +
                    "   gl_Position = transform * vec4(coords, 1.0);\n" +
                    "   uv = uvs;\n" +
                    "   normal = normals;\n" +
                    positionPostProcessing +
                    "}", y3D + listOf(Variable(GLSLType.V3F, "normal")), "" +
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
        val assimpVertex = v3DBase +
                "$attribute vec3 coords;\n" +
                "$attribute vec2 uvs;\n" +
                "$attribute vec3 normals;\n" +
                "$attribute vec3 tangents;\n" +
                "$attribute vec4 colors;\n" +
                "$attribute vec4 weights;\n" +
                "$attribute ivec4 indices;\n" +
                "uniform bool hasAnimation;\n" +
                "uniform mat4x3 localTransform;\n" +
                (if (useAnimTextures) "" +
                        "uniform sampler2D animTexture;\n" +
                        "uniform vec4 animWeights, animIndices;\n" +
                        getAnimMatrix else "" +
                        "uniform mat4x3 jointTransforms[${min(128, maxBones)}];\n" +
                        "") +
                "void main(){\n" +
                (if (useAnimTextures) "" +
                        "   if(hasAnimation && textureSize(animTexture,0).x > 1.0){\n" +
                        "       mat4x3 jointMat;\n" +
                        "jointMat  = getAnimMatrix(indices.x) * weights.x;\n" +
                        "jointMat += getAnimMatrix(indices.y) * weights.y;\n" +
                        "jointMat += getAnimMatrix(indices.z) * weights.z;\n" +
                        "jointMat += getAnimMatrix(indices.w) * weights.w;\n"
                else "" +
                        "   if(hasAnimation){\n" +
                        "       mat4x3 jointMat;\n" +
                        "jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                        "jointMat += jointTransforms[indices.y] * weights.y;\n" +
                        "jointMat += jointTransforms[indices.z] * weights.z;\n" +
                        "jointMat += jointTransforms[indices.w] * weights.w;\n"
                        ) +
                "       finalPosition = jointMat * vec4(coords, 1.0);\n" +
                "       normal = jointMat * vec4(normals, 0.0);\n" +
                "       tangent = jointMat * vec4(tangents, 0.0);\n" +
                "   } else {\n" +
                "       finalPosition = coords;\n" +
                "       normal = normals;\n" +
                "       tangent = tangents;\n" +
                "   }\n" +
                "   normal = localTransform * vec4(normal, 0.0);\n" +
                "   tangent = localTransform * vec4(tangent, 0.0);\n" +
                "   finalPosition = localTransform * vec4(finalPosition, 1.0);\n" +
                // normal only needs to be normalized, if we show the normal
                "   normal = normalize(normal);\n" + // here? nah ^^
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                "   uv = uvs;\n" +
                // "   weight = weights;\n" +
                "   vertexColor = colors;\n" +
                positionPostProcessing +
                "}"

        val assimpVarying = y3D + listOf(
            Variable(GLSLType.V3F, "tangent"),
            // Variable(GLSLType.V4F, "weight"),
            Variable(GLSLType.V4F, "vertexColor")
        )

        shaderAssimp = createShader(
            "assimp",
            assimpVertex, assimpVarying, "" +
                    "uniform sampler2D albedoTex;\n" +
                    "uniform vec4 diffuseBase;\n" +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = vec4(vertexColor.rgb,1) * diffuseBase * getTexture(albedoTex, uv);\n" +
                    "   color.rgb *= 0.6 + 0.4 * dot(vec3(-1.0, 0.0, 0.0), normal);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = color.a;\n" +
                    "   vec3 finalPosition = finalPosition;\n" +
                    "   vec3 finalNormal = normal;\n" +
                    "}", listOf("albedoTex", "animTexture")
        )
        shaderAssimp.glslVersion = 330

        monochromeModelShader = createShader(
            "monochrome-model",
            assimpVertex, assimpVarying, "" +
                    "uniform vec4 tint;\n" +
                    "uniform sampler2D tex;\n" +
                    "void main(){\n" +
                    "   vec4 color = texture(tex, uv);\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = color.a;\n" +
                    "   vec3 finalPosition = finalPosition;\n" +
                    "   vec3 finalNormal = normal;\n" +
                    "   vec3 finalEmissive = tint.rgb;\n" +
                    "   float finalRoughness = 1.0;" +
                    "   float finalMetallic = 0.0;\n" +
                    "}", listOf("tex", "animTexture")
        )
        monochromeModelShader.glslVersion = 330
        monochromeModelShader.ignoreUniformWarnings("worldScale")

        // create the fbx shader
        // shaderFBX = FBXShader.getShader(v3DBase, positionPostProcessing, y3D, getTextureLib)

        shader3DYUV = createShader(
            "3d-yuv",
            v3D, y3D, "" +
                    "uniform sampler2D texY, texUV;\n" +
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
                    "       getTexture(texUV, correctedUV, correctedDUV).rg);\n" + //
                    "   vec4 color = vec4(yuv2rgb(yuv), 1.0);\n" +
                    "   color.rgb = colorGrading(color.rgb);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor();\n" +
                    "   vec3 finalColor = color.rgb;\n" +
                    "   float finalAlpha = color.a;\n" +
                    "}", listOf("texY", "texUV")
        )

        fun createSwizzleShader(swizzle: String): BaseShader {
            return createShader(
                "3d-${swizzle.ifEmpty { "rgba" }}",
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
            "$attribute vec3 coords;\n" +
                    "uniform mat4 transform;\n" +
                    "void main(){" +
                    "   gl_Position = transform * vec4(coords, 1.0);\n" +
                    positionPostProcessing +
                    "}", listOf(Variable(GLSLType.V1F, "zDistance")), "" +
                    "uniform vec4 color;\n" +
                    "void main(){" +
                    "   gl_FragColor = color;\n" +
                    "}"

        )

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
                "           float weight = exp(-relativeX*relativeX);\n" +
                "           sum += weight;\n" +
                "           color += vec4(max(vec3(0.0), colorHere.rgb - threshold), colorHere.a) * weight;\n" +
                "       }\n" +
                "       color /= sum;\n" +
                "   }\n" +
                "   gl_FragColor = color;\n" +
                "}"
        shader3DGaussianBlur =
            createShader("3d-blur", v3DMasked, y3DMasked, f3DGaussianBlur, listOf("tex"))

        tick.stop("creating default shaders")

    }

    fun createShader(
        shaderName: String,
        vertexShader: String,
        varyings: List<Variable>,
        fragmentShader: String,
        textures: List<String>
    ): BaseShader {
        val shader = BaseShader(shaderName, vertexShader, varyings, fragmentShader)
        shader.setTextureIndices(textures)
        return shader
    }

    fun createShader(
        shaderName: String,
        vertexVariables: List<Variable>,
        vertexShader: String,
        varyings: List<Variable>,
        fragmentVariables: List<Variable>,
        fragmentShader: String,
        textures: List<String>
    ): BaseShader {
        val shader = BaseShader(shaderName, vertexVariables, vertexShader, varyings, fragmentVariables, fragmentShader)
        shader.setTextureIndices(textures)
        return shader
    }


}